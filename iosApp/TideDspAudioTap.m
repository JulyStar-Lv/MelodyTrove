#import "TideDspAudioTap.h"

#import <AudioToolbox/AudioToolbox.h>
#import <MediaToolbox/MediaToolbox.h>
#import <stdlib.h>

// Implemented by the Rust app_backend static library embedded in SharedKit.
extern int32_t tide_audio_dsp_retain(uint64_t handle);
extern void tide_audio_dsp_release(uint64_t handle);
extern int32_t tide_audio_dsp_configure_format(
    uint64_t handle,
    uint32_t sampleRate,
    uint32_t channels);
extern void tide_audio_dsp_reset(uint64_t handle);
extern int32_t tide_audio_dsp_process_interleaved_f32(
    uint64_t handle,
    float *samples,
    uint32_t frames,
    uint32_t channels);
extern int32_t tide_audio_dsp_process_planar_f32(
    uint64_t handle,
    float **channelBuffers,
    uint32_t frames,
    uint32_t channels);
extern int32_t tide_audio_dsp_process_interleaved_i16(
    uint64_t handle,
    int16_t *samples,
    uint32_t sampleCount);

typedef struct {
    uint64_t dspHandle;
    AudioStreamBasicDescription format;
    bool configured;
} TideDspTapContext;

static void TideDspTapInit(
    MTAudioProcessingTapRef tap,
    void *clientInfo,
    void **tapStorageOut) {
    (void)tap;
    *tapStorageOut = clientInfo;
}

static void TideDspTapFinalize(MTAudioProcessingTapRef tap) {
    TideDspTapContext *context = MTAudioProcessingTapGetStorage(tap);
    if (context == NULL) {
        return;
    }
    tide_audio_dsp_release(context->dspHandle);
    free(context);
}

static void TideDspTapPrepare(
    MTAudioProcessingTapRef tap,
    CMItemCount maxFrames,
    const AudioStreamBasicDescription *processingFormat) {
    (void)maxFrames;
    TideDspTapContext *context = MTAudioProcessingTapGetStorage(tap);
    if (context == NULL || processingFormat == NULL) {
        return;
    }
    context->format = *processingFormat;
    context->configured =
        tide_audio_dsp_configure_format(
            context->dspHandle,
            (uint32_t)processingFormat->mSampleRate,
            processingFormat->mChannelsPerFrame) == 0;
    if (context->configured) {
        tide_audio_dsp_reset(context->dspHandle);
    }
}

static void TideDspTapUnprepare(MTAudioProcessingTapRef tap) {
    TideDspTapContext *context = MTAudioProcessingTapGetStorage(tap);
    if (context != NULL && context->configured) {
        tide_audio_dsp_reset(context->dspHandle);
    }
}

static void TideDspTapProcess(
    MTAudioProcessingTapRef tap,
    CMItemCount requestedFrames,
    MTAudioProcessingTapFlags flags,
    AudioBufferList *bufferListInOut,
    CMItemCount *numberFramesOut,
    MTAudioProcessingTapFlags *flagsOut) {
    (void)flags;
    CMItemCount sourceFrames = 0;
    MTAudioProcessingTapFlags sourceFlags = 0;
    OSStatus status = MTAudioProcessingTapGetSourceAudio(
        tap,
        requestedFrames,
        bufferListInOut,
        &sourceFlags,
        NULL,
        &sourceFrames);
    *numberFramesOut = status == noErr ? sourceFrames : 0;
    *flagsOut = sourceFlags;
    if (status != noErr || sourceFrames <= 0) {
        return;
    }

    TideDspTapContext *context = MTAudioProcessingTapGetStorage(tap);
    if (context == NULL || !context->configured) {
        return;
    }
    if ((sourceFlags & kMTAudioProcessingTapFlag_StartOfStream) != 0) {
        tide_audio_dsp_reset(context->dspHandle);
    }

    const AudioStreamBasicDescription format = context->format;
    const uint32_t channels = format.mChannelsPerFrame;
    const bool isLinearPcm = format.mFormatID == kAudioFormatLinearPCM;
    const bool isFloat = (format.mFormatFlags & kAudioFormatFlagIsFloat) != 0;
    const bool isNonInterleaved =
        (format.mFormatFlags & kAudioFormatFlagIsNonInterleaved) != 0;
    if (!isLinearPcm || channels == 0 || channels > 2) {
        return;
    }

    if (isFloat && format.mBitsPerChannel == 32) {
        if (isNonInterleaved && bufferListInOut->mNumberBuffers >= channels) {
            float *channelBuffers[2] = {NULL, NULL};
            for (uint32_t channel = 0; channel < channels; channel++) {
                channelBuffers[channel] =
                    (float *)bufferListInOut->mBuffers[channel].mData;
                if (channelBuffers[channel] == NULL) {
                    return;
                }
            }
            tide_audio_dsp_process_planar_f32(
                context->dspHandle,
                channelBuffers,
                (uint32_t)sourceFrames,
                channels);
        } else if (
            !isNonInterleaved &&
            bufferListInOut->mNumberBuffers == 1 &&
            bufferListInOut->mBuffers[0].mData != NULL) {
            tide_audio_dsp_process_interleaved_f32(
                context->dspHandle,
                (float *)bufferListInOut->mBuffers[0].mData,
                (uint32_t)sourceFrames,
                channels);
        }
    } else if (
        !isFloat &&
        !isNonInterleaved &&
        format.mBitsPerChannel == 16 &&
        bufferListInOut->mNumberBuffers == 1 &&
        bufferListInOut->mBuffers[0].mData != NULL) {
        tide_audio_dsp_process_interleaved_i16(
            context->dspHandle,
            (int16_t *)bufferListInOut->mBuffers[0].mData,
            (uint32_t)sourceFrames * channels);
    }
}

bool TideDspAudioTapAttach(AVPlayerItem *item, uint64_t dspHandle) {
    if (item == nil || dspHandle == 0) {
        return false;
    }
    AVAssetTrack *audioTrack =
        [[item.asset tracksWithMediaType:AVMediaTypeAudio] firstObject];
    if (audioTrack == nil) {
        return false;
    }

    TideDspTapContext *context = calloc(1, sizeof(TideDspTapContext));
    if (context == NULL || tide_audio_dsp_retain(dspHandle) != 0) {
        free(context);
        return false;
    }
    context->dspHandle = dspHandle;

    MTAudioProcessingTapCallbacks callbacks;
    callbacks.version = kMTAudioProcessingTapCallbacksVersion_0;
    callbacks.clientInfo = context;
    callbacks.init = TideDspTapInit;
    callbacks.finalize = TideDspTapFinalize;
    callbacks.prepare = TideDspTapPrepare;
    callbacks.unprepare = TideDspTapUnprepare;
    callbacks.process = TideDspTapProcess;

    MTAudioProcessingTapRef tap = NULL;
    OSStatus status = MTAudioProcessingTapCreate(
        kCFAllocatorDefault,
        &callbacks,
        kMTAudioProcessingTapCreationFlag_PostEffects,
        &tap);
    if (status != noErr || tap == NULL) {
        tide_audio_dsp_release(dspHandle);
        free(context);
        return false;
    }

    AVMutableAudioMixInputParameters *inputParameters =
        [AVMutableAudioMixInputParameters
            audioMixInputParametersWithTrack:audioTrack];
    inputParameters.audioTapProcessor = tap;
    AVMutableAudioMix *audioMix = [AVMutableAudioMix audioMix];
    audioMix.inputParameters = @[inputParameters];
    item.audioMix = audioMix;
    CFRelease(tap);
    return true;
}

void TideDspAudioTapDetach(AVPlayerItem *item) {
    item.audioMix = nil;
}

void TideDspAudioTapReset(uint64_t dspHandle) {
    if (dspHandle != 0) {
        tide_audio_dsp_reset(dspHandle);
    }
}
