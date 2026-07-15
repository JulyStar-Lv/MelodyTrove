# TideTunes Room KMP Schema

Date: 2026-07-03

The shared Room database is `tidetunes.db`. Android, iOS, and Desktop use
platform-specific builders with bundled SQLite. Schema versions 1 through 7 are
exported under
`shared/schemas/com.github.tidetunes.database.TideTunesDatabase/`.

## Ownership

- Room is the UI-facing source of truth for library, playlist, sync, and
  download state.
- Canonical library tables are source-agnostic. Tracks, albums, artists, genres,
  lyrics, artwork, raw metadata, playlists, and downloads are not owned by any
  provider.
- `RemoteLibraryImportCoordinator` is the write boundary from source scan data
  into canonical Room tables.
- Source adapters authenticate, browse, scan, and resolve playback resources;
  they do not write canonical DAOs directly.
- Rust owns remote access, bounded range reads, metadata parsing, and scanning,
  but it does not open or own an app database.
- Credentials, access tokens, cookies, signed URLs, playback headers, and
  temporary loopback URLs are never persisted in Room.

## Current Tables

| Table | Purpose | Important constraints |
| --- | --- | --- |
| `source_account` | Local, WebDAV, OneDrive, and future provider account metadata | Provider type, display name, endpoint/account hints, credential reference only |
| `library_root` | User-selected import roots and root-level sync state | Unique source-account/provider-root and source-account/path identities |
| `source_item` | Provider inventory item identity and file facts | Unique source-account/provider-item and source-account/path identities; deletion and scan markers |
| `source_item_property` | Extensible provider-specific item attributes | Key/value rows scoped to one source item |
| `track_source_ref` | Relationship between canonical tracks and playable source items | One source item maps to one canonical track; availability/download/preference flags |
| `source_sync_cursor` | Delta or scan checkpoint state | One cursor per source account, library root, and cursor type |
| `source_error` | Persisted source/import errors | Scoped to account, root, and optionally source item |
| `track` | Canonical normalized audio metadata | No provider ownership fields; indexed title, ISRC, MusicBrainz IDs |
| `album`, `artist`, `genre` | Canonical library dimensions | Unique normalized names |
| `track_artist`, `album_artist`, `track_genre` | Ordered many-to-many metadata | Foreign-key cascades |
| `artwork` | Extracted artwork cache metadata | Artwork bytes stay outside Room |
| `lyrics` | Embedded or sidecar lyrics | One current lyric row per track |
| `raw_metadata` | Unmapped source tags | Indexed by track and tag key |
| `import_job` | Resumable import progress and errors | References `library_root` |
| `download_task` | Offline download task state and progress | Unique source/media/remote ID; indexed status and update time |
| `playlist`, `playlist_track` | User playlists and stable ordering | Foreign-key cascades and ordered indexes |
| `track_fts` | Full-text search index for local library search | FTS4 content table backed by `track` |

## 表与字段说明（中文）

本节说明当前 Room schema 中每个业务表的职责和字段含义。时间字段除特别说明外均为毫秒级 Unix epoch。密码、访问令牌、Cookie、临时播放 URL 和请求头不写入 Room。

### `source_account`

用途：保存一个音乐来源账号或来源入口的非敏感元数据，例如本地、WebDAV、OneDrive。真实密码和 token 只通过 `credentialRef` 指向安全凭据存储。

| 字段 | 含义 |
| --- | --- |
| `id` | 来源账号主键；Rust/UniFFI 侧的 `StorageId` 与这里保持一致。 |
| `providerType` | 来源类型，如 `local`、`webdav`、`onedrive`。 |
| `displayName` | UI 中展示的来源名称。 |
| `endpoint` | WebDAV 地址、本地路径或其他非敏感连接端点。 |
| `externalAccountId` | 第三方来源的外部账号、drive 或 provider ID；没有则为空。 |
| `credentialRef` | 指向安全凭据存储的引用，不保存密钥本身。 |
| `priority` | 来源排序或播放候选排序权重。 |
| `enabled` | 来源是否启用；禁用后不会作为可播放候选。 |
| `createdAt` | 来源账号首次创建时间。 |
| `updatedAt` | 来源账号最近更新时间。 |

### `library_root`

用途：记录用户选择导入或同步的根目录，并保存该根目录的同步状态。

| 字段 | 含义 |
| --- | --- |
| `id` | 导入根目录主键。 |
| `sourceAccountId` | 所属 `source_account.id`。 |
| `providerRootId` | provider 返回的稳定根目录 ID；适合 OneDrive 等有稳定 ID 的来源。 |
| `canonicalPath` | 规范化根路径；适合 WebDAV、本地路径等路径型来源。 |
| `displayName` | UI 中展示的根目录名称。 |
| `syncStatus` | 当前同步状态，如运行、已同步、失败、暂停、取消。 |
| `syncCursor` | 旧兼容游标字段；新 delta 游标主要写入 `source_sync_cursor`。 |
| `lastSyncAt` | 最近一次同步结束或状态更新时间。 |
| `createdAt` | 根目录首次创建时间。 |
| `updatedAt` | 根目录最近更新时间。 |

### `source_item`

用途：保存来源侧文件、目录或媒体对象的身份和文件事实。它是 provider 清单与规范化曲库之间的桥梁。

| 字段 | 含义 |
| --- | --- |
| `id` | 来源对象主键。 |
| `sourceAccountId` | 所属 `source_account.id`。 |
| `libraryRootId` | 所属 `library_root.id`；根外或临时对象可为空。 |
| `itemType` | 对象类型，如 `folder`、`file`、`track`。 |
| `providerItemId` | provider 返回的稳定对象 ID；没有稳定 ID 时为空。 |
| `parentProviderItemId` | provider 返回的父对象 ID，用于目录层级和移动识别。 |
| `canonicalPath` | 规范化路径；同一来源内唯一。 |
| `displayPath` | 用于展示或调试的路径。 |
| `displayName` | 文件或对象展示名。 |
| `mimeType` | provider 或扫描器识别出的 MIME 类型。 |
| `sizeBytes` | 文件大小，单位字节。 |
| `etag` | provider 返回的实体标签，用于判断文件是否变化。 |
| `revision` | provider 的版本号或修订标识。 |
| `createdAtRemote` | 来源侧创建时间。 |
| `modifiedAtRemote` | 来源侧修改时间。 |
| `contentHash` | 内容哈希；当前用于去重或未来校验。 |
| `audioFingerprint` | 音频指纹；当前用于未来跨来源匹配。 |
| `isDeleted` | 来源对象是否已在最近同步中被标记删除。 |
| `firstSyncedAt` | 第一次写入 Room 的时间。 |
| `lastSyncedAt` | 最近一次同步更新该对象的时间。 |
| `lastSeenScanId` | 最近一次完整扫描看到该对象的扫描 ID。 |

### `source_item_property`

用途：保存 provider 特有、暂时不适合提升为固定字段的扩展属性。

| 字段 | 含义 |
| --- | --- |
| `sourceItemId` | 所属 `source_item.id`。 |
| `propertyKey` | 属性键；与 `sourceItemId` 组成联合主键。 |
| `stringValue` | 字符串属性值。 |
| `longValue` | 整数属性值。 |
| `doubleValue` | 浮点属性值。 |
| `booleanValue` | 布尔属性值。 |

### `track_source_ref`

用途：连接规范化曲目 `track` 和来源对象 `source_item`。播放、下载和可见性都从这里判断，而不是把 provider 字段写回 `track`。

| 字段 | 含义 |
| --- | --- |
| `trackId` | 关联的 `track.id`；联合主键的一部分。 |
| `sourceItemId` | 关联的 `source_item.id`；联合主键的一部分，且一个来源对象只能映射到一个曲目。 |
| `role` | 来源引用角色，如主音频文件。 |
| `matchMethod` | 曲目匹配方法，如来源身份、MusicBrainz、ISRC、严格元数据匹配。 |
| `matchConfidence` | 匹配置信度，数值越高越可靠。 |
| `isPreferred` | 是否为该曲目的优先来源。 |
| `isAvailable` | 来源对象当前是否可用。 |
| `isDownloaded` | 是否已有本地离线副本。 |
| `playable` | 是否可用于播放。 |
| `downloadable` | 是否可用于下载。 |
| `codec` | 音频编码，如 FLAC、AAC。 |
| `container` | 容器格式，如 FLAC、MP4。 |
| `bitRate` | 比特率，通常为 kbps。 |
| `sampleRate` | 采样率，单位 Hz。 |
| `bitsPerSample` | 位深。 |
| `channels` | 声道数。 |
| `lossless` | 是否无损。 |
| `createdAt` | 引用创建时间。 |
| `updatedAt` | 引用最近更新时间。 |

### `source_sync_cursor`

用途：保存来源同步游标，例如 OneDrive deltaLink 或完整扫描 checkpoint。

| 字段 | 含义 |
| --- | --- |
| `id` | 游标主键。 |
| `sourceAccountId` | 所属 `source_account.id`。 |
| `libraryRootId` | 所属 `library_root.id`；账号级游标可为空。 |
| `cursorType` | 游标类型，如 `delta`。 |
| `cursorValue` | provider 返回的游标字符串。 |
| `lastScanId` | 产生或更新该游标的扫描 ID。 |
| `lastSyncAt` | 游标最近成功持久化时间。 |

### `source_error`

用途：记录来源连接、扫描、导入过程中可展示或可诊断的错误。

| 字段 | 含义 |
| --- | --- |
| `id` | 错误记录主键。 |
| `sourceAccountId` | 关联的来源账号。 |
| `libraryRootId` | 关联的导入根目录；账号级错误可为空。 |
| `sourceItemId` | 关联的来源对象；非对象级错误可为空。 |
| `importJobId` | 关联的扫描/导入任务；历史错误或非任务错误可为空。 |
| `errorType` | 错误类型或分类。 |
| `message` | 错误消息。 |
| `createdAt` | 错误创建时间。 |
| `resolvedAt` | 错误解决时间；未解决时为空。 |

### `track`

用途：保存规范化曲目元数据。它不归属于某一个 provider；同一首歌可以通过多个 `track_source_ref` 指向不同来源。

| 字段 | 含义 |
| --- | --- |
| `id` | 曲目主键；由导入逻辑生成的稳定 ID。 |
| `title` | 曲目标题。 |
| `sortTitle` | 排序标题。 |
| `albumId` | 所属 `album.id`；未知专辑时为空。 |
| `albumArtist` | 专辑艺术家文本。 |
| `composer` | 作曲者。 |
| `comment` | 元数据注释。 |
| `grouping` | 分组标签。 |
| `durationMs` | 曲目时长，单位毫秒。 |
| `discNumber` | 碟号。 |
| `discTotal` | 总碟数。 |
| `trackNumber` | 曲目序号。 |
| `trackTotal` | 总曲目数。 |
| `year` | 年份。 |
| `date` | 原始日期文本。 |
| `sampleRate` | 采样率，单位 Hz。 |
| `bitRate` | 比特率，通常为 kbps。 |
| `bitsPerSample` | 位深。 |
| `channels` | 声道数。 |
| `channelLayout` | 声道布局文本。 |
| `codec` | 音频编码。 |
| `container` | 文件容器格式。 |
| `lossless` | 是否无损。 |
| `createdAt` | 曲目记录创建时间。 |
| `updatedAt` | 曲目记录最近更新时间。 |
| `lastPlayedAt` | 最近播放时间。 |
| `artist` | 主艺术家文本，用于快速展示和搜索。 |
| `lyricist` | 作词者。 |
| `conductor` | 指挥。 |
| `copyright` | 版权信息。 |
| `publisher` | 发行方。 |
| `originalReleaseDate` | 原始发行日期。 |
| `bpm` | 每分钟节拍数。 |
| `musicalKey` | 调性。 |
| `isrc` | ISRC 国际标准录音编码。 |
| `musicBrainzRecordingId` | MusicBrainz recording ID。 |
| `musicBrainzTrackId` | MusicBrainz track ID。 |
| `musicBrainzReleaseId` | MusicBrainz release ID。 |
| `musicBrainzReleaseGroupId` | MusicBrainz release group ID。 |
| `musicBrainzArtistId` | MusicBrainz artist ID。 |
| `musicBrainzReleaseArtistId` | MusicBrainz release artist ID。 |
| `musicBrainzWorkId` | MusicBrainz work ID。 |
| `replayGainTrackGain` | ReplayGain 单曲增益。 |
| `replayGainTrackPeak` | ReplayGain 单曲峰值。 |
| `replayGainAlbumGain` | ReplayGain 专辑增益。 |
| `replayGainAlbumPeak` | ReplayGain 专辑峰值。 |

### `album`

用途：保存规范化专辑维度，供曲库、搜索、专辑页复用。

| 字段 | 含义 |
| --- | --- |
| `id` | 专辑主键。 |
| `name` | 专辑名称。 |
| `normalizedName` | 规范化名称，用于去重和查找。 |
| `sortName` | 排序名称。 |
| `year` | 专辑年份。 |
| `artworkId` | 专辑级封面 `artwork.id`；没有时为空。 |

### `artist`

用途：保存规范化艺术家维度。

| 字段 | 含义 |
| --- | --- |
| `id` | 艺术家主键。 |
| `name` | 艺术家名称。 |
| `normalizedName` | 规范化名称，用于去重和查找。 |
| `sortName` | 排序名称。 |

### `genre`

用途：保存规范化流派维度。

| 字段 | 含义 |
| --- | --- |
| `id` | 流派主键。 |
| `name` | 流派名称。 |
| `normalizedName` | 规范化名称，用于去重和查找。 |

### `track_artist`

用途：保存曲目与艺术家的多对多关系，并保留艺术家顺序。

| 字段 | 含义 |
| --- | --- |
| `trackId` | 关联的 `track.id`；联合主键的一部分。 |
| `artistId` | 关联的 `artist.id`；联合主键的一部分。 |
| `position` | 艺术家在曲目元数据中的顺序。 |

### `album_artist`

用途：保存专辑与艺术家的多对多关系，并保留专辑艺术家顺序。

| 字段 | 含义 |
| --- | --- |
| `albumId` | 关联的 `album.id`；联合主键的一部分。 |
| `artistId` | 关联的 `artist.id`；联合主键的一部分。 |
| `position` | 艺术家在专辑元数据中的顺序。 |

### `track_genre`

用途：保存曲目与流派的多对多关系。

| 字段 | 含义 |
| --- | --- |
| `trackId` | 关联的 `track.id`；联合主键的一部分。 |
| `genreId` | 关联的 `genre.id`；联合主键的一部分。 |

### `artwork`

用途：保存封面缓存的元数据。图片二进制不进 Room，只保存本地缓存路径和识别信息。

| 字段 | 含义 |
| --- | --- |
| `id` | 封面主键。 |
| `trackId` | 曲目级封面关联的 `track.id`。 |
| `albumId` | 专辑级封面关联的 `album.id`。 |
| `contentHash` | 图片内容哈希，用于去重和缓存命中。 |
| `localPath` | 原图缓存文件路径。 |
| `thumbnailPath` | 缩略图缓存路径。 |
| `width` | 图片宽度。 |
| `height` | 图片高度。 |
| `mimeType` | 图片 MIME 类型。 |
| `pictureType` | 图片类型，如 front cover。 |

### `lyrics`

用途：保存嵌入歌词或同目录歌词文件解析后的当前歌词内容。

| 字段 | 含义 |
| --- | --- |
| `id` | 歌词主键。 |
| `trackId` | 所属 `track.id`；每首曲目最多一条当前歌词。 |
| `format` | 歌词格式，如 LRC、TTML、plain text。 |
| `language` | 歌词语言。 |
| `synchronized` | 是否带时间轴同步。 |
| `content` | 歌词文本内容。 |
| `sourcePath` | 歌词来源路径或来源描述。 |
| `updatedAt` | 歌词最近更新时间。 |

### `raw_metadata`

用途：保存解析到但尚未映射到规范字段的原始标签，方便调试、未来迁移和高级展示。

| 字段 | 含义 |
| --- | --- |
| `id` | 原始标签主键。 |
| `trackId` | 所属 `track.id`。 |
| `tagKey` | 原始标签键。 |
| `value` | 原始标签值。 |
| `locale` | 标签语言或地区。 |
| `description` | 标签描述。 |

### `import_job`

用途：保存导入任务的进度、结果和错误，支持 UI 展示、暂停/取消/恢复语义。

| 字段 | 含义 |
| --- | --- |
| `id` | 导入任务 ID，通常也是 scan/job ID。 |
| `libraryRootId` | 导入目标 `library_root.id`。 |
| `status` | 任务状态，如 queued、running、paused、cancelled、completed、failed。 |
| `scannedCount` | 已扫描音乐项数量。 |
| `importedCount` | 已成功导入曲目数量。 |
| `skippedCount` | 因未变化等原因跳过的数量。 |
| `failedCount` | 导入失败数量。 |
| `metadataScanMode` | 任务创建时的元数据扫描模式快照：Fast、Standard 或 Full。 |
| `metadataConcurrency` | 元数据读取并发度快照。 |
| `importBatchSize` | 导入批大小快照。 |
| `scanSubdirectories` | 是否递归扫描子目录。 |
| `ignoreShortAudio` / `minDurationMs` | 短音频过滤规则快照。 |
| `ignoreHiddenFiles` / `ignoredDirectoryNames` | 隐藏文件和忽略目录规则快照。 |
| `missingFilePolicy` | 缺失文件处理策略快照。 |
| `duplicateTrackPolicy` | 重复曲目处理策略快照。 |
| `metadataRequestCount` | 元数据 Range 请求累计次数。 |
| `metadataFetchedBytes` | 元数据 Range 请求累计传输字节。 |
| `metadataElapsedMs` | 元数据读取累计耗时，单位毫秒。 |
| `artworkCachedBytes` | 本次任务新增写入封面缓存的字节数。 |
| `checkpoint` | 恢复或调试用 checkpoint。 |
| `errorMessage` | 任务级错误消息。 |
| `createdAt` | 任务创建时间。 |
| `updatedAt` | 任务最近更新时间。 |

### `download_task`

用途：保存离线下载任务状态和进度。

| 字段 | 含义 |
| --- | --- |
| `id` | 下载任务主键。 |
| `sourceId` | 来源 ID，通常对应源模块或账号命名空间。 |
| `mediaType` | 媒体类型，如 track。 |
| `remoteId` | 来源侧远程媒体 ID 或路径标识。 |
| `title` | 下载项标题。 |
| `artist` | 艺术家展示文本。 |
| `album` | 专辑展示文本。 |
| `durationMs` | 媒体时长，单位毫秒。 |
| `status` | 下载状态。 |
| `downloadedBytes` | 已下载字节数。 |
| `totalBytes` | 总字节数；未知时为空。 |
| `localPath` | 下载完成或部分下载的本地路径。 |
| `mimeType` | 下载内容 MIME 类型。 |
| `errorMessage` | 下载失败原因。 |
| `createdAt` | 任务创建时间。 |
| `updatedAt` | 任务最近更新时间。 |

### `playlist`

用途：保存用户创建的播放列表。

| 字段 | 含义 |
| --- | --- |
| `id` | 播放列表主键。 |
| `title` | 播放列表标题。 |
| `artworkId` | 播放列表封面 `artwork.id`。 |
| `coverStorageId` | 自定义封面来源账号 ID。 |
| `coverPath` | 自定义封面路径。 |
| `createdAt` | 播放列表创建时间。 |
| `updatedAt` | 播放列表最近更新时间。 |
| `sortOrder` | 播放列表排序值。 |

### `playlist_track`

用途：保存播放列表内的曲目成员和顺序。

| 字段 | 含义 |
| --- | --- |
| `playlistId` | 所属 `playlist.id`；联合主键的一部分。 |
| `trackId` | 所含 `track.id`；联合主键的一部分。 |
| `sortOrder` | 曲目在播放列表内的排序值。 |
| `addedAt` | 加入播放列表的时间。 |

### `track_fts`

用途：FTS4 全文搜索虚表，用于本地曲库搜索；内容来源是 `track` 表。

| 字段 | 含义 |
| --- | --- |
| `title` | 参与全文索引的曲目标题。 |
| `artist` | 参与全文索引的艺术家文本。 |
| `albumArtist` | 参与全文索引的专辑艺术家文本。 |
| `composer` | 参与全文索引的作曲者文本。 |

The live schema no longer contains `storage`, `selected_folder`, `remote_file`,
or `sync_cursor`. Those tables are read only by historical migration code.
`TrackEntity` also no longer has `remoteFileId`, `sourceStorageId`, or
`sourcePath`.

## Import Coordinator

`RemoteLibraryImportCoordinator.scanAndImportFolder` consumes Rust
`RemoteMusicScanSession` batches and writes Room transactions:

1. Ensure a `source_account` and `library_root`.
2. Create or update the `import_job`.
3. Stream each Rust directory batch directly into planning and persistence;
   the coordinator does not retain the complete discovered tree.
4. Load the lightweight live-source signature projection once and compare
   incoming `StorageEntry` values by stable provider item ID, then canonical
   path.
5. Skip unchanged source items using size plus ETag, falling back to modified
   time when the source has no ETag.
6. Read metadata only for changed items through Rust metadata APIs.
7. Upsert `source_item`, canonical `track`, normalized album/artist/genre
   relationships, and `track_source_ref`; update lyrics, artwork metadata, and
   raw tags only when the task's metadata options requested them. Skipped
   families are neither deleted nor overwritten.
8. Persist item-level failures to `source_error` with the current
   `importJobId`.
9. Remove matched IDs from an in-memory missing-candidate set without updating
   unchanged rows. Apply the remaining IDs only after a complete snapshot;
   cancellation and failure never run this step.
10. Advance a typed `source_sync_cursor` and final `import_job` state in the
    same transaction. WebDAV uses `webdav_sync_token` and
    `webdav_sync_capability`, so it cannot overwrite OneDrive's `delta` cursor.

Canonical track matching prefers MusicBrainz recording ID, then ISRC plus
duration, then strict title/artist/album/duration metadata. A track can have
multiple source refs across accounts/providers, while each source item points
to one canonical track.

## Import Job UI State

`import_job` is sufficient for the current scan-first import UI to show
hundreds of files with complete task-level status through `LibrarySyncTask`:

- `status` maps to queued/running/paused/completed/error/cancelled UI states.
- `scannedCount`, `importedCount`, `skippedCount`, and `failedCount` provide
  the main counters. `scannedCount` is the discovered total once folder
  enumeration completes.
- `checkpoint` exposes the last processed path for resumable/debug display.
- Scan mode, scan rules, concurrency, batch size, and policies are persisted as
  a task snapshot and reused by pause/resume/retry.
- `metadataRequestCount`, `metadataFetchedBytes`, `metadataElapsedMs`, and
  `artworkCachedBytes` expose the Rust metadata-read cost.
- `syncMode` distinguishes `WEBDAV_SYNC_TOKEN`, `PARALLEL_FULL_SCAN`, and
  `LEGACY_FULL_SCAN_FALLBACK`.
- `directoryConcurrency`, capability/directory/Room timings, request and entry
  counts, and unchanged/added/modified/renamed/deleted counters expose WebDAV
  scan cost without storing credentials, tokens, or full paths in logs.
- `errorMessage`, `createdAt`, and `updatedAt` support error and time display.
- `LibrarySyncTask` also derives `processedCount`, `pendingCount`,
  `successfulCount`, and `hasProgress` from those persisted counters so UI code
  does not need to duplicate counter math.
- `source_error.importJobId` provides the per-task failure detail list.

Schema version 14 adds the WebDAV scan metrics to `import_job` with defaults,
so old jobs deserialize as `LEGACY_FULL_SCAN_FALLBACK`, directory concurrency
4, and zero counters. Progress updates remain task-level and batch-level; the UI
can show complete task status, counters, and per-task failure details.

`import_job` is not sufficient by itself for a future background enrichment
workflow where metadata, artwork, lyrics, and raw tags continue after the
initial import result is visible. That workflow should add explicit phase and
enrichment fields, for example:

- `phase`: scanning, indexing, enriching metadata, finalizing.
- `totalCount`: known total after folder enumeration completes.
- `metadataScannedCount` / `metadataImportedCount` / `metadataFailedCount`:
  background enrichment progress.
- `phaseCheckpoint`: current phase-specific path or cursor.

Android background execution should run library sync through a dedicated
WorkManager worker with foreground notification for long WebDAV imports. Desktop
can keep a coroutine-backed task queue. iOS can expose the same task model but
must treat long-running background WebDAV imports as best-effort because the
system may suspend network work.

## Playback Resolution

Playback resolves through persisted source references, not provider fields on
`track`:

```text
TrackEntity
  -> TrackSourceRefEntity
  -> SourceItemEntity
  -> MusicSource.resolvePlayback(...)
  -> transient PlaybackResource
```

`PlaybackResourceResolver` orders available refs by downloaded/local/preferred
and audio quality hints. The returned URI, headers, cookies, and expiration
metadata remain transient and are released through the playback resolver.

## Visibility And Deletion

Ordinary library/search queries require an available `track_source_ref`.
When a source item disappears or an account is unavailable, the app marks the
source item/ref unavailable. It does not delete canonical tracks, metadata, or
user playlist data as part of source disappearance.

## Migrations

- `MIGRATION_1_2` adds standardized metadata columns to `track`.
- `MIGRATION_2_3` adds old nullable playback columns used by the previous Room
  schema line.
- `MIGRATION_3_4` adds `download_task`.
- `MIGRATION_4_5` adds playlist cover location columns.
- `MIGRATION_5_6` adds Room FTS4 support for local search.
- `MIGRATION_6_7` creates `source_account`, `library_root`, `source_item`,
  `source_item_property`, `track_source_ref`, `source_sync_cursor`, and
  `source_error`; rebuilds `track` and `import_job`; migrates old storage,
  selected-folder, remote-file, and sync-cursor data; then drops the old tables.
- `MIGRATION_11_12` adds the scan configuration snapshot and metadata request,
  byte, elapsed-time, and artwork-cache counters to `import_job`. Defaults keep
  historical jobs compatible with Full scanning and the existing scan rules.
