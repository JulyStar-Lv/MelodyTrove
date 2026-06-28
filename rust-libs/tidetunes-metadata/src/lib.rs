use std::{
    collections::HashMap,
    io::{self, Read, Seek, SeekFrom},
    sync::{Arc, Mutex},
};

use bytes::Bytes;
use lofty::{
    config::ParseOptions,
    file::{AudioFile, FileType, TaggedFileExt},
    picture::{Picture, PictureInformation, PictureType},
    probe::Probe,
    properties::FileProperties,
    tag::{Accessor, ItemKey, ItemValue, Tag},
};
use tidetunes_remote_storage::{ByteRange, StorageBackend};

const MAX_TEXT_TAG_ENTRIES: usize = 2_048;
const MAX_TEXT_TAG_VALUE_BYTES: usize = 256 * 1024;
const MAX_TEXT_TAG_TOTAL_BYTES: usize = 1024 * 1024;
const MAX_ARTWORK_BYTES: usize = 2 * 1024 * 1024;

#[derive(Debug, Clone, Copy)]
pub struct ReaderLimits {
    pub block_size: u64,
    pub max_requests: usize,
    pub max_read_bytes: u64,
}

impl Default for ReaderLimits {
    fn default() -> Self {
        Self {
            block_size: 256 * 1024,
            max_requests: 64,
            max_read_bytes: 4 * 1024 * 1024,
        }
    }
}

#[derive(Debug, thiserror::Error)]
pub enum MetadataError {
    #[error("invalid reader limits")]
    InvalidLimits,
    #[error("range source failed: {0}")]
    Source(String),
    #[error("metadata scan exceeded request budget ({0})")]
    RequestBudgetExceeded(usize),
    #[error("metadata scan exceeded byte budget ({0})")]
    ByteBudgetExceeded(u64),
    #[error("metadata text tag exceeded value budget ({0} bytes)")]
    TextTagValueTooLarge(usize),
    #[error("metadata text tags exceeded total budget ({0} bytes)")]
    TextTagBudgetExceeded(usize),
    #[error("metadata text tags exceeded entry budget ({0})")]
    TextTagEntryBudgetExceeded(usize),
    #[error(transparent)]
    Io(#[from] io::Error),
    #[error(transparent)]
    Lofty(#[from] lofty::error::LoftyError),
}

pub trait RangeSource: Send + Sync {
    fn len(&self) -> u64;
    fn is_empty(&self) -> bool {
        self.len() == 0
    }
    fn read_range(&self, range: ByteRange) -> Result<Bytes, MetadataError>;
}

pub struct StorageRangeSource {
    backend: Arc<dyn StorageBackend + Send + Sync>,
    path: String,
    len: u64,
}

impl StorageRangeSource {
    pub fn new(
        backend: Arc<dyn StorageBackend + Send + Sync>,
        path: impl Into<String>,
        len: u64,
    ) -> Self {
        Self {
            backend,
            path: path.into(),
            len,
        }
    }
}

impl RangeSource for StorageRangeSource {
    fn len(&self) -> u64 {
        self.len
    }

    fn read_range(&self, range: ByteRange) -> Result<Bytes, MetadataError> {
        tidetunes_runtime::tokio_runtime()
            .block_on(self.backend.get_range(self.path.clone(), range))
            .map_err(|error| MetadataError::Source(error.to_string()))
    }
}

#[derive(Default)]
struct ReaderState {
    cache: HashMap<u64, Bytes>,
    requests: usize,
    read_bytes: u64,
}

pub struct RemoteRangeReader {
    source: Arc<dyn RangeSource>,
    limits: ReaderLimits,
    position: u64,
    state: Mutex<ReaderState>,
}

impl RemoteRangeReader {
    pub fn new(source: Arc<dyn RangeSource>, limits: ReaderLimits) -> Result<Self, MetadataError> {
        if limits.block_size == 0 || limits.max_requests == 0 || limits.max_read_bytes == 0 {
            return Err(MetadataError::InvalidLimits);
        }
        Ok(Self {
            source,
            limits,
            position: 0,
            state: Mutex::new(ReaderState::default()),
        })
    }

    pub fn request_count(&self) -> usize {
        self.state.lock().unwrap().requests
    }

    pub fn fetched_bytes(&self) -> u64 {
        self.state.lock().unwrap().read_bytes
    }

    fn block(&self, block_start: u64) -> Result<Bytes, MetadataError> {
        let mut state = self.state.lock().unwrap();
        if let Some(bytes) = state.cache.get(&block_start) {
            return Ok(bytes.clone());
        }
        if state.requests >= self.limits.max_requests {
            return Err(MetadataError::RequestBudgetExceeded(
                self.limits.max_requests,
            ));
        }

        let file_len = self.source.len();
        let end_inclusive = block_start
            .saturating_add(self.limits.block_size - 1)
            .min(file_len.saturating_sub(1));
        let expected = end_inclusive - block_start + 1;
        if state.read_bytes.saturating_add(expected) > self.limits.max_read_bytes {
            return Err(MetadataError::ByteBudgetExceeded(
                self.limits.max_read_bytes,
            ));
        }

        let range = ByteRange::new(block_start, end_inclusive)
            .map_err(|error| MetadataError::Source(error.to_string()))?;
        let bytes = self.source.read_range(range)?;
        state.requests += 1;
        state.read_bytes += bytes.len() as u64;
        state.cache.insert(block_start, bytes.clone());
        Ok(bytes)
    }
}

impl Read for RemoteRangeReader {
    fn read(&mut self, buf: &mut [u8]) -> io::Result<usize> {
        if buf.is_empty() || self.position >= self.source.len() {
            return Ok(0);
        }

        let mut written = 0;
        while written < buf.len() && self.position < self.source.len() {
            let block_start = self.position / self.limits.block_size * self.limits.block_size;
            let block = self
                .block(block_start)
                .map_err(|error| io::Error::other(error.to_string()))?;
            let offset = (self.position - block_start) as usize;
            if offset >= block.len() {
                break;
            }
            let available = block.len() - offset;
            let remaining = buf.len() - written;
            let count = available.min(remaining);
            buf[written..written + count].copy_from_slice(&block[offset..offset + count]);
            self.position += count as u64;
            written += count;
        }
        Ok(written)
    }
}

impl Seek for RemoteRangeReader {
    fn seek(&mut self, position: SeekFrom) -> io::Result<u64> {
        let next = match position {
            SeekFrom::Start(value) => value as i128,
            SeekFrom::Current(value) => self.position as i128 + value as i128,
            SeekFrom::End(value) => self.source.len() as i128 + value as i128,
        };
        if next < 0 || next > u64::MAX as i128 {
            return Err(io::Error::new(
                io::ErrorKind::InvalidInput,
                "invalid seek position",
            ));
        }
        self.position = next as u64;
        Ok(self.position)
    }
}

#[derive(Debug, Clone, Default, PartialEq)]
pub struct RawMetadataEntry {
    pub key: String,
    pub value: String,
    pub locale: Option<String>,
    pub description: Option<String>,
}

#[derive(Debug, Clone, Default, PartialEq)]
pub struct EmbeddedLyrics {
    pub content: String,
    pub synchronized: bool,
    pub language: Option<String>,
    pub description: Option<String>,
}

#[derive(Debug, Clone, Default, PartialEq)]
pub struct EmbeddedArtwork {
    pub data: Vec<u8>,
    pub mime_type: Option<String>,
    pub picture_type: String,
    pub width: Option<u32>,
    pub height: Option<u32>,
}

#[derive(Debug, Clone, Default, PartialEq)]
pub struct NormalizedMetadata {
    pub title: Option<String>,
    pub artist: Option<String>,
    pub artists: Vec<String>,
    pub album_artist: Option<String>,
    pub album: Option<String>,
    pub composer: Option<String>,
    pub lyricist: Option<String>,
    pub conductor: Option<String>,
    pub genre: Option<String>,
    pub grouping: Option<String>,
    pub comment: Option<String>,
    pub copyright: Option<String>,
    pub publisher: Option<String>,
    pub date: Option<String>,
    pub original_release_date: Option<String>,
    pub track_number: Option<u32>,
    pub track_total: Option<u32>,
    pub disc_number: Option<u32>,
    pub disc_total: Option<u32>,
    pub bpm: Option<f64>,
    pub musical_key: Option<String>,
    pub isrc: Option<String>,
    pub musicbrainz_recording_id: Option<String>,
    pub musicbrainz_track_id: Option<String>,
    pub musicbrainz_release_id: Option<String>,
    pub musicbrainz_release_group_id: Option<String>,
    pub musicbrainz_artist_id: Option<String>,
    pub musicbrainz_release_artist_id: Option<String>,
    pub musicbrainz_work_id: Option<String>,
    pub replay_gain_track_gain: Option<f64>,
    pub replay_gain_track_peak: Option<f64>,
    pub replay_gain_album_gain: Option<f64>,
    pub replay_gain_album_peak: Option<f64>,
    pub lyrics: Option<EmbeddedLyrics>,
    pub artwork: Option<EmbeddedArtwork>,
    pub raw_metadata: Vec<RawMetadataEntry>,
    pub duration_ms: u64,
    pub sample_rate: Option<u32>,
    pub bit_depth: Option<u8>,
    pub channels: Option<u8>,
    pub channel_layout: Option<String>,
    pub overall_bitrate: Option<u32>,
    pub audio_bitrate: Option<u32>,
    pub codec: Option<String>,
    pub container: Option<String>,
    pub lossless: Option<bool>,
}

pub fn read_metadata(
    source: Arc<dyn RangeSource>,
    limits: ReaderLimits,
) -> Result<NormalizedMetadata, MetadataError> {
    let reader = RemoteRangeReader::new(source, limits)?;
    let tagged_file = Probe::new(reader)
        .options(ParseOptions::new().read_cover_art(true))
        .guess_file_type()?
        .read()?;
    let properties = tagged_file.properties();
    let file_type = tagged_file.file_type();
    let tag = tagged_file
        .primary_tag()
        .or_else(|| tagged_file.first_tag());

    normalize_metadata(tag, properties, file_type)
}

fn normalize_metadata(
    tag: Option<&Tag>,
    properties: &FileProperties,
    file_type: FileType,
) -> Result<NormalizedMetadata, MetadataError> {
    let artist = tag.and_then(|tag| tag.artist().map(|value| value.into_owned()));
    let mut artists: Vec<String> = tag
        .map(|tag| {
            tag.get_strings(ItemKey::TrackArtists)
                .map(str::to_owned)
                .collect()
        })
        .unwrap_or_default();
    if let Some(primary) = artist.as_ref().filter(|value| !value.is_empty()) {
        if !artists.contains(primary) {
            artists.insert(0, primary.clone());
        }
    }
    let raw_metadata = match tag {
        Some(tag) => extract_raw_metadata(tag)?,
        None => Vec::new(),
    };
    let lyrics = tag.and_then(extract_lyrics);
    let artwork = tag.and_then(extract_artwork);
    let (codec, container, lossless) = audio_format(file_type);

    Ok(NormalizedMetadata {
        title: tag.and_then(|tag| tag.title().map(|value| value.into_owned())),
        artist,
        artists,
        album_artist: tag
            .and_then(|tag| tag.get_string(ItemKey::AlbumArtist))
            .map(str::to_owned),
        album: tag.and_then(|tag| tag.album().map(|value| value.into_owned())),
        composer: tag.and_then(|tag| text(tag, ItemKey::Composer)),
        lyricist: tag.and_then(|tag| text(tag, ItemKey::Lyricist)),
        conductor: tag.and_then(|tag| text(tag, ItemKey::Conductor)),
        genre: tag.and_then(|tag| tag.genre().map(|value| value.into_owned())),
        grouping: tag.and_then(|tag| text(tag, ItemKey::ContentGroup)),
        comment: tag.and_then(|tag| text(tag, ItemKey::Comment)),
        copyright: tag.and_then(|tag| text(tag, ItemKey::CopyrightMessage)),
        publisher: tag
            .and_then(|tag| text(tag, ItemKey::Publisher).or_else(|| text(tag, ItemKey::Label))),
        date: tag.and_then(|tag| tag.date().map(|value| value.to_string())),
        original_release_date: tag.and_then(|tag| text(tag, ItemKey::OriginalReleaseDate)),
        track_number: tag.and_then(|tag| tag.track()),
        track_total: tag.and_then(|tag| tag.track_total()),
        disc_number: tag.and_then(|tag| tag.disk()),
        disc_total: tag.and_then(|tag| tag.disk_total()),
        bpm: tag.and_then(|tag| {
            text(tag, ItemKey::Bpm)
                .or_else(|| text(tag, ItemKey::IntegerBpm))
                .and_then(|value| value.parse().ok())
        }),
        musical_key: tag.and_then(|tag| text(tag, ItemKey::InitialKey)),
        isrc: tag.and_then(|tag| text(tag, ItemKey::Isrc)),
        musicbrainz_recording_id: tag.and_then(|tag| text(tag, ItemKey::MusicBrainzRecordingId)),
        musicbrainz_track_id: tag.and_then(|tag| text(tag, ItemKey::MusicBrainzTrackId)),
        musicbrainz_release_id: tag.and_then(|tag| text(tag, ItemKey::MusicBrainzReleaseId)),
        musicbrainz_release_group_id: tag
            .and_then(|tag| text(tag, ItemKey::MusicBrainzReleaseGroupId)),
        musicbrainz_artist_id: tag.and_then(|tag| text(tag, ItemKey::MusicBrainzArtistId)),
        musicbrainz_release_artist_id: tag
            .and_then(|tag| text(tag, ItemKey::MusicBrainzReleaseArtistId)),
        musicbrainz_work_id: tag.and_then(|tag| text(tag, ItemKey::MusicBrainzWorkId)),
        replay_gain_track_gain: tag.and_then(|tag| replay_gain(tag, ItemKey::ReplayGainTrackGain)),
        replay_gain_track_peak: tag.and_then(|tag| replay_gain(tag, ItemKey::ReplayGainTrackPeak)),
        replay_gain_album_gain: tag.and_then(|tag| replay_gain(tag, ItemKey::ReplayGainAlbumGain)),
        replay_gain_album_peak: tag.and_then(|tag| replay_gain(tag, ItemKey::ReplayGainAlbumPeak)),
        lyrics,
        artwork,
        raw_metadata,
        duration_ms: properties.duration().as_millis() as u64,
        sample_rate: properties.sample_rate(),
        bit_depth: properties.bit_depth(),
        channels: properties.channels(),
        channel_layout: properties.channel_mask().map(|value| format!("{value:?}")),
        overall_bitrate: properties.overall_bitrate(),
        audio_bitrate: properties.audio_bitrate(),
        codec: Some(codec.to_string()),
        container: Some(container.to_string()),
        lossless,
    })
}

fn extract_artwork(tag: &Tag) -> Option<EmbeddedArtwork> {
    let picture = tag.get_picture_type(PictureType::CoverFront).or_else(|| {
        tag.pictures()
            .iter()
            .find(|picture| !picture.data().is_empty())
    })?;
    if picture.data().is_empty() || picture.data().len() > MAX_ARTWORK_BYTES {
        return None;
    }
    Some(embedded_artwork(picture))
}

fn embedded_artwork(picture: &Picture) -> EmbeddedArtwork {
    let info = PictureInformation::from_picture(picture).ok();
    EmbeddedArtwork {
        data: picture.data().to_vec(),
        mime_type: picture
            .mime_type()
            .map(|mime_type| mime_type.as_str().to_owned()),
        picture_type: format!("{:?}", picture.pic_type()),
        width: info.and_then(|info| non_zero_u32(info.width)),
        height: info.and_then(|info| non_zero_u32(info.height)),
    }
}

fn non_zero_u32(value: u32) -> Option<u32> {
    (value > 0).then_some(value)
}

fn text(tag: &Tag, key: ItemKey) -> Option<String> {
    tag.get_string(key)
        .map(str::trim)
        .filter(|value| !value.is_empty())
        .map(str::to_owned)
}

fn replay_gain(tag: &Tag, key: ItemKey) -> Option<f64> {
    text(tag, key).and_then(|value| {
        value
            .trim_end_matches(|character: char| character.is_ascii_alphabetic())
            .trim()
            .parse()
            .ok()
    })
}

fn extract_lyrics(tag: &Tag) -> Option<EmbeddedLyrics> {
    for (key, synchronized) in [(ItemKey::Lyrics, true), (ItemKey::UnsyncLyrics, false)] {
        for item in tag.get_items(key) {
            let Some(content) = item.value().text().filter(|value| !value.trim().is_empty()) else {
                continue;
            };
            return Some(EmbeddedLyrics {
                content: content.to_owned(),
                synchronized: synchronized && looks_synchronized(content),
                language: language(item.lang()),
                description: non_empty(item.description()),
            });
        }
    }
    None
}

fn looks_synchronized(content: &str) -> bool {
    content.lines().any(|line| {
        line.strip_prefix('[')
            .and_then(|line| line.split_once(']'))
            .is_some_and(|(timestamp, _)| timestamp.contains(':'))
    })
}

fn extract_raw_metadata(tag: &Tag) -> Result<Vec<RawMetadataEntry>, MetadataError> {
    let mut total_bytes = 0;
    let mut entries = Vec::new();
    for item in tag.items() {
        let value = match item.value() {
            ItemValue::Text(value) | ItemValue::Locator(value) => value,
            ItemValue::Binary(_) => continue,
        };
        if value.is_empty() {
            continue;
        }
        if value.len() > MAX_TEXT_TAG_VALUE_BYTES {
            return Err(MetadataError::TextTagValueTooLarge(value.len()));
        }
        total_bytes += value.len();
        if total_bytes > MAX_TEXT_TAG_TOTAL_BYTES {
            return Err(MetadataError::TextTagBudgetExceeded(total_bytes));
        }
        if entries.len() >= MAX_TEXT_TAG_ENTRIES {
            return Err(MetadataError::TextTagEntryBudgetExceeded(
                MAX_TEXT_TAG_ENTRIES,
            ));
        }
        entries.push(RawMetadataEntry {
            key: format!("{:?}", item.key()),
            value: value.clone(),
            locale: language(item.lang()),
            description: non_empty(item.description()),
        });
    }
    Ok(entries)
}

fn language(value: &[u8; 3]) -> Option<String> {
    (value != b"XXX")
        .then(|| String::from_utf8_lossy(value).into_owned())
        .filter(|value| !value.is_empty())
}

fn non_empty(value: &str) -> Option<String> {
    (!value.is_empty()).then(|| value.to_owned())
}

fn audio_format(file_type: FileType) -> (&'static str, &'static str, Option<bool>) {
    match file_type {
        FileType::Aac => ("AAC", "ADTS", Some(false)),
        FileType::Aiff => ("PCM", "AIFF", Some(true)),
        FileType::Ape => ("APE", "APE", Some(true)),
        FileType::Flac => ("FLAC", "FLAC", Some(true)),
        FileType::Mpeg => ("MPEG Audio", "MPEG", Some(false)),
        FileType::Mp4 => ("MPEG-4 Audio", "MP4", None),
        FileType::Mpc => ("Musepack", "MPC", Some(false)),
        FileType::Opus => ("Opus", "Ogg", Some(false)),
        FileType::Vorbis => ("Vorbis", "Ogg", Some(false)),
        FileType::Speex => ("Speex", "Ogg", Some(false)),
        FileType::Wav => ("PCM", "WAV", Some(true)),
        FileType::WavPack => ("WavPack", "WavPack", Some(true)),
        FileType::Custom(_) => ("Unknown", "Unknown", None),
        _ => ("Unknown", "Unknown", None),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use lofty::picture::{MimeType, Picture, PictureType};
    use lofty::tag::{TagItem, TagType};

    struct MemorySource(Bytes);

    impl RangeSource for MemorySource {
        fn len(&self) -> u64 {
            self.0.len() as u64
        }

        fn read_range(&self, range: ByteRange) -> Result<Bytes, MetadataError> {
            let start = range.start as usize;
            let end = (range.end_inclusive as usize + 1).min(self.0.len());
            Ok(self.0.slice(start..end))
        }
    }

    #[test]
    fn reads_and_seeks_with_block_cache() {
        let source = Arc::new(MemorySource(Bytes::from_static(b"0123456789")));
        let mut reader = RemoteRangeReader::new(
            source,
            ReaderLimits {
                block_size: 4,
                max_requests: 3,
                max_read_bytes: 12,
            },
        )
        .unwrap();

        let mut first = [0; 3];
        reader.read_exact(&mut first).unwrap();
        assert_eq!(&first, b"012");
        reader.seek(SeekFrom::Start(1)).unwrap();
        let mut cached = [0; 2];
        reader.read_exact(&mut cached).unwrap();
        assert_eq!(&cached, b"12");
        reader.seek(SeekFrom::End(-2)).unwrap();
        let mut tail = [0; 2];
        reader.read_exact(&mut tail).unwrap();
        assert_eq!(&tail, b"89");
        assert_eq!(reader.request_count(), 2);
    }

    #[test]
    fn enforces_byte_budget() {
        let source = Arc::new(MemorySource(Bytes::from_static(b"0123456789")));
        let mut reader = RemoteRangeReader::new(
            source,
            ReaderLimits {
                block_size: 4,
                max_requests: 3,
                max_read_bytes: 4,
            },
        )
        .unwrap();
        let mut data = [0; 5];
        let error = reader.read_exact(&mut data).unwrap_err();
        assert!(error.to_string().contains("byte budget"));
    }

    #[test]
    fn reads_wav_properties_through_range_reader() {
        let wav = minimal_pcm_wav();
        let metadata = read_metadata(
            Arc::new(MemorySource(Bytes::from(wav))),
            ReaderLimits {
                block_size: 16,
                max_requests: 16,
                max_read_bytes: 1024,
            },
        )
        .unwrap();

        assert_eq!(metadata.sample_rate, Some(8_000));
        assert_eq!(metadata.bit_depth, Some(16));
        assert_eq!(metadata.channels, Some(1));
        assert_eq!(metadata.codec.as_deref(), Some("PCM"));
        assert_eq!(metadata.container.as_deref(), Some("WAV"));
        assert_eq!(metadata.lossless, Some(true));
    }

    #[test]
    fn normalizes_extended_text_tags_lyrics_and_raw_metadata() {
        let mut tag = Tag::new(TagType::VorbisComments);
        tag.insert_text(ItemKey::TrackTitle, "Song".to_string());
        tag.insert_text(ItemKey::TrackArtist, "Primary".to_string());
        tag.push(TagItem::new(
            ItemKey::TrackArtists,
            ItemValue::Text("Guest".to_string()),
        ));
        tag.insert_text(ItemKey::Composer, "Composer".to_string());
        tag.insert_text(ItemKey::Lyricist, "Lyricist".to_string());
        tag.insert_text(ItemKey::Conductor, "Conductor".to_string());
        tag.insert_text(ItemKey::ContentGroup, "Suite".to_string());
        tag.insert_text(ItemKey::CopyrightMessage, "Copyright".to_string());
        tag.insert_text(ItemKey::Label, "Label".to_string());
        tag.insert_text(ItemKey::OriginalReleaseDate, "1999-01-01".to_string());
        tag.insert_text(ItemKey::Bpm, "128.5".to_string());
        tag.insert_text(ItemKey::InitialKey, "8A".to_string());
        tag.insert_text(ItemKey::Isrc, "US-AAA-26-00001".to_string());
        tag.insert_text(ItemKey::MusicBrainzRecordingId, "recording-id".to_string());
        tag.insert_text(ItemKey::ReplayGainTrackGain, "-7.25 dB".to_string());
        let mut lyrics = TagItem::new(
            ItemKey::Lyrics,
            ItemValue::Text("[00:01.00]Line".to_string()),
        );
        lyrics.set_lang(*b"eng");
        lyrics.set_description("main".to_string());
        tag.push(lyrics);

        let metadata = normalize_metadata(
            Some(&tag),
            &FileProperties::new(
                std::time::Duration::from_secs(180),
                Some(1_000),
                Some(900),
                Some(48_000),
                Some(24),
                Some(2),
                None,
            ),
            FileType::Flac,
        )
        .unwrap();

        assert_eq!(metadata.title.as_deref(), Some("Song"));
        assert_eq!(metadata.artists, vec!["Primary", "Guest"]);
        assert_eq!(metadata.composer.as_deref(), Some("Composer"));
        assert_eq!(metadata.lyricist.as_deref(), Some("Lyricist"));
        assert_eq!(metadata.conductor.as_deref(), Some("Conductor"));
        assert_eq!(metadata.grouping.as_deref(), Some("Suite"));
        assert_eq!(metadata.publisher.as_deref(), Some("Label"));
        assert_eq!(
            metadata.original_release_date.as_deref(),
            Some("1999-01-01")
        );
        assert_eq!(metadata.bpm, Some(128.5));
        assert_eq!(metadata.musical_key.as_deref(), Some("8A"));
        assert_eq!(metadata.isrc.as_deref(), Some("US-AAA-26-00001"));
        assert_eq!(
            metadata.musicbrainz_recording_id.as_deref(),
            Some("recording-id")
        );
        assert_eq!(metadata.replay_gain_track_gain, Some(-7.25));
        assert_eq!(
            metadata.lyrics,
            Some(EmbeddedLyrics {
                content: "[00:01.00]Line".to_string(),
                synchronized: true,
                language: Some("eng".to_string()),
                description: Some("main".to_string()),
            })
        );
        assert!(metadata
            .raw_metadata
            .iter()
            .any(|entry| entry.key == "Composer" && entry.value == "Composer"));
        assert_eq!(metadata.codec.as_deref(), Some("FLAC"));
        assert_eq!(metadata.lossless, Some(true));
    }

    #[test]
    fn extracts_bounded_embedded_artwork() {
        let mut tag = Tag::new(TagType::VorbisComments);
        tag.push_picture(
            Picture::unchecked(minimal_png(320, 240))
                .pic_type(PictureType::CoverFront)
                .mime_type(MimeType::Png)
                .build(),
        );

        let metadata =
            normalize_metadata(Some(&tag), &FileProperties::default(), FileType::Flac).unwrap();

        let artwork = metadata.artwork.expect("artwork should be extracted");
        assert_eq!(artwork.mime_type.as_deref(), Some("image/png"));
        assert_eq!(artwork.picture_type, "CoverFront");
        assert_eq!(artwork.width, Some(320));
        assert_eq!(artwork.height, Some(240));
        assert_eq!(artwork.data, minimal_png(320, 240));
    }

    #[test]
    fn skips_oversized_embedded_artwork() {
        let mut tag = Tag::new(TagType::VorbisComments);
        tag.push_picture(
            Picture::unchecked(vec![1; MAX_ARTWORK_BYTES + 1])
                .pic_type(PictureType::CoverFront)
                .mime_type(MimeType::Jpeg)
                .build(),
        );

        let metadata =
            normalize_metadata(Some(&tag), &FileProperties::default(), FileType::Flac).unwrap();

        assert_eq!(metadata.artwork, None);
    }

    #[test]
    fn rejects_oversized_text_metadata() {
        let mut tag = Tag::new(TagType::VorbisComments);
        tag.insert_text(ItemKey::Comment, "x".repeat(MAX_TEXT_TAG_VALUE_BYTES + 1));

        let error =
            normalize_metadata(Some(&tag), &FileProperties::default(), FileType::Flac).unwrap_err();

        assert!(matches!(error, MetadataError::TextTagValueTooLarge(_)));
    }

    fn minimal_pcm_wav() -> Vec<u8> {
        let data = [0_u8; 16];
        let mut wav = Vec::new();
        wav.extend_from_slice(b"RIFF");
        wav.extend_from_slice(&(36_u32 + data.len() as u32).to_le_bytes());
        wav.extend_from_slice(b"WAVEfmt ");
        wav.extend_from_slice(&16_u32.to_le_bytes());
        wav.extend_from_slice(&1_u16.to_le_bytes());
        wav.extend_from_slice(&1_u16.to_le_bytes());
        wav.extend_from_slice(&8_000_u32.to_le_bytes());
        wav.extend_from_slice(&16_000_u32.to_le_bytes());
        wav.extend_from_slice(&2_u16.to_le_bytes());
        wav.extend_from_slice(&16_u16.to_le_bytes());
        wav.extend_from_slice(b"data");
        wav.extend_from_slice(&(data.len() as u32).to_le_bytes());
        wav.extend_from_slice(&data);
        wav
    }

    fn minimal_png(width: u32, height: u32) -> Vec<u8> {
        let mut png = Vec::new();
        png.extend_from_slice(&[0x89, b'P', b'N', b'G', 0x0D, 0x0A, 0x1A, 0x0A]);
        png.extend_from_slice(&13_u32.to_be_bytes());
        png.extend_from_slice(b"IHDR");
        png.extend_from_slice(&width.to_be_bytes());
        png.extend_from_slice(&height.to_be_bytes());
        png.extend_from_slice(&[8, 2, 0, 0, 0]);
        png.extend_from_slice(&0_u32.to_be_bytes());
        png
    }
}
