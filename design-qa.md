# 专辑 / 歌单 UI 视觉验收

## 验收范围

- 参考实现：`Design/src/app/App.tsx` 中的 `PlaylistDetailPage` 及专辑复用状态。
- 目标实现：Compose Multiplatform 的专辑详情、歌单详情和歌单编辑态。
- 参考截图：`build/ui-qa/design-album-mobile.png`、`build/ui-qa/design-playlist-mobile.png`。
- 实现截图：`build/ui-qa/album-playlist-android/album-final-playing.png`、`build/ui-qa/album-playlist-android/playlist-final.png`、`build/ui-qa/album-playlist-android/playlist-editing-final.png`。
- 并排对照：`build/ui-qa/album-playlist-android/compare-album-final.jpg`、`build/ui-qa/album-playlist-android/compare-playlist-final.jpg`（左侧 Design，右侧 Android）。
- 验收设备：Pixel 7 Pro / Android 14，1440 × 3120；对照时缩放到与参考稿相同的 393 × 852。

## 视觉与交互检查

- [x] 顶部返回、更多菜单、封面、标题、元信息、播放全部和定位操作与参考层级一致。
- [x] 曲目列表采用无卡片紧凑行布局，包含序号、歌曲名、歌手与专辑、收藏和更多操作。
- [x] 专辑元信息显示歌手、年份、流派、曲目数及总时长。
- [x] 歌单详情提供播放、定位、编辑、导入、移除歌曲等操作。
- [x] 歌单编辑态提供全选、单曲复选、拖动排序和完成操作。
- [x] 收藏切换、播放全部、当前曲目定位及菜单入口已在模拟器中交互验证。
- [x] 空态、加载态和错误态沿用现有组件与本地化资源。
- [x] 可操作图标具有内容描述；曲名相关操作包含上下文文本。
- [x] 最终运行日志未发现应用崩溃或 Compose 异常。

## 差异处理

- 首轮并排检查发现活动曲目背景色与参考稿不一致，并发现专辑歌手回退字段错误；两处均已修正后重新构建与截图。
- Android 系统状态栏、系统字体字重与 iOS 参考图存在平台固有差异；内容结构、间距、色彩层级和操作布局保持一致。
- 底部导航和播放器继续复用应用既有设计系统，未改变全局导航行为。

## 工程验证

- `./gradlew :feature:album:compileKotlinDesktop :feature:playlist:compileKotlinDesktop :shared:compileKotlinDesktop :feature:album:desktopTest :feature:playlist:desktopTest :shared:desktopTest --stacktrace`
- `./gradlew :androidApp:assembleDebug --stacktrace`
- `git diff --check`

Final result: passed
