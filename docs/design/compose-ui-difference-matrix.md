# TideTunes Compose UI 差异矩阵

审计日期：2026-07-24。设计证据按优先级来自 `Design/src/app/App.tsx`、
`Design/src/imports/pasted_text/tidetunes-design-system-v3.md` 与
`docs/design/design-ui-implementation.md`；生产代码仅作为当前实现证据。

| 设计元素 | 现有 Compose 文件 | 已确认差异 | 优先级 | 验收方式 |
| --- | --- | --- | --- | --- |
| 四个根入口与播放器入口 | `shared/.../navigation/HomeTab.kt`、`HomePage.kt`、`RootNavHost.kt` | 已修复：仅 Home、Search、Library、Settings 为根入口；移除了播放后自动跳转、Library 和播放列表的直达路径。唯一的 `MusicGraph.NowPlaying` 导航由 `PlaybackMiniPlayerHost` 的点击回调提供。 | 已完成 | 静态导航审查；四个入口切换和 Mini Player 打开播放器。 |
| 紧凑底部导航 | `widgets/appbar/BottomBar.kt`、`core/.../PlayerChromeComponents.kt` | 已修复：62dp 高度、1dp 分隔线、48×28dp 指示器、20dp 图标和 10sp 标签均由 `TideTunesTokens.navigation` 提供。 | 已完成 | 390×844 下测量 62dp 内容高度、1dp 分隔线、四个 48dp+ 点击面。 |
| 自适应应用壳与 Mini Player | `navigation/HomePage.kt`、`RootNavHost.kt`、`widgets/appbar/NavigationRailBar.kt`、`SidebarBar.kt` | 已修复：根壳保持 <600 / 600–1279 / ≥1280；桌面主内容底部保留 Mini Player。设计源码确认二级页也必须持续显示 Mini Player，因此 `RootNavHost` 为非 Home、非 Now Playing 的二级路由保留播放器空间并承载同一 `PlaybackMiniPlayerHost`；侧栏主题行及根入口行提升至 48dp。 | 已完成 | Home 与专辑、艺术家、播放列表、下载、导入、队列、歌词、音源管理、设置子页均显示 Mini Player；Now Playing 本身不重复显示；390、840–1279、1280+；浅/深主题。 |
| 共享 tokens 与页面收起栏 | `theme/Theme.kt`、`components/TideGlassComponents.kt`、`TidePageHeader.kt` | 已修复：导航和 Mini Player 关键尺寸归入 tokens；粘性标题栏为 58dp，Home 收起距离为 48dp。 | 已完成 | Home 上滚 48dp 后出现 58dp 标题栏；中英文长标题截断；浅/深主题。 |
| 可点击共享卡片 | `components/TideCardSurface.kt`、`TideSettingsComponents.kt` | 已修复：`TideCardSurface(onClick)` 在不受固定视觉尺寸限制时保证 48dp 最小高度；设置行已为 68dp。 | 已完成 | 对所有经共享卡片承载的操作，在不受设计固定尺寸限制时测量 ≥48dp。 |
| Home 真实数据与播放 | `feature/home/.../HomeState.kt`、`HomeViewModel.kt`、`HomeDesignScreen.kt`、`HomeRoot.kt` | 已修复为安全子集：默认状态为空，Home 组合 `LibraryRepository` 的曲目/专辑/艺术家及 `PlaylistRepository` 摘要；点击真实曲目建立真实播放队列，不再使用设计稿示例封面或曲目。 | 已完成 | 无库时只显示可操作空状态；有库时只显示 Repository 数据，点击曲目由 `PlaybackController` 开始播放。 |
| Home 的置顶、历史、听歌统计 | 同上；`core/domain/.../LibraryRepository.kt`、`PlaylistRepository.kt` | 当前领域契约只提供曲目、专辑、艺术家与播放列表摘要；没有置顶、播放历史、听歌统计的真实数据。按要求不能用设计稿演示数据补齐。 | 阻塞该子项 | 需要产品决定：新增持久化业务契约后显示这些区块，或在生产 Home 中隐藏/以明确空状态替代。 |
| Search | `feature/search/.../SearchDesignScreen.kt`、`SearchViewModel.kt`、`SearchRoot.kt` | 已修复：生产 `SearchDesignScreen` 曾展示固定专辑、艺术家、趋势和默认历史。现仅展示真实 `tracks`、`history`、`suggestions`；点击真实结果直接交给 `PlaybackController`，不直达 Now Playing。 | 已完成，有范围限制 | 输入、清空、真实历史/建议、空/加载/错误/结果；点击结果后出现 Mini Player；中英文长查询。 |
| Search 的专辑/艺术家结果 | 同上；`feature/search/domain/SearchTrackItem.kt` | 设计稿含专辑/艺术家筛选，但现有搜索领域契约只提供曲目结果。为避免伪造结果，本轮隐藏了这些筛选与卡片。 | 阻塞该子项 | 需要新增真实专辑/艺术家搜索结果及导航契约后再实现。 |
| Library | `feature/library/.../LibraryDesignScreen.kt`、`LibraryVM.kt`、`LibraryRoot.kt` | 已修复：Library 内部桌面侧栏阈值从 1024dp 对齐为 1280dp，840–1279dp 仅保留根导航栏；真实专辑、艺术家与播放列表摘要可进入既有二级页；播放后保留在页面，由 Mini Player 进入 Now Playing。固定文件夹、流派、播放列表以及任意曲库切片伪装的收藏/历史/最近项已移除。 | 已完成，有范围限制 | 840–1279 仅根导航栏；≥1280 可显示桌面 Library 侧栏；空库导入入口可达；真实专辑、艺术家、播放列表可导航。 |
| Library 的流派、文件夹、收藏、历史、最近、无损/Hi-Res、下载集合 | 同上；`core/domain/.../LibraryRepository.kt`、下载领域状态 | 这些设计集合缺少对应的真实领域状态。文件夹显示真实导入入口；其余不再以 `tracks.take(...)` 等方式伪造集合，改为明确空状态。 | 阻塞该子项 | 为每个集合提供真实 Repository 状态、排序/筛选语义与导航后再展示内容。 |
| Settings 与设置子页 | `feature/settings/.../SettingsScreen.kt`、`PlaybackSettingsScreen.kt`、`SettingsRows.kt` | 分组卡片、状态、禁用态与真实设置状态已存在。旧“播放时自动打开播放器”与 Mini Player-only 规则冲突，持久化字段保持兼容，设置页改为不可交互的 Mini Player 入口说明。 | 已完成，有取舍 | 搜索、开关、禁用、长中英文、所有子页返回。 |
| 专辑、艺术家、播放列表、音源、导入、下载、队列、歌词、正在播放 | 对应 `feature/*/presentation` 与 `service/playback/presentation` | 已补上 Library → 真实专辑/艺术家/播放列表详情的二级路由；播放列表列表及创建、编辑、导入继续复用既有 ViewModel/Repository。播放列表、专辑与艺术家的播放后直达路径均已移除。其余页面路由、真实状态和共享卡片/标题沿用既有实现；静态审计没有确认额外的安全视觉差异。 | 已完成；运行时验收待设备 | 按页面的空、加载、错误、播放、选中、长文本状态做运行时视觉验收；共享改动会覆盖其表面与标题栏。 |

## 实施约束与取舍

- 不修改 `Design/`：当前工作树中该目录已有用户的未提交变更，审计只读取它。
- 不修改音乐库、音源、下载、播放或插件的领域契约。
- Home 的置顶、历史和统计缺少真实来源；在未获得新增业务契约授权前，不实现这些设计区块的“有数据”版本，也不保留任何演示音乐数据。
- Search 的专辑/艺术家结果以及 Library 的流派、文件夹列表、收藏、历史、最近、无损/Hi-Res、下载集合也缺少真实来源；本轮保留可用导入/管理入口，并用明确空状态替代设计稿样例。若产品要展示它们，需要先扩展领域契约，而不是在 UI 层推断或切片伪造。
- 旧 `openPlayerOnPlay` 设置与“只能由 Mini Player 进入”冲突。本轮以设计入口规则优先：不修改其序列化字段、Repository 或历史值，但不再读取该值导航，并以禁用说明项替代可切换开关。若要恢复该偏好，需要放宽 Mini Player-only 规则。
- 本机没有 Android 设备或模拟器；桌面窗口最小宽度为 840dp。因此 390×844 与 600–839dp 的截图/触摸测量未伪造为已完成，保留为设备视觉验收项。

## 当前验证记录

- 通过：`./gradlew :core:presentation:desktopTest :feature:home:desktopTest :feature:library:desktopTest :feature:playlist:desktopTest :feature:search:desktopTest :feature:settings:desktopTest :feature:album:desktopTest :feature:artist:desktopTest :shared:desktopTest :desktopApp:compileKotlinDesktop`。
- 通过：`git diff --check`。
- 未完成运行时视觉检查：`./gradlew :desktopApp:run` 需要 Java 21，本机仅检测到较低版本 JDK，且 Gradle 未配置自动下载工具链；同时没有 Android 设备或模拟器。因而 390×844、840–1279dp、1280dp+ 及浅/深主题下的截图、触控尺寸和 TalkBack/VoiceOver 检查仍待具备 Java 21 或移动设备的环境执行。
