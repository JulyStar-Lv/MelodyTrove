use std::{collections::VecDeque, f32::consts::PI, time::Duration};

use rodio::{source::SeekError, ChannelCount, SampleRate, Source};

const EQ_FREQUENCIES_HZ: [f32; 10] = [
    31.0, 62.0, 125.0, 250.0, 500.0, 1_000.0, 2_000.0, 4_000.0, 8_000.0, 16_000.0,
];

#[derive(Clone)]
pub struct DesktopDspSettings {
    pub enabled: bool,
    pub eq_band_gains_db: Vec<f32>,
    pub eq_q: f32,
    pub bass_db: f32,
    pub treble_db: f32,
    pub compressor_enabled: bool,
    pub compressor_threshold_db: f32,
    pub compressor_ratio: f32,
    pub compressor_makeup_db: f32,
    pub stereo_width: f32,
    pub reverb_preset: u8,
    pub replay_gain_db: f32,
    pub crossfade_duration_ms: u64,
}

impl Default for DesktopDspSettings {
    fn default() -> Self {
        Self {
            enabled: false,
            eq_band_gains_db: vec![0.0; EQ_FREQUENCIES_HZ.len()],
            eq_q: 1.0,
            bass_db: 0.0,
            treble_db: 0.0,
            compressor_enabled: false,
            compressor_threshold_db: -18.0,
            compressor_ratio: 4.0,
            compressor_makeup_db: 0.0,
            stereo_width: 1.0,
            reverb_preset: 0,
            replay_gain_db: 0.0,
            crossfade_duration_ms: 0,
        }
    }
}

pub struct DesktopDspSource<S> {
    inner: S,
    settings: DesktopDspSettings,
    filters: Vec<Vec<Biquad>>,
    pending: VecDeque<f32>,
    reverb: Vec<f32>,
    reverb_cursor: usize,
    reverb_mix: f32,
    reverb_feedback: f32,
}

impl<S: Source> DesktopDspSource<S> {
    pub fn new(inner: S, settings: DesktopDspSettings) -> Self {
        let sample_rate = inner.sample_rate().get() as f32;
        let channels = inner.channels().get() as usize;
        let mut coefficients = settings
            .eq_band_gains_db
            .iter()
            .copied()
            .zip(EQ_FREQUENCIES_HZ)
            .filter(|(gain, frequency)| gain.abs() > 0.01 && *frequency < sample_rate * 0.48)
            .map(|(gain, frequency)| {
                BiquadCoefficients::peaking(sample_rate, frequency, settings.eq_q, gain)
            })
            .collect::<Vec<_>>();
        if settings.bass_db.abs() > 0.01 {
            coefficients.push(BiquadCoefficients::peaking(
                sample_rate,
                100.0,
                0.7,
                settings.bass_db,
            ));
        }
        if settings.treble_db.abs() > 0.01 {
            coefficients.push(BiquadCoefficients::peaking(
                sample_rate,
                10_000.0_f32.min(sample_rate * 0.45),
                0.7,
                settings.treble_db,
            ));
        }
        let filters = (0..channels)
            .map(|_| coefficients.iter().copied().map(Biquad::new).collect())
            .collect();
        let (delay_ms, reverb_mix, reverb_feedback) = match settings.reverb_preset {
            1 => (35.0, 0.10, 0.22),
            2 => (55.0, 0.14, 0.30),
            3 => (80.0, 0.18, 0.38),
            4 => (115.0, 0.22, 0.45),
            5 => (70.0, 0.20, 0.34),
            _ => (0.0, 0.0, 0.0),
        };
        let delay_samples = ((sample_rate * delay_ms / 1_000.0) as usize)
            .saturating_mul(channels)
            .max(1);
        Self {
            inner,
            settings,
            filters,
            pending: VecDeque::with_capacity(channels),
            reverb: vec![0.0; delay_samples],
            reverb_cursor: 0,
            reverb_mix,
            reverb_feedback,
        }
    }

    fn reset_processing_state(&mut self) {
        self.pending.clear();
        self.reverb.fill(0.0);
        self.reverb_cursor = 0;
        for channel in &mut self.filters {
            for filter in channel {
                filter.reset();
            }
        }
    }

    fn process_frame(&mut self) -> Option<()> {
        let channels = self.inner.channels().get() as usize;
        let mut frame = Vec::with_capacity(channels);
        for channel in 0..channels {
            let mut sample = self.inner.next()?;
            for filter in &mut self.filters[channel] {
                sample = filter.process(sample);
            }
            if self.settings.compressor_enabled {
                sample = compress_sample(
                    sample,
                    self.settings.compressor_threshold_db,
                    self.settings.compressor_ratio,
                    self.settings.compressor_makeup_db,
                );
            }
            frame.push(sample);
        }

        if frame.len() >= 2 {
            let mid = (frame[0] + frame[1]) * 0.5;
            let side = (frame[0] - frame[1]) * 0.5 * self.settings.stereo_width;
            frame[0] = mid + side;
            frame[1] = mid - side;
        }
        for sample in &mut frame {
            if self.reverb_mix > 0.0 {
                let delayed = self.reverb[self.reverb_cursor];
                self.reverb[self.reverb_cursor] = *sample + delayed * self.reverb_feedback;
                *sample = *sample * (1.0 - self.reverb_mix) + delayed * self.reverb_mix;
                self.reverb_cursor = (self.reverb_cursor + 1) % self.reverb.len();
            }
            *sample = sample.clamp(-1.0, 1.0);
        }
        self.pending.extend(frame);
        Some(())
    }
}

impl<S: Source> Iterator for DesktopDspSource<S> {
    type Item = f32;

    fn next(&mut self) -> Option<Self::Item> {
        if let Some(sample) = self.pending.pop_front() {
            return Some(sample);
        }
        self.process_frame()?;
        self.pending.pop_front()
    }
}

impl<S: Source> Source for DesktopDspSource<S> {
    fn current_span_len(&self) -> Option<usize> {
        self.inner.current_span_len()
    }

    fn channels(&self) -> ChannelCount {
        self.inner.channels()
    }

    fn sample_rate(&self) -> SampleRate {
        self.inner.sample_rate()
    }

    fn total_duration(&self) -> Option<Duration> {
        self.inner.total_duration()
    }

    fn try_seek(&mut self, pos: Duration) -> Result<(), SeekError> {
        self.inner.try_seek(pos)?;
        self.reset_processing_state();
        Ok(())
    }
}

#[derive(Clone, Copy)]
struct BiquadCoefficients {
    b0: f32,
    b1: f32,
    b2: f32,
    a1: f32,
    a2: f32,
}

impl BiquadCoefficients {
    fn peaking(sample_rate: f32, frequency: f32, q: f32, gain_db: f32) -> Self {
        let amplitude = 10.0_f32.powf(gain_db / 40.0);
        let omega = 2.0 * PI * frequency.max(10.0) / sample_rate.max(1.0);
        let alpha = omega.sin() / (2.0 * q.clamp(0.1, 10.0));
        let cos = omega.cos();
        let a0 = 1.0 + alpha / amplitude;
        Self {
            b0: (1.0 + alpha * amplitude) / a0,
            b1: (-2.0 * cos) / a0,
            b2: (1.0 - alpha * amplitude) / a0,
            a1: (-2.0 * cos) / a0,
            a2: (1.0 - alpha / amplitude) / a0,
        }
    }
}

struct Biquad {
    coefficients: BiquadCoefficients,
    x1: f32,
    x2: f32,
    y1: f32,
    y2: f32,
}

impl Biquad {
    fn new(coefficients: BiquadCoefficients) -> Self {
        Self {
            coefficients,
            x1: 0.0,
            x2: 0.0,
            y1: 0.0,
            y2: 0.0,
        }
    }

    fn process(&mut self, sample: f32) -> f32 {
        let c = self.coefficients;
        let output =
            c.b0 * sample + c.b1 * self.x1 + c.b2 * self.x2 - c.a1 * self.y1 - c.a2 * self.y2;
        self.x2 = self.x1;
        self.x1 = sample;
        self.y2 = self.y1;
        self.y1 = output;
        output
    }

    fn reset(&mut self) {
        self.x1 = 0.0;
        self.x2 = 0.0;
        self.y1 = 0.0;
        self.y2 = 0.0;
    }
}

fn compress_sample(sample: f32, threshold_db: f32, ratio: f32, makeup_db: f32) -> f32 {
    let amplitude = sample.abs().max(1.0e-8);
    let input_db = 20.0 * amplitude.log10();
    let output_db = if input_db > threshold_db {
        threshold_db + (input_db - threshold_db) / ratio.max(1.0)
    } else {
        input_db
    } + makeup_db;
    sample * 10.0_f32.powf((output_db - input_db) / 20.0)
}
