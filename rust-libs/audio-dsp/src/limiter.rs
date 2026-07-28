// Adapted from RawS Music:
// https://github.com/QFDY-GZC/RawS-Music
//
// Original project license: Apache-2.0.
// This implementation has been rewritten and modified for MelodyTrove's
// cross-platform Rust DSP pipeline.

use crate::config::{db_to_linear, smoothing_coefficient, LimiterConfig, MAX_CHANNELS};

#[derive(Debug)]
pub(crate) struct PeakLimiter {
    enabled: bool,
    ceiling: f32,
    attack_coefficient: f32,
    release_coefficient: f32,
    gain: f32,
    gain_reduction_db: f32,
}

impl Default for PeakLimiter {
    fn default() -> Self {
        Self {
            enabled: true,
            ceiling: db_to_linear(-0.5),
            attack_coefficient: 1.0,
            release_coefficient: 1.0,
            gain: 1.0,
            gain_reduction_db: 0.0,
        }
    }
}

impl PeakLimiter {
    pub(crate) fn configure(&mut self, sample_rate: u32, config: LimiterConfig) {
        self.enabled = config.enabled;
        self.ceiling = db_to_linear(config.ceiling_db).clamp(0.01, 1.0);
        self.attack_coefficient = smoothing_coefficient(config.attack_ms, sample_rate);
        self.release_coefficient = smoothing_coefficient(config.release_ms, sample_rate);
        if !self.enabled {
            self.gain_reduction_db = 0.0;
        }
    }

    pub(crate) fn process_frame(&mut self, frame: &mut [f32; MAX_CHANNELS], channels: usize) {
        if !self.enabled {
            return;
        }
        let peak = frame[..channels]
            .iter()
            .fold(0.0_f32, |maximum, sample| maximum.max(sample.abs()));
        let target = if peak > self.ceiling {
            self.ceiling / peak.max(1.0e-12)
        } else {
            1.0
        };
        let coefficient = if target < self.gain {
            self.attack_coefficient
        } else {
            self.release_coefficient
        };
        self.gain += (target - self.gain) * coefficient;
        if !self.gain.is_finite() {
            self.reset();
            return;
        }
        // With no look-ahead, a smoothed attack cannot by itself guarantee
        // the configured ceiling on the first transient. Keep the envelope
        // smoothing for sustained material, but apply an instantaneous
        // sample-peak safety gain whenever the current frame needs more
        // attenuation.
        let applied_gain = self.gain.min(target);
        for sample in frame.iter_mut().take(channels) {
            *sample *= applied_gain;
        }
        self.gain_reduction_db = (-20.0 * applied_gain.max(1.0e-12).log10()).max(0.0);
    }

    pub(crate) fn gain_reduction_db(&self) -> f32 {
        self.gain_reduction_db
    }

    pub(crate) fn reset(&mut self) {
        self.gain = 1.0;
        self.gain_reduction_db = 0.0;
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn limiter_uses_one_gain_for_both_channels() {
        let mut limiter = PeakLimiter::default();
        limiter.configure(
            48_000,
            LimiterConfig {
                enabled: true,
                ceiling_db: -6.0,
                attack_ms: 0.01,
                ..LimiterConfig::default()
            },
        );
        let mut frame = [2.0, 0.5];
        for _ in 0..64 {
            frame = [2.0, 0.5];
            limiter.process_frame(&mut frame, 2);
        }
        assert!((frame[0] / 2.0 - frame[1] / 0.5).abs() < 1.0e-6);
        assert!(frame[0] <= 1.01);
    }

    #[test]
    fn reset_clears_gain_reduction() {
        let mut limiter = PeakLimiter {
            gain: 0.25,
            gain_reduction_db: 12.0,
            ..PeakLimiter::default()
        };
        limiter.reset();
        assert_eq!(limiter.gain, 1.0);
        assert_eq!(limiter.gain_reduction_db(), 0.0);
    }

    #[test]
    fn first_transient_respects_ceiling_without_lookahead() {
        let mut limiter = PeakLimiter::default();
        limiter.configure(
            48_000,
            LimiterConfig {
                enabled: true,
                ceiling_db: -6.0,
                attack_ms: 10.0,
                ..LimiterConfig::default()
            },
        );
        let mut frame = [2.0, -0.5];
        limiter.process_frame(&mut frame, 2);
        let ceiling = db_to_linear(-6.0);
        assert!(frame[0].abs() <= ceiling + 1.0e-6);
        assert!(frame[1].abs() <= ceiling + 1.0e-6);
    }
}
