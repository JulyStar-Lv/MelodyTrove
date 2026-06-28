# WebDAV metadata scan: first 50 files — 2026-06-25

Target: the first 50 supported audio files at the configured AList WebDAV root,
ordered by remote path. All selected files were FLAC. Credentials were supplied
at runtime and are not present in this report or source code.

## Current optimized result

| Metric | Result |
| --- | ---: |
| Files selected | 50 |
| Metadata files parsed | 50 |
| Full metadata success | 50 |
| Partial metadata success | 0 |
| Timeout | 0 |
| Range/network failure | 0 |
| Budget exceeded | 0 |
| Unsupported format | 0 |
| Parse failure | 0 |
| Metadata concurrency | 4 |
| Directory listing time | 0.865 s |
| Metadata scan time | 20.183 s |
| Total elapsed time | 21.049 s |
| Median file time | 1.384 s |
| P95 file time | 2.140 s |
| Range requests | 75 |
| Range bytes | 19,660,800 bytes (18.75 MiB) |
| ETag present | 50/50 |
| Last-Modified present | 50/50 |
| MIME type present | 50/50 |

All 50 files contained title, artist, album, non-zero duration, sample rate,
bit depth, channel count, overall bitrate, and audio bitrate.

Technical property distribution:

- sample rate: 44.1 kHz for 44 files; 48 kHz for 6 files;
- bit depth: 16-bit for 44 files; 24-bit for 6 files;
- channels: stereo for all 50 files;
- duration range: 85,400–342,000 ms.

Lofty's base metadata pass now disables embedded artwork parsing. Artwork
remains a separate on-demand concern because the normalized result does not
contain picture data. This removed the multi-megabyte cover-art outliers:

- `任贤齐 - 伤心太平洋.flac`: 13 requests/3,407,872 bytes previously; now
  2 requests/524,288 bytes;
- `刘欢 - 从头再来 (Live).flac`: 7 requests/1,835,008 bytes previously; now
  2 requests/524,288 bytes.

## Incremental result

The current report was supplied as the previous fingerprint manifest and the
same remote directory was listed again. Matching uses size plus ETag, falling
back to size plus Last-Modified when ETag is unavailable.

| Metric | Result |
| --- | ---: |
| Files selected | 50 |
| Files unchanged/skipped | 50 |
| Metadata files parsed | 0 |
| PROPFIND/listing time | 0.824 s |
| Total elapsed time | 0.824 s |
| Range requests | 0 |
| Range bytes | 0 |

This is the expected steady-state behavior for libraries above 1,000 files:
directory enumeration still occurs, but unchanged files do not enter Lofty or
the Range reader.

## Baseline and optimization

The original 64 KiB reader opened a new HTTP client for every Range request:

| Stage | Concurrency | Success | Timeout | Total time | P50 | P95 | Requests | Bytes |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Baseline, 64 KiB | 1 | 47 | 3 | 405.013 s | 5.953 s | 24.753 s | 266 | 17,432,576 |
| Shared client, 64 KiB | 1 | 49 | 1 | 329.388 s | 4.799 s | 11.584 s | 278 | 18,219,008 |
| Shared client + 256 KiB | 1 | 50 | 0 | 117.497 s | 1.378 s | 3.867 s | 96 | 25,165,824 |
| Skip embedded artwork | 1 | 50 | 0 | 95.076 s | 2.181 s | 2.619 s | 75 | 19,660,800 |
| Skip artwork + bounded workers | 4 | 50 | 0 | 20.183 s | 1.384 s | 2.140 s | 75 | 19,660,800 |

Compared with the original baseline, the current implementation:

- reduced metadata scan time by 95.0%;
- reduced HTTP request count by 71.8%;
- kept all 50 files at full metadata success;
- supports zero-Range incremental scans for unchanged files.

Compared with the previous 117.497-second result, bounded concurrency plus
skipping artwork reduced wall time by 82.8%, requests by 21.9%, and transferred
bytes by 21.9%.

Implemented changes:

- one reusable `reqwest::Client` and connection pool per WebDAV backend;
- 256 KiB default metadata blocks;
- Lofty base parsing with `read_cover_art(false)`;
- finite Range request timeout;
- one retry for timeout, connection failure, or HTTP 5xx;
- configurable bounded metadata concurrency, default 4;
- WebDAV ETag, Last-Modified, creation time, and MIME parsing;
- typed fingerprint fields exposed through UniFFI;
- batch metadata FFI with ordered per-file results and bounded concurrency;
- Room batch lookup, mark-seen, and transaction helpers for incremental scans;
- reusable scanner with per-file status, timeout, request, byte, timing, tag,
  fingerprint, and audio-property output.

Observed baseline cases:

- `任贤齐 - 伤心太平洋.flac`: timed out after 22 small requests; final scan
  succeeded in 16.201 s with 13 requests and 3,407,872 bytes.
- `刘欢 - 从头再来 (Live).flac`: timed out after 21 small requests; final scan
  succeeded in 8.086 s with 7 requests and 1,835,008 bytes.
- `信乐团 - 离歌.flac`: one transient request stalled in the baseline; final
  scan succeeded in 2.462 s.
- one repeated run saw `周杰伦 - 东风破.flac` stall before its first response;
  after bounded request timeout/retry, final validation succeeded in 1.044 s.

## Previous per-file sequential results

| # | File | Status | Time ms | Requests | KiB | Parsed title |
| ---: | --- | --- | ---: | ---: | ---: | --- |
| 1 | `五月天 - 倔强.flac` | success | 3679 | 3 | 768 | `倔强` |
| 2 | `任然 - 一步之遥.flac` | success | 2434 | 2 | 512 | `一步之遥` |
| 3 | `任然 - 凉城.flac` | success | 1209 | 1 | 256 | `凉城` |
| 4 | `任然 - 后继者.flac` | success | 1168 | 1 | 256 | `后继者` |
| 5 | `任然 - 唇语.flac` | success | 2556 | 2 | 512 | `唇语` |
| 6 | `任然 - 山外小楼夜听雨.flac` | success | 1095 | 1 | 256 | `山外小楼夜听雨` |
| 7 | `任然 - 念.flac` | success | 2277 | 2 | 512 | `念` |
| 8 | `任然 - 无人之岛.flac` | success | 1378 | 1 | 256 | `无人之岛` |
| 9 | `任然 - 疑心病.flac` | success | 1261 | 1 | 256 | `疑心病` |
| 10 | `任然 - 空空如也.flac` | success | 1366 | 1 | 256 | `空空如也` |
| 11 | `任然 - 花雨落.flac` | success | 1208 | 1 | 256 | `花雨落` |
| 12 | `任然 - 落海.flac` | success | 2520 | 2 | 512 | `落海` |
| 13 | `任然 - 雀跃.flac` | success | 2638 | 2 | 512 | `雀跃` |
| 14 | `任然 - 飞鸟和蝉.flac` | success | 2537 | 2 | 512 | `飞鸟和蝉` |
| 15 | `任贤齐 - 伤心太平洋.flac` | success | 16201 | 13 | 3328 | `伤心太平洋` |
| 16 | `信乐团 - 死了都要爱.flac` | success | 2520 | 2 | 512 | `死了都要爱` |
| 17 | `信乐团 - 离歌.flac` | success | 2462 | 2 | 512 | `离歌` |
| 18 | `兰雨 - 最后一次的温柔.flac` | success | 2697 | 2 | 512 | `最后一次的温柔` |
| 19 | `关淑怡 - 难得有情人.flac` | success | 3867 | 3 | 768 | `难得有情人` |
| 20 | `刘德华 - 一起走过的日子.flac` | success | 2476 | 2 | 512 | `一起走过的日子` |
| 21 | `刘德华 - 来生缘.flac` | success | 2295 | 2 | 512 | `来生缘` |
| 22 | `刘德华 - 爱你一万年.flac` | success | 3632 | 3 | 768 | `爱你一万年` |
| 23 | `刘德华 - 爱火烧不尽.flac` | success | 2501 | 2 | 512 | `爱火烧不尽` |
| 24 | `刘欢 - 从头再来 (Live).flac` | success | 8086 | 7 | 1792 | `从头再来 (Live)` |
| 25 | `刘若英 - 后来.flac` | success | 2446 | 2 | 512 | `后来` |
| 26 | `刘若英 - 当爱在靠近 (Live).flac` | success | 1134 | 1 | 256 | `当爱在靠近 (Live)` |
| 27 | `动力火车 - 当.flac` | success | 4761 | 4 | 1024 | `当` |
| 28 | `动力火车 - 那就这样吧.flac` | success | 2575 | 2 | 512 | `那就这样吧` |
| 29 | `南宫嘉骏、彭清 - 回忆总想哭 (DJ何鹏版).flac` | success | 2437 | 2 | 512 | `回忆总想哭 (DJ何鹏版)` |
| 30 | `叶蒨文 - 春风秋雨.flac` | success | 2407 | 2 | 512 | `春风秋雨` |
| 31 | `周传雄 - 青花.flac` | success | 2688 | 2 | 512 | `青花` |
| 32 | `周华健 - 有没有一首歌会让你想起我.flac` | success | 2480 | 2 | 512 | `有没有一首歌会让你想起我` |
| 33 | `周杰伦 - 一口气全念对.flac` | success | 1228 | 1 | 256 | `一口气全念对` |
| 34 | `周杰伦 - 一点点.flac` | success | 2280 | 2 | 512 | `一点点` |
| 35 | `周杰伦 - 一路向北.flac` | success | 1162 | 1 | 256 | `一路向北` |
| 36 | `周杰伦 - 七里香.flac` | success | 1221 | 1 | 256 | `七里香` |
| 37 | `周杰伦 - 三年二班.flac` | success | 1194 | 1 | 256 | `三年二班` |
| 38 | `周杰伦 - 上海一九四三.flac` | success | 1130 | 1 | 256 | `上海一九四三` |
| 39 | `周杰伦 - 不爱我就拉倒.flac` | success | 1185 | 1 | 256 | `不爱我就拉倒` |
| 40 | `周杰伦 - 不能说的秘密.flac` | success | 1137 | 1 | 256 | `不能说的秘密` |
| 41 | `周杰伦 - 与父共舞.flac` | success | 1136 | 1 | 256 | `与父共舞` |
| 42 | `周杰伦 - 东风破.flac` | success | 1044 | 1 | 256 | `东风破` |
| 43 | `周杰伦 - 乌克丽丽.flac` | success | 1296 | 1 | 256 | `乌克丽丽` |
| 44 | `周杰伦 - 乔克叔叔.flac` | success | 1160 | 1 | 256 | `乔克叔叔` |
| 45 | `周杰伦 - 乱舞春秋.flac` | success | 1319 | 1 | 256 | `乱舞春秋` |
| 46 | `周杰伦 - 以父之名.flac` | success | 1318 | 1 | 256 | `以父之名` |
| 47 | `周杰伦 - 伊斯坦堡.flac` | success | 1248 | 1 | 256 | `伊斯坦堡` |
| 48 | `周杰伦 - 你听得到.flac` | success | 1193 | 1 | 256 | `你听得到` |
| 49 | `周杰伦 - 倒影.flac` | success | 1158 | 1 | 256 | `倒影` |
| 50 | `周杰伦 - 借口.flac` | success | 1070 | 1 | 256 | `借口` |

## Reproduction

Run `webdav_metadata_scan` with credentials supplied through environment
variables. Optional controls include:

- `TIDETUNES_SCAN_START`;
- `TIDETUNES_SCAN_LIMIT`;
- `TIDETUNES_SCAN_FILE_TIMEOUT_SECS`;
- `TIDETUNES_SCAN_BLOCK_SIZE`;
- `TIDETUNES_SCAN_CONCURRENCY`;
- `TIDETUNES_SCAN_PREVIOUS`;
- `TIDETUNES_SCAN_OUTPUT`.

The scanner emits detailed JSON including normalized tags, technical audio
properties, per-file timings, request counts, byte counts, and classified
errors.
