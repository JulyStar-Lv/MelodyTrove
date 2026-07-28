use std::f32::consts::TAU;

use audio_dsp::{
    AudioDspConfig, AudioDspProcessor, CompressorConfig, CrossfeedConfig, DynamicEqConfig, EqMode,
    GraphicEqualizerConfig, LimiterConfig, LoudnessConfig, MonoBassConfig, MoogFilterConfig,
    MoogFilterMode, ParametricEqBand, ParametricEqualizerConfig, ReverbConfig, ReverbPreset,
    SpatialAudioConfig, SpatialMode, SpeakerOutputConfig, SpeakerOutputMode, StereoWidthConfig,
    ToneControlConfig,
};

fn configured(config: AudioDspConfig, sample_rate: u32, channels: usize) -> AudioDspProcessor {
    let mut processor = AudioDspProcessor::new(config).unwrap();
    processor.configure_format(sample_rate, channels).unwrap();
    processor
}

fn full_effects_config() -> AudioDspConfig {
    let mut parametric_equalizer = ParametricEqualizerConfig {
        enabled: true,
        preamp_db: -6.0,
        band_count: 3,
        ..Default::default()
    };
    parametric_equalizer.bands[0] = ParametricEqBand {
        enabled: true,
        frequency_hz: 90.0,
        gain_db: 2.0,
        q: 0.8,
        ..Default::default()
    };
    parametric_equalizer.bands[1] = ParametricEqBand {
        enabled: true,
        frequency_hz: 1_600.0,
        gain_db: -2.0,
        q: 1.2,
        ..Default::default()
    };
    parametric_equalizer.bands[2] = ParametricEqBand {
        enabled: true,
        frequency_hz: 6_500.0,
        gain_db: 1.0,
        q: 1.5,
        ..Default::default()
    };
    AudioDspConfig {
        enabled: true,
        input_gain_db: -3.0,
        eq_mode: EqMode::Parametric,
        parametric_equalizer,
        tone: ToneControlConfig {
            enabled: true,
            bass_gain_db: 2.0,
            treble_gain_db: 1.0,
            ..Default::default()
        },
        loudness: LoudnessConfig {
            enabled: true,
            amount: 0.3,
            balance: -0.1,
        },
        mono_bass: MonoBassConfig {
            enabled: true,
            crossover_hz: 130.0,
            amount: 0.7,
        },
        dynamic_eq: DynamicEqConfig {
            enabled: true,
            amount: 0.4,
            de_esser_amount: 0.35,
            de_esser_frequency_hz: 6_500.0,
        },
        moog: MoogFilterConfig {
            enabled: true,
            cutoff_hz: 12_000.0,
            resonance: 0.2,
            drive_db: 2.0,
            mix: 0.25,
            ..Default::default()
        },
        compressor: CompressorConfig {
            enabled: true,
            threshold_db: -18.0,
            ratio: 3.0,
            attack_ms: 8.0,
            release_ms: 120.0,
            makeup_gain_db: 1.0,
            ..Default::default()
        },
        reverb: ReverbConfig {
            preset: ReverbPreset::SmallRoom,
            wet: 0.08,
        },
        spatial: SpatialAudioConfig {
            mode: SpatialMode::Panoramic360,
            intensity: 0.3,
            azimuth_degrees: 45.0,
            elevation_degrees: 20.0,
            auto_rotate_degrees_per_second: 15.0,
            room_amount: 0.15,
        },
        speaker_output: SpeakerOutputConfig {
            enabled: true,
            mode: SpeakerOutputMode::Elasticity,
            strength: 0.2,
        },
        limiter: LimiterConfig {
            enabled: true,
            ceiling_db: -1.0,
            ..Default::default()
        },
        ..Default::default()
    }
}

fn generated_program(sample_rate: u32, frames: usize, channels: usize) -> Vec<f32> {
    let mut samples = vec![0.0; frames * channels];
    let mut noise_state = 0x1234_5678_u32;
    for frame in 0..frames {
        let time = frame as f32 / sample_rate as f32;
        noise_state = noise_state
            .wrapping_mul(1_664_525)
            .wrapping_add(1_013_904_223);
        let noise = ((noise_state >> 8) as f32 / 0x00ff_ffff as f32 - 0.5) * 0.04;
        let transient = if frame % (sample_rate as usize / 20).max(1) < 4 {
            0.35
        } else {
            0.0
        };
        let bass = (TAU * 80.0 * time).sin() * 0.18;
        let mid = (TAU * 1_000.0 * time).sin() * 0.12;
        let sibilance = if (frame / 256) % 4 == 0 {
            (TAU * 7_000.0 * time).sin() * 0.08
        } else {
            0.0
        };
        let left = bass + mid + sibilance + transient + noise;
        samples[frame * channels] = left;
        if channels == 2 {
            samples[frame * channels + 1] =
                bass - mid * 0.7 + sibilance * 0.8 + transient + noise * 0.5;
        }
    }
    samples
}

fn metrics(samples: &[f32]) -> (f32, f32, f32) {
    let count = samples.len().max(1) as f32;
    let rms = (samples.iter().map(|sample| sample * sample).sum::<f32>() / count).sqrt();
    let peak = samples
        .iter()
        .map(|sample| sample.abs())
        .fold(0.0, f32::max);
    let dc = samples.iter().sum::<f32>() / count;
    (rms, peak, dc)
}

#[test]
fn generated_golden_program_is_safe_at_all_required_rates_and_channel_counts() {
    for sample_rate in [44_100, 48_000, 88_200, 96_000, 176_400, 192_000] {
        for channels in [1, 2] {
            let mut processor = configured(full_effects_config(), sample_rate, channels);
            let mut samples = generated_program(sample_rate, 4_096, channels);
            processor.process_interleaved_f32(&mut samples).unwrap();
            let (rms, peak, dc) = metrics(&samples);
            assert!(samples.iter().all(|sample| sample.is_finite()));
            assert!(rms > 0.001 && rms < 0.8, "unexpected RMS {rms}");
            assert!(peak <= 1.0, "unsafe peak {peak}");
            assert!(dc.abs() < 0.2, "unexpected DC offset {dc}");
        }
    }
}

#[test]
fn logarithmic_sweeps_remain_finite_and_bounded() {
    for sample_rate in [44_100, 96_000, 192_000] {
        let frames = 16_384;
        let duration_seconds = frames as f32 / sample_rate as f32;
        let start_hz = 20.0_f32;
        let end_hz = (sample_rate as f32 * 0.45).min(20_000.0);
        let sweep_rate = (end_hz / start_hz).ln() / duration_seconds;
        let mut samples = vec![0.0; frames * 2];
        for frame in 0..frames {
            let time = frame as f32 / sample_rate as f32;
            let phase = TAU * start_hz * ((sweep_rate * time).exp() - 1.0) / sweep_rate;
            samples[frame * 2] = phase.sin() * 0.2;
            samples[frame * 2 + 1] = (phase + 0.35).sin() * 0.2;
        }

        let mut processor = configured(full_effects_config(), sample_rate, 2);
        processor.process_interleaved_f32(&mut samples).unwrap();
        let (rms, peak, dc) = metrics(&samples);
        assert!(samples.iter().all(|sample| sample.is_finite()));
        assert!(rms > 0.001 && rms < 0.8, "unexpected RMS {rms}");
        assert!(peak <= 1.0, "unsafe peak {peak}");
        assert!(dc.abs() < 0.2, "unexpected DC offset {dc}");
    }
}

#[test]
fn all_moog_and_speaker_modes_remain_finite_under_extreme_input() {
    for mode in [
        MoogFilterMode::LowPass24,
        MoogFilterMode::LowPass12,
        MoogFilterMode::HighPass24,
        MoogFilterMode::BandPass12,
        MoogFilterMode::Notch,
    ] {
        let mut processor = configured(
            AudioDspConfig {
                enabled: true,
                moog: MoogFilterConfig {
                    enabled: true,
                    mode,
                    cutoff_hz: 18_000.0,
                    resonance: 1.0,
                    drive_db: 18.0,
                    mix: 1.0,
                },
                limiter: LimiterConfig {
                    enabled: true,
                    ..Default::default()
                },
                ..Default::default()
            },
            192_000,
            2,
        );
        let mut samples = generated_program(192_000, 8_192, 2);
        samples[100] = f32::NAN;
        samples[101] = f32::INFINITY;
        processor.process_interleaved_f32(&mut samples).unwrap();
        assert!(samples.iter().all(|sample| sample.is_finite()));
    }

    for mode in [
        SpeakerOutputMode::Elasticity,
        SpeakerOutputMode::Powerful,
        SpeakerOutputMode::Wide,
    ] {
        let mut processor = configured(
            AudioDspConfig {
                enabled: true,
                speaker_output: SpeakerOutputConfig {
                    enabled: true,
                    mode,
                    strength: 1.0,
                },
                ..Default::default()
            },
            48_000,
            2,
        );
        let mut samples = generated_program(48_000, 4_096, 2);
        processor.process_interleaved_f32(&mut samples).unwrap();
        assert!(samples.iter().all(|sample| sample.is_finite()));
        assert!(metrics(&samples).1 <= 1.0);
    }
}

#[test]
fn spatial_direction_and_mono_safety_are_repeatable() {
    let config = AudioDspConfig {
        enabled: true,
        spatial: SpatialAudioConfig {
            mode: SpatialMode::Surround360,
            intensity: 1.0,
            azimuth_degrees: 90.0,
            ..Default::default()
        },
        ..Default::default()
    };
    let mut stereo = configured(config, 48_000, 2);
    let mut samples = vec![0.0; 8_192];
    for frame in 0..samples.len() / 2 {
        let sample = (TAU * 440.0 * frame as f32 / 48_000.0).sin() * 0.2;
        samples[frame * 2] = sample;
        samples[frame * 2 + 1] = sample;
    }
    stereo.process_interleaved_f32(&mut samples).unwrap();
    let left_rms = metrics(&samples.iter().step_by(2).copied().collect::<Vec<_>>()).0;
    let right_rms = metrics(
        &samples
            .iter()
            .skip(1)
            .step_by(2)
            .copied()
            .collect::<Vec<_>>(),
    )
    .0;
    assert!(right_rms > left_rms * 1.1);

    let mut mono = configured(config, 192_000, 1);
    let mut mono_samples = generated_program(192_000, 8_192, 1);
    mono.process_interleaved_f32(&mut mono_samples).unwrap();
    assert!(mono_samples.iter().all(|sample| sample.is_finite()));
}

#[test]
fn crossfeed_width_and_mono_bass_change_only_stereo_content_safely() {
    let config = AudioDspConfig {
        enabled: true,
        mono_bass: MonoBassConfig {
            enabled: true,
            crossover_hz: 160.0,
            amount: 1.0,
        },
        stereo_width: StereoWidthConfig {
            enabled: true,
            width: 1.5,
        },
        crossfeed: CrossfeedConfig {
            enabled: true,
            ..Default::default()
        },
        spatial: SpatialAudioConfig {
            mode: SpatialMode::CrossfeedAndWidth,
            ..Default::default()
        },
        ..Default::default()
    };
    let mut processor = configured(config, 48_000, 2);
    let mut samples = generated_program(48_000, 8_192, 2);
    let original = samples.clone();
    processor.process_interleaved_f32(&mut samples).unwrap();
    assert!(samples.iter().all(|sample| sample.is_finite()));
    assert!(samples
        .iter()
        .zip(original)
        .any(|(processed, input)| (processed - input).abs() > 1.0e-4));
}

#[test]
fn empty_buffers_and_reset_after_every_effect_are_safe() {
    let mut processor = configured(full_effects_config(), 48_000, 2);
    processor.process_interleaved_f32(&mut []).unwrap();
    let mut samples = generated_program(48_000, 4_096, 2);
    processor.process_interleaved_f32(&mut samples).unwrap();
    processor.reset();
    let mut silence = vec![0.0; 8_192];
    processor.process_interleaved_f32(&mut silence).unwrap();
    assert!(silence.iter().all(|sample| sample.abs() < 1.0e-6));
}

#[test]
fn graphic_zero_db_response_is_neutral() {
    let processor = configured(
        AudioDspConfig {
            enabled: true,
            graphic_equalizer: GraphicEqualizerConfig {
                enabled: true,
                ..Default::default()
            },
            ..Default::default()
        },
        48_000,
        2,
    );
    let frequencies = [31.0, 125.0, 1_000.0, 8_000.0, 16_000.0];
    let mut response = [0.0; 5];
    processor
        .calculate_frequency_response(&frequencies, &mut response)
        .unwrap();
    assert!(response.iter().all(|gain| gain.abs() < 1.0e-5));
}

#[test]
fn every_effect_stage_changes_a_nontrivial_program_independently() {
    let base = AudioDspConfig {
        enabled: true,
        limiter: LimiterConfig {
            enabled: false,
            ..Default::default()
        },
        ..Default::default()
    };
    let effects = [
        (
            "graphic equalizer",
            AudioDspConfig {
                graphic_equalizer: GraphicEqualizerConfig {
                    enabled: true,
                    gains_db: [6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
                    ..Default::default()
                },
                ..base
            },
        ),
        (
            "tone",
            AudioDspConfig {
                tone: ToneControlConfig {
                    enabled: true,
                    bass_gain_db: 6.0,
                    treble_gain_db: -6.0,
                    ..Default::default()
                },
                ..base
            },
        ),
        (
            "equal loudness",
            AudioDspConfig {
                loudness: LoudnessConfig {
                    enabled: true,
                    amount: 1.0,
                    balance: 0.25,
                },
                ..base
            },
        ),
        (
            "mono bass",
            AudioDspConfig {
                mono_bass: MonoBassConfig {
                    enabled: true,
                    crossover_hz: 200.0,
                    amount: 1.0,
                },
                ..base
            },
        ),
        (
            "dynamic equalizer",
            AudioDspConfig {
                dynamic_eq: DynamicEqConfig {
                    enabled: true,
                    amount: 1.0,
                    de_esser_amount: 1.0,
                    de_esser_frequency_hz: 6_500.0,
                },
                ..base
            },
        ),
        (
            "moog ladder",
            AudioDspConfig {
                moog: MoogFilterConfig {
                    enabled: true,
                    cutoff_hz: 2_000.0,
                    resonance: 0.5,
                    drive_db: 6.0,
                    mix: 1.0,
                    ..Default::default()
                },
                ..base
            },
        ),
        (
            "compressor",
            AudioDspConfig {
                compressor: CompressorConfig {
                    enabled: true,
                    threshold_db: -40.0,
                    ratio: 10.0,
                    attack_ms: 0.05,
                    ..Default::default()
                },
                ..base
            },
        ),
        (
            "reverb",
            AudioDspConfig {
                reverb: ReverbConfig {
                    preset: ReverbPreset::Hall,
                    wet: 0.4,
                },
                ..base
            },
        ),
        (
            "crossfeed and width",
            AudioDspConfig {
                stereo_width: StereoWidthConfig {
                    enabled: true,
                    width: 1.8,
                },
                crossfeed: CrossfeedConfig {
                    enabled: true,
                    ..Default::default()
                },
                spatial: SpatialAudioConfig {
                    mode: SpatialMode::CrossfeedAndWidth,
                    ..Default::default()
                },
                ..base
            },
        ),
        (
            "spatial",
            AudioDspConfig {
                spatial: SpatialAudioConfig {
                    mode: SpatialMode::Surround360,
                    intensity: 1.0,
                    azimuth_degrees: 90.0,
                    ..Default::default()
                },
                ..base
            },
        ),
        (
            "speaker output",
            AudioDspConfig {
                speaker_output: SpeakerOutputConfig {
                    enabled: true,
                    mode: SpeakerOutputMode::Powerful,
                    strength: 1.0,
                },
                ..base
            },
        ),
        (
            "peak limiter",
            AudioDspConfig {
                limiter: LimiterConfig {
                    enabled: true,
                    ceiling_db: -12.0,
                    attack_ms: 0.01,
                    ..Default::default()
                },
                ..base
            },
        ),
    ];

    for (name, config) in effects {
        let mut processor = configured(config, 48_000, 2);
        let original = generated_program(48_000, 8_192, 2);
        let mut processed = original.clone();
        processor.process_interleaved_f32(&mut processed).unwrap();
        let maximum_difference = processed
            .iter()
            .zip(&original)
            .map(|(output, input)| (output - input).abs())
            .fold(0.0, f32::max);
        assert!(
            maximum_difference > 1.0e-4,
            "{name} did not change the test program"
        );
        assert!(processed.iter().all(|sample| sample.is_finite()));
    }
}
