# Kotlin Compose UI 代码生成基线

> 本文档为后续基于设计稿生成 Kotlin Compose UI 代码的基线说明。
> 待设计方向确认后，按页批量生成 Compose 代码。

## 1. 技术栈建议

| 维度 | 选型 |
| --- | --- |
| UI 框架 | Jetpack Compose（Material 3 基础 + 自定义主题） |
| 多平台 | Kotlin Multiplatform + Compose Multiplatform（共享 UI） |
| 最低版本 | Android API 24 / iOS 15 / Desktop JVM 17 |
| 状态管理 | ViewModel + StateFlow + collectAsStateWithLifecycle |
| 导航 | Navigation Compose（移动端）/ Voyager（Desktop 可选） |
| 媒体 | Media3 ExoPlayer |
| 图片 | Coil 3（KMP） |
| 持久化 | DataStore（偏好设置）+ Room（本地音乐库） |

## 2. 目录结构建议

```
app/
├── shared/                          # KMP 共享模块
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/com/tidetunes/
│   │   │   │   ├── theme/           # 主题系统
│   │   │   │   │   ├── Color.kt     # 双主题色值
│   │   │   │   │   ├── Type.kt      # 字阶
│   │   │   │   │   ├── Shape.kt     # 圆角
│   │   │   │   │   ├── Motion.kt    # 动效
│   │   │   │   │   ├── Spacing.kt   # 间距
│   │   │   │   │   └── Theme.kt     # TideTunesTheme 入口
│   │   │   │   ├── components/      # 全局组件
│   │   │   │   │   ├── GradientPlayButton.kt
│   │   │   │   │   ├── MiniPlayerBar.kt
│   │   │   │   │   ├── TrackRow.kt
│   │   │   │   │   ├── PlaylistCard.kt
│   │   │   │   │   ├── FrostedSheet.kt
│   │   │   │   │   └── ...
│   │   │   │   ├── features/        # 按页面组织
│   │   │   │   │   ├── home/
│   │   │   │   │   ├── player/
│   │   │   │   │   ├── playlist/
│   │   │   │   │   ├── search/
│   │   │   │   │   └── ...
│   │   │   │   ├── design/          # 设计 Token 读取
│   │   │   │   │   └── Tokens.kt    # 从 tokens.json 反序列化
│   │   │   │   └── App.kt
│   │   ├── androidMain/             # Android 差异
│   │   ├── iosMain/                 # iOS 差异
│   │   └── desktopMain/             # Desktop 差异
```

## 3. Token 映射（tokens.json → Compose）

| Token | Compose 表达 |
| --- | --- |
| `color.brand.primary` | `val BrandPrimary = Color(0xFF2E7CF6)` |
| `color.brand.gradient` | `val BrandGradient = Brush.linearGradient(listOf(Color(0xFF2E7CF6), Color(0xFF6C5CE7)))` |
| `color.light.bg.base` | 在 `LightColorScheme` 中作为 `background` |
| `color.dark.bg.base` | 在 `DarkColorScheme` 中作为 `background` |
| `text.display` | `Typography(display = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp))` |
| `space.16` | `val Space16 = 16.dp`（建议封装为 `Spacing` 数据类） |
| `radius.lg` | `val RadiusLg = RoundedCornerShape(20.dp)` |
| `motion.playerExpand` | `tween(380, easing = FastOutSlowInEasing)` |

## 4. 组件命名约定

| 类型 | 命名规则 | 示例 |
| --- | --- | --- |
| 主题入口 | `TideTunesTheme` | `TideTunesTheme(darkTheme = true) { ... }` |
| ColorScheme | `LightColorScheme` / `DarkColorScheme` | — |
| 组件 | PascalCase，与设计稿组件同名 | `GradientPlayButton`、`MiniPlayerBar`、`TrackRow` |
| 页面 Composable | PascalCase + Screen 后缀 | `HomeScreen`、`PlayerScreen` |
| 状态 | `xxxUiState` | `PlayerUiState` |
| 事件 | `xxxEvent` / `onXxx` | `onPlayClick`、`onNextClick` |

## 5. 双主题实现要点

```kotlin
@Composable
fun TideTunesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = TideTunesTypography
    val shapes = TideTunesShapes
    val spacing = Spacing()  // 自定义 LocalSpacing

    CompositionLocalProvider(
        LocalSpacing provides spacing
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
```

## 6. 生成顺序建议

1. **基础设施**：Theme（Color/Type/Shape/Spacing/Motion）+ Tokens 读取
2. **全局组件**：GradientPlayButton、MiniPlayerBar、TrackRow、FrostedSheet、BottomTabBar
3. **P0 页面**：HomeScreen、PlayerScreen、PlaylistDetailScreen
4. **P1 页面**：SearchScreen、RankingScreen、MineScreen
5. **P2/P3 页面**：PlaylistSquareScreen、ArtistDetailScreen、LocalMusicScreen、SettingsScreen
6. **平台差异**：在 `androidMain` / `iosMain` / `desktopMain` 中处理导航、弹窗、手势差异

## 7. 待确认事项

- [ ] 是否采用 KMP 共享 UI，还是 Android 优先 + iOS 后续？
- [ ] 状态管理用 ViewModel + StateFlow 还是 MVI（Mavericks/Molecule）？
- [ ] 导航方案：Navigation Compose 还是 Voyager？
- [ ] 是否需要立即开始生成代码，还是先完善更多页面设计稿？
