#import <AVFoundation/AVFoundation.h>
#import <stdbool.h>
#import <stdint.h>

NS_ASSUME_NONNULL_BEGIN

/// Attaches an in-place post-effects processing tap to the item's first audio track.
bool TideDspAudioTapAttach(AVPlayerItem *item, uint64_t dspHandle);

/// Detaches the audio mix and releases the tap after in-flight callbacks finish.
void TideDspAudioTapDetach(AVPlayerItem *item);

/// Clears history after seeks and explicit discontinuities.
void TideDspAudioTapReset(uint64_t dspHandle);

NS_ASSUME_NONNULL_END
