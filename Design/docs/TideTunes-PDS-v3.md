# TideTunes Design System v3

> **Apple Music Information Architecture × HyperOS Design Language × Compose Multiplatform Responsive Design System**
>
> Version 3.0 · 2026-07-05 (v4 多设备响应式：2026-07-06 · v5 Figma Make 对齐：2026-07-06)
> Tagline: **One Library. Every Source.**

---

## 文件结构

```
00 Cover          — Brand / Vision / Design Principles / Product Philosophy
01 Foundation     — Color / Typography / Grid / Elevation / Blur / Radius / Motion / Iconography / Haptics
02 Design Tokens  — Color / Space / Radius / Typography / Motion / Blur / Shadow / Icon Tokens
03 Components     — Buttons / Navigation / Cards / Music Cards / Source Cards / Mini Player / Queue / Lyrics / Dialog / Bottom Sheet / Settings / Search / Slider / Tabs / FAB
04 Adaptive Layout— Android Phone / Fold / Tablet / Auto / iPhone / iPad / Desktop / TV (Future)
05 Pages          — Home / Search / Library / Settings / Source Manager / Now Playing / Lyrics / Queue / Album / Artist / Playlist
06 Prototype      — Interaction flows
07 Motion         — Mini → Expanded → Immersive / Shared Element / Blur Morph
08 Dev Mode       — Component specs / Measurements / Asset export
09 Compose Mapping— Figma → Compose 1:1 mapping
```

---

## 00 Cover

### 00.1 Brand

| 属性 | 值 |
|---|---|
| Product Name | TideTunes |
| Chinese Name | 潮音 |
| Tagline | One Library. Every Source. |
| Positioning | 跨平台音乐聚合播放器 — 统一管理 WebDAV / SMB / OneDrive / Plex / Emby / Jellyfin / Navidrome / Local 等多来源音乐 |

### 00.2 Vision

> 让用户的音乐库不再被来源分割。无论音乐存储在哪里，TideTunes 都能将其聚合为一个统一的、沉浸式的、跨平台的聆听体验。

### 00.3 Design Principles

| 原则 | 释义 | 设计表现 |
|---|---|---|
| **Simple** | 简单到不需要学习 | 四 Tab 导航，无隐藏入口 |
| **Calm** | 克制的视觉噪音 | 大留白，弱分割线，低饱和控件 |
| **Immersive** | 沉浸于音乐本身 | 动态模糊背景，全屏播放体验 |
| **Music First** | 音乐永远是主角 | 封面优先，控件让位 |
| **Content First** | 内容驱动布局 | 自适应网格，内容密度跟随屏幕 |
| **Adaptive** | 一套设计，自然适配所有形态 | Compact → XL 断点系统 |
| **Native** | 尊重每个平台的原生习惯 | iOS 用 HIG 手势，Android 用 M3 组件 |
| **Cross Platform** | 跨端一致但非雷同 | Compose Multiplatform 共享逻辑，UI 各端原生 |
| **Plugin Driven** | Source 即插件，可插拔 | 统一 SourceAdapter 架构 |

### 00.4 Product Philosophy

TideTunes 不是一个流媒体订阅服务，而是一个**个人音乐聚合器**。它不与 Spotify / Apple Music 竞争，而是服务于拥有自建音乐库（NAS / 云盘 / 本地）的用户。核心价值主张：

1. **聚合** — 一个界面管理所有来源的音乐
2. **统一** — 统一的播放队列、歌词、收藏、播放历史
3. **沉浸** — Apple Music 级别的播放体验
4. **开放** — 插件化 Source 架构，社区可扩展

---

## 01 Foundation

### 01.1 Color System

#### 01.1.1 Brand Colors (v3 全新)

| Token | 名称 | 色值 | 用途 |
|---|---|---|---|
| `color.brand.primary` | 潮汐粉 | `#FF5B8A` | 主品牌色、播放键、选中态 |
| `color.brand.secondary` | 极光紫 | `#7A6CFF` | 渐变终点、强调点缀 |
| `color.brand.gradient` | 品牌渐变 | `linear-gradient(90°, #FF5B8A → #7A6CFF)` | 播放键、进度条、沉浸背景 |
| `color.brand.gradientSoft` | 柔和渐变 | `linear-gradient(90°, #FF5B8A → #7A6CFF)` @ 40% | 卡片悬浮、徽章 |
| `color.support.blue` | 辅助蓝 | `#4D99FF` | 信息提示、链接 |
| `color.support.orange` | 辅助橙 | `#FF994D` | 警告、Hi-Res 标识 |
| `color.support.green` | 辅助绿 | `#33CC99` | 成功、下载完成 |
| `color.support.yellow` | 辅助黄 | `#FFD94D` | 高亮、暖色状态 |

> v5 支持色来自 `Design/.workbuddy/screenshots/5_853.png` 的 Section 1 色块像素采样；TidePink / TidePurple 仍以 Figma Make 元数据中的 `#FF5B8A` / `#7A6CFF` 为准。

#### 01.1.2 Light Theme

| Token | 名称 | 色值 | 用途 |
|---|---|---|---|
| `color.light.bg.base` | 页面底色 | `#FFFFFF` | 主背景 |
| `color.light.bg.subtle` | 微抬升 | `#FAFAFC` | 区块底色 |
| `color.light.bg.elevated` | 卡片底 | `#F7F8FA` | 卡片、设置组 |
| `color.light.bg.sunken` | 凹陷 | `#F2F3F6` | 输入框、搜索栏 |
| `color.light.surface.primary` | 主表面 | `#FFFFFF` | 卡片表面 |
| `color.light.surface.frosted` | 毛玻璃 | `#FFFFFF` @ 90% | 弹窗、顶栏磨砂 |
| `color.light.text.primary` | 主文字 | `#1A1A1F` | 标题、正文 |
| `color.light.text.secondary` | 次文字 | `#6B6B75` | 副标题 |
| `color.light.text.tertiary` | 弱文字 | `#9A9AA3` | 计数、时间 |
| `color.light.text.inverse` | 反白 | `#FFFFFF` | 渐变上的文字 |
| `color.light.divider` | 分割线 | `#EFEFF2` | 列表分隔 |
| `color.light.outline.default` | 边线 | `#E8E8ED` | 卡片边、输入框边 |

#### 01.1.3 Dark Theme

| Token | 名称 | 色值 | 用途 |
|---|---|---|---|
| `color.dark.bg.base` | 页面底色 | `#0B0B0F` | 主背景（非纯黑，保留深度感） |
| `color.dark.bg.subtle` | 微抬升 | `#111118` | 区块底色 |
| `color.dark.bg.elevated` | 卡片底 | `#16161D` | 卡片、设置组 |
| `color.dark.bg.sunken` | 凹陷 | `#08080C` | 输入框、搜索栏 |
| `color.dark.surface.primary` | 主表面 | `#16161D` | 卡片表面 |
| `color.dark.surface.frosted` | 毛玻璃 | `#16161D` @ 85% | 弹窗、顶栏磨砂 |
| `color.dark.text.primary` | 主文字 | `#F5F5F7` | 标题、正文 |
| `color.dark.text.secondary` | 次文字 | `#9A9AA3` | 副标题 |
| `color.dark.text.tertiary` | 弱文字 | `#6B6B75` | 计数、时间 |
| `color.dark.text.inverse` | 反白 | `#FFFFFF` | 渐变上的文字 |
| `color.dark.divider` | 分割线 | `#1F1F28` | 列表分隔 |
| `color.dark.outline.default` | 边线 | `#2A2A35` | 卡片边、输入框边 |

#### 01.1.4 Dynamic Color

播放页与 Now Playing 背景从**当前播放专辑封面**提取主色调，生成动态渐变：

```
Album Artwork
    ↓ Palette extraction
Primary Color → 60% opacity gradient
Secondary Color → 30% opacity gradient
    ↓
Background = base + dynamic gradient overlay
```

### 01.2 Typography

#### 01.2.1 Font Family

| 平台 | 主字体 | 回退 |
|---|---|---|
| Android | Plus Jakarta Sans | MiSans / Roboto / Noto Sans SC |
| iOS | Plus Jakarta Sans | SF Pro Display / SF Pro Text / PingFang SC |
| Desktop (macOS) | Plus Jakarta Sans | SF Pro / PingFang SC |
| Desktop (Windows) | Plus Jakarta Sans | Segoe UI / Microsoft YaHei |
| Badge / Data | JetBrains Mono | SF Mono / Roboto Mono / monospace |

> v5 对齐 Figma Make：App UI 通过 `core:presentation` 的 Compose Resources 打包 Plus Jakarta Sans / JetBrains Mono variable TTF；平台字体仍作为 fallback。字体 license 随资源保留在 `composeResources/files/fonts/`。

#### 01.2.2 Type Scale

| Token | 字号 | 字重 | 行高 | 用途 |
|---|---|---|---|---|
| `text.display` | 34sp | 700 | 40 | Now Playing 歌曲名 |
| `text.headline` | 28sp | 700 | 34 | 页面大标题 |
| `text.title1` | 22sp | 600 | 28 | 区块标题 |
| `text.title2` | 18sp | 600 | 24 | 卡片标题 |
| `text.title3` | 16sp | 500 | 22 | 列表标题、Tab |
| `text.body` | 14sp | 400 | 20 | 正文、曲目名 |
| `text.callout` | 13sp | 400 | 18 | 辅助正文 |
| `text.caption` | 12sp | 400 | 16 | 副信息 |
| `text.micro` | 10sp | 600 | 14 | 徽章、Tag（全大写） |

### 01.3 Grid & Spacing

#### 01.3.1 Spacing System (4pt base)

| Token | 值 | 用途 |
|---|---|---|
| `space.4` | 4dp | 图标与文字最小间距 |
| `space.8` | 8dp | 卡片内边距、行内间距 |
| `space.12` | 12dp | 卡片间距、列表行内距 |
| `space.16` | 16dp | 页面边距、区块间距 |
| `space.20` | 20dp | 大卡片内边距 |
| `space.24` | 24dp | 区块大间距 |
| `space.32` | 32dp | 段落间距 |
| `space.40` | 40dp | 顶部留白 |
| `space.48` | 48dp | 大段落间距 |

#### 01.3.2 Grid

| 设备 | 列数 | 边距 | 间距 |
|---|---|---|---|
| Compact (Phone) | 2-4 | 16dp | 12dp |
| Medium (Fold) | 4-6 | 20dp | 16dp |
| Expanded (Tablet) | 6-8 | 24dp | 16dp |
| Large (Desktop) | 8-12 | 32dp | 20dp |
| XL (Wide Desktop) | 12-16 | 40dp | 24dp |

### 01.4 Radius

| Token | 值 | 用途 |
|---|---|---|
| `radius.sm` | 12dp | 小按钮、Chip、Tag |
| `radius.md` | 18dp | 列表行、输入框 |
| `radius.lg` | 24dp | 普通卡片 |
| `radius.xl` | 28dp | 大卡片、歌单封面 |
| `radius.2xl` | 36dp | 弹窗、Bottom Sheet |
| `radius.full` | 999dp | 圆形按钮、头像、播放键 |

> v3 圆角整体比 v1 更大，对齐 HyperOS / MIUI 的"大圆角"视觉语言。

### 01.5 Elevation

| Token | Light | Dark | 用途 |
|---|---|---|---|
| `elevation.surface` | 无阴影 + 1px 边 | 无阴影 + 1px 边 | 普通卡片 |
| `elevation.card` | `0 1px 3px rgba(0,0,0,0.04)` | 1px 边 `#2A2A35` | 悬浮卡片 |
| `elevation.popup` | `0 4px 16px rgba(0,0,0,0.08)` | 1px 边 + 内发光 | 弹窗、Sheet |
| `elevation.floating` | `0 8px 24px rgba(0,0,0,0.10)` | 1px 边 + 内发光 | Mini Player、FAB |
| `elevation.overlay` | `0 16px 48px rgba(0,0,0,0.12)` | 内发光 `rgba(122,108,255,0.15)` | 全屏播放页 |

### 01.6 Blur

| Token | 值 | 用途 |
|---|---|---|
| `blur.none` | 0dp | 无模糊 |
| `blur.light` | 8dp | 顶栏磨砂 |
| `blur.medium` | 16dp | Bottom Sheet 磨砂 |
| `blur.heavy` | 32dp | Now Playing 背景模糊 |
| `blur.ultra` | 48dp | 动态封面背景模糊 |

### 01.7 Motion

| Token | 时长 | 缓动 | 用途 |
|---|---|---|---|
| `motion.instant` | 100ms | `ease-out` | 按钮按压反馈 |
| `motion.fast` | 180ms | `ease-out` | Tab 切换、卡片点击 |
| `motion.normal` | 280ms | `cubic-bezier(0.32, 0.72, 0, 1)` | 弹窗进入 |
| `motion.expand` | 380ms | `cubic-bezier(0.32, 0.72, 0, 1)` | Mini Player → Now Playing |
| `motion.morph` | 500ms | `cubic-bezier(0.32, 0.72, 0, 1)` | 背景模糊变形 |
| `motion.theme` | 240ms | `ease-in-out` | 主题切换 |

#### Motion Choreography

```
Mini Player (56dp)
    ↓ tap (380ms, shared element)
Expanded Player (full screen)
    ↓ artwork zoom + blur morph
Immersive Player (artwork fullscreen, lyrics overlay)
```

- **Artwork**: Shared Element Transition，从 Mini Player 56dp 平滑放大到全屏
- **Background**: 从透明 → Blur Morph → 动态渐变
- **Lyrics**: Fade in from bottom，逐行高亮
- **Controls**: Scale + Fade

### 01.8 Iconography

| 属性 | 值 |
|---|---|
| 网格 | 24×24dp |
| 风格 | 线性（默认）/ 填充（选中态） |
| 描边 | 2px |
| 线帽 | Round cap / Round join |
| 命名 | `ic-{name}-{line\|fill}` |
| 尺寸 | 20dp (Tab) / 24dp (导航) / 28dp (播放页) / 32dp (播放键) |

### 01.9 Haptics

| 场景 | iOS | Android |
|---|---|---|
| 播放/暂停 | Light Impact | `HapticFeedbackConstants.VIRTUAL_KEY` |
| 收藏 | Medium Impact | `CONFIRM` |
| 下一首 | Light Impact | `VIRTUAL_KEY` |
| 长按菜单 | Medium Impact | `LONG_PRESS` |
| 拖动进度条 | Selection Feedback | `CLOCK_TICK` |
| Mini Player 展开 | Medium Impact | `GESTURE_END` |

---

## 02 Design Tokens

### 02.1 Token Categories

```
┌─────────────────────────────────────────────────┐
│                  Design Tokens                    │
├──────────┬──────────┬──────────┬────────────────┤
│  Color   │  Space   │  Radius  │  Typography    │
├──────────┼──────────┼──────────┼────────────────┤
│  Motion  │  Blur    │  Shadow  │  Icon          │
├──────────┼──────────┼──────────┼────────────────┤
│  Touch Target │  Adaptive Width │  Elevation   │
└──────────┴──────────┴──────────┴────────────────┘
```

### 02.2 Token Naming Convention

```
{category}.{subCategory}.{property}.{variant}

color.brand.primary
color.light.bg.base
space.16
radius.lg
text.title1
motion.expand
blur.heavy
```

### 02.3 Gradient Pair Tokens

> 来源：`Design/.workbuddy/screenshots/5_853.png` 中 "8 Gradient Pairs" 区域像素采样。卡片方向为横向左 → 右，角度记为 `90°`；第 8 张卡片视觉上是 Blue → Green → Purple 三色 blend，按截图保留三段 stop。

| Token | 角度 | Stops | 用途 |
|---|---:|---|---|
| `color.gradientPairs.purplePink` | 90° | `#7A6CFF → #FF5B8A` | 品牌反向高光 |
| `color.gradientPairs.pinkPurple` | 90° | `#FF5B8A → #7A6CFF` | 品牌主渐变、播放主控 |
| `color.gradientPairs.pinkOrange` | 90° | `#FF5B8A → #FF994D` | 暖色音乐卡、Hi-Res 场景 |
| `color.gradientPairs.orangeYellow` | 90° | `#FF994D → #FFD94D` | 活力/警告弱化态 |
| `color.gradientPairs.greenBlue` | 90° | `#33CC99 → #4D99FF` | 下载完成、同步成功 |
| `color.gradientPairs.bluePurple` | 90° | `#4D99FF → #7A6CFF` | 信息态、搜索/发现 |
| `color.gradientPairs.pinkYellow` | 90° | `#FF5B8A → #FFD94D` | 推荐、热度标签 |
| `color.gradientPairs.blueGreenPurple` | 90° | `#4D99FF → #33CC99 → #7A6CFF` | 沉浸背景、氛围卡 |

### 02.4 Adaptive Width Tokens

| Token | Compact | Medium | Expanded | Large | XL |
|---|---|---|---|---|---|
| `width.content` | fill - 32dp | fill - 40dp | 600dp | 800dp | 1000dp |
| `width.sidebar` | 0 (hidden) | 0 (hidden) | 240dp | 260dp | 280dp |
| `width.detailPanel` | 0 (full screen) | 0 (full screen) | 360dp | 400dp | 480dp |
| `width.miniPlayer` | fill - 32dp | fill - 40dp | fill - 280dp | 480dp | 560dp |
| `width.card.min` | 140dp | 140dp | 160dp | 180dp | 200dp |
| `width.card.max` | 180dp | 200dp | 220dp | 240dp | 280dp |

### 02.5 Touch Target

| Token | 值 | 用途 |
|---|---|---|
| `touch.minimum` | 44dp | iOS HIG 最小触摸区域 |
| `touch.comfortable` | 48dp | Android M3 推荐触摸区域 |
| `touch.large` | 56dp | Android Auto / 车载场景 |
| `touch.xl` | 64dp | Android Auto 主按钮 |

---

## 03 Components

### 03.1 Component Map

```
┌──────────────────────────────────────────────────────────┐
│                    Components                              │
├───────────┬───────────┬───────────┬───────────────────────┤
│  Buttons  │ Navigation│  Cards    │  Music Components     │
├───────────┼───────────┼───────────┼───────────────────────┤
│ Primary   │ Bottom Nav│ Album Card│ Mini Player           │
│ Secondary │ Nav Rail  │ Artist Card│ Now Playing          │
│ Icon      │ Sidebar   │ Playlist  │ Queue                 │
│ FAB       │ Top Bar   │ Music Card│ Lyrics Panel          │
│ Chip      │ Search Bar│ Source Card│ Progress Bar         │
├───────────┼───────────┼───────────┼───────────────────────┤
│  Input    │  Feedback │  Settings │  Overlay              │
├───────────┼───────────┼───────────┼───────────────────────┤
│ TextField │ Snackbar  │ Group Card│ Bottom Sheet          │
│ Switch    │ Toast     │ Preference│ Dialog                │
│ Slider    │ Tooltip   │ Switch    │ Context Menu          │
│ Tab       │ Banner    │ Slider    │ Full-screen Player    │
└───────────┴───────────┴───────────┴───────────────────────┘
```

### 03.2 Buttons

#### Primary Button (Gradient Play Button)

| 属性 | 值 |
|---|---|
| Shape | Circle (radius.full) |
| Size | 56dp (standard) / 64dp (large) |
| Background | `linear-gradient(90°, #FF5B8A → #7A6CFF)` |
| Icon | 32dp, white, `ic-play-fill` / `ic-pause-fill` |
| Shadow (Light) | `0 12px 32px rgba(255,91,138,0.25)` |
| Glow (Dark) | Inner glow `0 0 24px rgba(122,108,255,0.30)` |
| Press | Scale 0.92 → 1.0, 100ms ease-out |
| States | Playing / Paused / Loading |

#### Secondary Button

| 属性 | 值 |
|---|---|
| Shape | Capsule (radius.full) |
| Height | 48dp (mobile) / 36dp (desktop) |
| Background | `color.brand.primary` solid |
| Text | 14sp 600, white |
| Padding | 24dp horizontal |

#### Icon Button

| 属性 | 值 |
|---|---|
| Shape | Circle |
| Size | 44dp (touch target) |
| Icon | 24dp |
| Background | transparent → `bg.sunken` (hover) |

#### FAB (Floating Action Button)

| 属性 | 值 |
|---|---|
| Shape | Circle / Squircle (radius.2xl) |
| Size | 56dp |
| Position | Bottom-right, above Mini Player |
| Shadow | `elevation.floating` |

### 03.3 Navigation

#### Bottom Navigation Bar (Compact)

```
┌─────────────────────────────────────────────┐
│              Content Area                    │
├─────────────────────────────────────────────┤
│ ▢ Mini Player (56dp, persistent)             │
├──────┬──────┬──────┬──────┬──────────────────┤
│ Home │Search│Library│Setting│  ← 4 Tabs      │
└──────┴──────┴──────┴──────┘
```

| 属性 | 值 |
|---|---|
| Tabs | Home / Search / Library / Settings |
| Height | 56dp + safe area bottom |
| Active | Icon fill + brand color + label bold |
| Inactive | Icon line + `text.tertiary` |
| Label | 10sp 600, uppercase |
| Background | `surface.frosted` (blur.light) |

#### Navigation Rail (Expanded / Large)

| 属性 | 值 |
|---|---|
| Width | 80dp (rail only) / 240dp (rail + labels) |
| Alignment | Top-aligned |
| Items | Same 4 tabs + optional section dividers |
| Active | Pill background + brand color icon |
| Inactive | Transparent + `text.tertiary` icon |

#### Sidebar (Desktop / XL)

| 属性 | 值 |
|---|---|
| Width | 240dp (default) / resizable 200-320dp |
| Content | Logo + Search + Nav items + Playlist tree + User profile |
| Active | 3dp brand color left indicator + `bg.elevated` background |
| Scroll | Independent scroll for playlist tree |

### 03.4 Cards

#### Album Card

| 属性 | 值 |
|---|---|
| Width | `width.card` (adaptive) |
| Aspect | 1:1 (artwork) + text area |
| Artwork | radius.xl (28dp) |
| Title | `text.title3` (16sp 500) |
| Subtitle | `text.caption` (12sp 400) |
| Hover | Scale 1.02 + shadow elevation up |
| Click | Navigate to Album Detail |

#### Music Card (Track Row)

| 属性 | 值 |
|---|---|
| Height | 56dp (compact) / 64dp (comfortable) |
| Layout | Artwork(48dp) + Title/Subtitle + Duration + More |
| Playing | Title → brand color, waveform animation |
| Long press | Context menu (Play next / Add to queue / Download / Share) |

#### Source Card

| 属性 | 值 |
|---|---|
| Layout | Icon(40dp) + Name + Status badge + Music count |
| Status | Connected (green) / Syncing (orange) / Error (red) / Offline (gray) |
| Tap | Expand to detail (Storage / Sync / Scan / Logs / Settings) |
| Background | `surface.primary` with `outline.default` border |

#### Settings Group Card (HyperOS Style)

| 属性 | 值 |
|---|---|
| Background | `surface.primary` |
| Radius | `radius.lg` (24dp) |
| Border | 1px `outline.default` |
| Items | Preference rows with soft dividers (no hard lines) |
| Divider | 1px `divider` with 16dp left indent |
| Header | Optional `text.caption` label above group |

### 03.5 Mini Player

| 属性 | 值 |
|---|---|
| Height | 56dp + safe area |
| Position | Above Bottom Nav, persistent |
| Background | `surface.frosted` (blur.light) |
| Layout | Artwork(40dp) + Title/Artist + Controls(prev/play/next) |
| Artwork | radius.md (18dp) |
| Progress | 2dp line at bottom of Mini Player |
| Tap | Expand to Now Playing (shared element transition) |
| Swipe right | Next track |
| Swipe down | Dismiss (pause) |

### 03.6 Now Playing

| 属性 | 值 |
|---|---|
| Background | Dynamic gradient from album artwork + blur.heavy |
| Artwork | Centered, radius.xl, max 320dp |
| Title | `text.display` (34sp 700) |
| Artist | `text.title3` (16sp 500) with 60% opacity |
| Progress | Slider with brand gradient fill |
| Controls | Shuffle / Prev / Play(64dp gradient) / Next / Repeat |
| Secondary | Queue / Lyrics / Audio Output / Favorite / Download / EQ / Share |
| Lyrics | Slide-in panel from right (or bottom on compact) |
| Gesture | Swipe down to dismiss → Mini Player |

### 03.7 Queue

| 属性 | 值 |
|---|---|
| Type | Bottom Sheet (mobile) / Side Panel (desktop) |
| Background | `surface.frosted` (blur.medium) |
| Header | "Playing Next" + clear button |
| Items | Reorderable Music Card rows |
| Current | Highlighted with brand color accent bar |

### 03.8 Lyrics Panel

| 属性 | 值 |
|---|---|
| Layout | Centered, single column |
| Current line | `text.headline` (28sp 700), full opacity |
| Adjacent lines | `text.body` (14sp 400), 40% opacity |
| Far lines | 15% opacity |
| Animation | Karaoke-style word-by-word highlight (optional) |
| Background | Transparent over Now Playing gradient |

### 03.9 Bottom Sheet

| 属性 | 值 |
|---|---|
| Radius | `radius.2xl` (36dp) top corners |
| Background | `surface.frosted` (blur.medium) |
| Drag handle | 4×36dp `outline.default` (optional) |
| Animation | 280ms slide up + scrim fade |
| Dismiss | Swipe down / tap scrim |

### 03.10 Dialog

| 属性 | 值 |
|---|---|
| Radius | `radius.lg` (24dp) |
| Background | `surface.primary` |
| Position | Center (mobile + desktop) |
| Animation | 200ms scale + fade |
| Scrim | `bg.overlay` (40% black) |

### 03.11 Slider

| 属性 | 值 |
|---|---|
| Track | 4dp height, `bg.sunken` |
| Fill | Brand gradient |
| Thumb | 12dp circle, brand color |
| Active thumb | 16dp, scale animation |
| Scrubbing | Real-time, no delay |

### 03.12 Tabs

| 属性 | 值 |
|---|---|
| Type | Line / Pill / Segmented |
| Underline | 3dp, brand gradient |
| Active | `text.title3` brand color |
| Inactive | `text.title3` `text.tertiary` |
| Animation | 180ms underline slide |
| Accessibility | Tab semantics + selectable group |

### 03.13 Chip / Tag

| 属性 | 值 |
|---|---|
| Shape | Capsule (radius.full) |
| Height | 32dp |
| Background | `bg.elevated` |
| Text | `text.caption` (12sp 500) |
| Selected | Brand color background + white text |
| Examples | Lossless / Hi-Res / Dolby Atmos / FLAC |

---

## 04 Adaptive Layout

### 04.1 Breakpoints

| 名称 | 宽度范围 | 导航形态 | 典型设备 |
|---|---|---|---|
| **Compact** | 0 - 599dp | Bottom Navigation | Phone (Portrait) |
| **Medium** | 600 - 839dp | Bottom Navigation / Rail | Fold (unfolded), Small Tablet |
| **Expanded** | 840 - 1279dp | Navigation Rail | Tablet, Fold (wide) |
| **Large** | 1280 - 1599dp | Sidebar | Desktop, Large Tablet |
| **XL** | 1600dp+ | Sidebar + Detail Panel | Wide Desktop, TV (Future) |

### 04.2 Navigation Adaptation

```
Compact (0-599dp)
┌───────────────┐
│   Content     │
│               │
│ Mini Player   │
├───────────────┤
│ Bottom Nav    │
└───────────────┘

Medium (600-839dp)
┌───────────────┐
│   Content     │
│               │
│ Mini Player   │
├───────────────┤
│ Bottom Nav    │
└───────────────┘
(content uses wider grid, 2→3 columns)

Expanded (840-1279dp)
┌────┬──────────┐
│Rail│  Content │
│    │          │
│    ├──────────┤
│    │Mini Player│
└────┴──────────┘

Large (1280-1599dp)
┌──────┬────────────┐
│      │            │
│Sidebar  Content  │
│      │            │
│      ├────────────┤
│      │ Mini Player│
└──────┴────────────┘

XL (1600dp+)
┌──────┬────────┬───────┐
│      │        │       │
│Sidebar Content Detail │
│      │        │ Panel │
│      ├────────┴───────┤
│      │  Mini Player   │
└──────┴────────────────┘
```

### 04.3 Device Matrix

| 设备 | 断点 | 导航 | 网格列 | Player | 特殊 |
|---|---|---|---|---|---|
| Android Phone | Compact | Bottom Nav | 2 | Full screen | - |
| Android Fold (cover) | Compact | Bottom Nav | 2 | Full screen | Cover screen mode |
| Android Fold (open) | Medium | Bottom Nav | 3 | Full screen | Dual pane option |
| Android Tablet | Expanded | Nav Rail | 4-6 | Side panel | - |
| Android Auto | - | Custom | - | Compact card | 48dp+ touch, large text |
| iPhone | Compact | Bottom Nav | 2 | Full screen | HIG gestures |
| iPad | Expanded | Nav Rail | 4-6 | Side panel | Stage Manager |
| Desktop | Large/XL | Sidebar | 8-12 | Side panel | Resizable panels |

### 04.4 Android Auto

| 属性 | 值 |
|---|---|
| Navigation | 3 tabs: Home / Library / Playing |
| Card height | 48dp+ |
| Button size | 64dp+ |
| Font | `text.title3` minimum (16sp) |
| Colors | High contrast, follow car system theme |
| Layout | Landscape, simplified |

### 04.5 Desktop Layout

```
┌─────────────────────────────────────────────────────┐
│ Toolbar (56dp)                                      │
├────────┬──────────────────────────┬─────────────────┤
│        │                          │                 │
│Sidebar │      Content             │  Lyrics Panel   │
│(240dp) │      (flexible)          │  / Queue Panel  │
│        │                          │  (360dp, optional)│
│        │                          │                 │
│        ├──────────────────────────┴─────────────────┤
│        │          Mini Player (56dp)                │
└────────┴────────────────────────────────────────────┘
```

- Sidebar: Logo + Search + Nav + Playlist Tree + Sources + User
- Content: Main browsing area, adaptive grid
- Right Panel: Toggleable Lyrics / Queue
- Mini Player: Persistent, full width minus sidebar
- All panels resizable

---

## 05 Pages

### 05.1 Navigation Structure

```
Bottom Navigation (4 Tabs):
├── Home
├── Search
├── Library
└── Settings

Now Playing (NOT a tab):
  Opens ONLY from Mini Player
  Mini Player ← tap → Now Playing (shared element)
```

### 05.2 Home

| 模块 | 内容 |
|---|---|
| Hero Banner | Featured album / playlist with dynamic background |
| Continue Listening | Recently played, horizontal scroll |
| Recently Added | Latest additions across all sources |
| Recommended Albums | Grid, adaptive columns |
| Recommended Artists | Circular avatar row, horizontal scroll |
| Recommended Playlist | Grid |
| Pinned Playlists | User-pinned, persistent |
| Recently Played | Horizontal scroll of recently played items |

### 05.3 Search

| 模块 | 内容 |
|---|---|
| Search Bar | Sticky top, `bg.sunken` input |
| Recent Search | Chips of recent queries |
| Trending | Trending searches (if online sources) |
| Results | Tabbed: All / Albums / Artists / Songs / Folders / Sources |
| Filter | By source, by genre, by quality (Lossless/Hi-Res) |

### 05.4 Library

| 模块 | 内容 |
|---|---|
| Songs | Full track list, sortable |
| Albums | Grid |
| Artists | List with avatar |
| Genres | Grid or list |
| Folders | File browser view |
| Playlists | User-created + imported |
| Favorites | Heart-marked tracks |
| Downloads | Offline tracks |
| History | Recently played full list |
| Recently Added | Sorted by date added |
| Recently Played | Sorted by play date |
| Lossless | Filtered: FLAC / ALAC / WAV |
| Hi-Res | Filtered: 24bit / 96kHz+ |
| Sources | → Source Manager |

### 05.5 Source Manager

```
Library → Sources
```

Each Source is a Card with:

| 属性 | 内容 |
|---|---|
| Icon | Source-type-specific icon |
| Name | User-defined name |
| Type | WebDAV / SMB / OneDrive / Google Drive / Dropbox / Emby / Plex / Jellyfin / Navidrome / Local |
| Status | Connected / Syncing / Error / Offline |
| Music Count | Total tracks indexed |
| Storage | Used / Total |
| Actions | Sync / Scan / View Logs / Settings |

Supported Sources:

| Source | Protocol | Auth |
|---|---|---|
| WebDAV | HTTP/HTTPS | Basic / Bearer |
| SMB | SMB 2/3 | NTLM |
| OneDrive | Microsoft Graph | OAuth 2.0 |
| Google Drive | Google Drive API | OAuth 2.0 |
| Dropbox | Dropbox API | OAuth 2.0 |
| Emby | Emby API | API Key |
| Plex | Plex API | Token |
| Jellyfin | Jellyfin API | API Key |
| Navidrome | OpenSubsonic API | Username/Password |
| Local Storage | File System | None |

### 05.6 Settings (HyperOS Style)

All settings use **Group Card** layout — no Material Settings UI.

| Group | Items |
|---|---|
| Transfer & Download | Download quality / Auto-download / Storage location / Download over WiFi only |
| Playback | Gapless playback / Crossfade / Audio output / Equalizer / Replay Gain |
| Library | Scan interval / Exclude folders / Metadata edit / Album art priority |
| Lyrics | Online lyrics source / Translation / Karaoke mode / Font size |
| Appearance | Theme (Light/Dark/System) / Dynamic color / Now Playing style / Language |
| Plugins | Source plugins / Lyrics plugins / Metadata plugins / Plugin settings |
| Advanced | Audio engine / Cache / Network / Debug / Experimental |
| About | Version / Open source licenses / GitHub / Changelog |

### 05.7 Now Playing

| 区域 | 内容 |
|---|---|
| Background | Dynamic gradient from artwork + blur.heavy |
| Artwork | Centered, 280-320dp, radius.xl |
| Title + Artist | Below artwork, centered |
| Progress Bar | Slider with remaining time |
| Controls | Shuffle / Prev / Play(64dp) / Next / Repeat |
| Secondary Row | Queue / Lyrics / AirPlay / Favorite / Download / EQ / Share |
| Lyrics Overlay | Swipe left to reveal, karaoke style |
| Queue Overlay | Swipe right to reveal queue |

### 05.8 Album / Artist / Playlist Detail

Shared layout pattern:

| 区域 | 内容 |
|---|---|
| Header | Large artwork / avatar + title + meta |
| Action Bar | Play all / Shuffle / Download / Sort |
| Track List | Music Card rows |
| Artist only | Top tracks + Albums grid + Related artists |

---

## 06 Prototype

### Key Flows

1. **Play Flow**: Any track tap → Mini Player appears → Tap Mini Player → Now Playing (shared element)
2. **Search Flow**: Search tab → Type → Results (tabbed) → Tap result → Detail page
3. **Source Setup**: Settings → Plugins → Add Source → Choose type → Authenticate → Scan → Indexed
4. **Library Browse**: Library tab → Choose category → Browse → Tap item → Detail
5. **Queue Management**: Now Playing → Queue button → Reorder / Remove → Done

---

## 07 Motion

### 07.1 Motion Hierarchy

```
Level 1: Micro (100-180ms)
  - Button press
  - Tab switch
  - Card tap

Level 2: Transition (280ms)
  - Bottom Sheet enter/exit
  - Dialog appear
  - Page push

Level 3: Hero (380-500ms)
  - Mini Player → Now Playing (shared element)
  - Background blur morph
  - Artwork zoom

Level 4: Ambient (continuous)
  - Vinyl rotation (12s/loop, optional)
  - Audio visualizer
  - Now Playing gradient shift
```

### 07.2 Shared Element Transition

```
Mini Player Artwork (40dp, radius.md)
    ↓ 380ms, cubic-bezier(0.32, 0.72, 0, 1)
Now Playing Artwork (320dp, radius.xl)

Concurrent:
  - Background: transparent → dynamic gradient + blur
  - Controls: scale 0.8 → 1.0 + fade in
  - Title/Artist: translate Y + fade in
  - Progress bar: expand from 2dp to 4dp
```

### 07.3 Blur Morph

```
Mini Player background: surface.frosted (blur.light)
    ↓ 500ms
Now Playing background: blur.heavy + dynamic gradient

The blur radius animates smoothly, creating a "depth dive" effect.
```

---

## 08 Dev Mode

### 08.1 Component Naming

| 类型 | 规则 | 示例 |
|---|---|---|
| Component | PascalCase | `MiniPlayer`, `AlbumCard`, `SourceCard` |
| Variant | `Property=Value` | `State=Playing`, `Size=Large` |
| Token | `category.property` | `color.brand.primary`, `space.16` |
| Icon | `ic-{name}-{line\|fill}` | `ic-play-fill`, `ic-search-line` |
| Page | PascalCase + Screen | `HomeScreen`, `NowPlayingScreen` |

### 08.2 Layer Organization

```
Frame: ScreenName
├── StatusBar (62dp)
├── TopAppBar (optional)
├── Content (scrollable)
│   ├── Section 1
│   │   ├── SectionHeader
│   │   └── SectionContent
│   └── Section 2
├── MiniPlayer (floating)
└── BottomNavigation (56dp + safe area)
```

### 08.3 Export Rules

- All components must be Component/ComponentSet nodes
- No flattened layers inside components
- All colors must use variables (no raw hex in components)
- All spacing must use space tokens
- Export SVG for icons, PNG @1x/@2x/@3x for bitmaps

---

## 09 Compose Mapping

### 09.1 Figma → Compose Component Map（Canonical）

> 以下为 **Figma → Compose Multiplatform → compose-miuix-ui** 的 1:1 标准映射表。
> 标记「自定义」的组件需要基于 compose-miuix-ui 基础组件二次封装。

#### 核心映射（14 项）

| Figma 组件 | Compose Multiplatform | compose-miuix-ui | 类别 |
|---|---|---|---|
| App Scaffold | `Scaffold` | `MiuixScaffold` | Layout |
| Top Bar | `TopAppBar` | `DefaultTopAppBar` | Navigation |
| Group Card | `Card` | `Card` / `CardGroup` | Surface |
| Settings Item | `Preference` | `SuperArrow` | Settings |
| Section Header | List Header | `SmallTitle` | Typography |
| Navigation Rail | `NavigationRail` | `NavigationRail` | Navigation |
| Bottom Navigation | `NavigationBar` | `NavigationBar` | Navigation |
| Mini Player | Custom Component | `MiniPlayer`（自定义） | Player |
| Album Card | `ElevatedCard` | `Card` | Content |
| Source Card | `Card` | `Card` | Content |
| Search Bar | `SearchBar` | `core.presentation.components.TideSearchBar` | Input |
| Lyrics Panel | Custom Layout | `LyricsLayout`（自定义） | Player |
| Queue Panel | `LazyColumn` | `QueueList`（自定义） | Player |
| Adaptive Grid | `LazyVerticalGrid` | `AdaptiveGrid`（自定义） | Layout |

#### 扩展映射（compose-miuix-ui 原生组件）

| Figma 组件 | Compose Multiplatform | compose-miuix-ui | 类别 |
|---|---|---|---|
| Sidebar | `NavigationDrawer` | `NavigationDrawer`（自定义） | Navigation |
| Switch | `Switch` | `core.presentation.components.TideSwitch` | Settings |
| Slider | `Slider` | `SuperSlider` | Settings |
| Dialog | `Dialog` | `core.presentation.components.TideDialog` | Feedback |
| Dropdown | `DropdownMenu` | `SuperDropdown` | Settings |
| Checkbox | `Checkbox` | `core.presentation.components.TideCheckbox` | Settings |
| TextField | `TextField` | `core.presentation.components.TideTextField` | Input |
| Loading Indicator | `CircularProgressIndicator` | `core.presentation.components.TideLoadingIndicator` | Feedback |
| Linear Progress | `LinearProgressIndicator` | `core.presentation.components.TideLinearProgressIndicator` | Feedback |
| Navigation | `NavController` | `Navigator` | Navigation |
| Icon Button | `IconButton` | `core.presentation.components.TideIconButton` | Action |
| Text Button | `TextButton` | `core.presentation.components.TideTextButton` | Action |
| FAB | `FloatingActionButton` | `core.presentation.components.TideFab` | Action |
| Elevated Card | `ElevatedCard` | `FloatingCard` | Surface |
| Now Playing | Custom `NowPlayingScreen` | `NowPlayingScreen`（自定义） | Player |
| Track Row | Custom `TrackRow` | `TrackRow`（自定义） | Content |
| Bottom Sheet | `ModalBottomSheet` | `core.presentation.components.TideBottomSheet` | Overlay |
| Tab Row | `TabRow` | `TabRow`（自定义） | Navigation |
| Chip | `FilterChip` | `core.presentation.components.TideChip` | Input |
| Snackbar | `Snackbar` | `Snackbar` | Feedback |

#### 映射规则

1. **Layout 层**：所有页面必须以 `MiuixScaffold` 为根容器，替代 Material `Scaffold`
2. **Navigation 层**：Compact → `NavigationBar`；Expanded → `NavigationRail`；Large → `NavigationDrawer`
3. **Settings 层**：全面使用 compose-miuix-ui 的 `Super*` 系列组件，不使用 Material `ListItem`
4. **Surface 层**：分组卡片用 `CardGroup`，独立卡片用 `Card` / `FloatingCard`
5. **自定义组件**：`MiniPlayer` / `LyricsLayout` / `QueueList` / `AdaptiveGrid` / `NowPlayingScreen` / `TrackRow` 需基于 compose-miuix-ui 基础组件二次封装，命名以 `TideTunes` 为前缀
6. **渐变 FAB**：使用 `Brush.linearGradient()` 在 `FloatingActionButton` 上叠加品牌渐变

### 09.2 Token → Kotlin Constant Map

| Design Token | Kotlin Constant | File |
|---|---|---|
| `color.brand.primary` | `val BrandPrimary = Color(0xFFFF5B8A)` | `Color.kt` |
| `color.brand.secondary` | `val BrandSecondary = Color(0xFF7A6CFF)` | `Color.kt` |
| `color.brand.gradient` | `val BrandGradient = Brush.linearGradient(...)` | `Color.kt` |
| `color.light.bg.base` | `val LightBgBase = Color(0xFFFFFFFF)` | `Color.kt` |
| `color.dark.bg.base` | `val DarkBgBase = Color(0xFF0B0B0F)` | `Color.kt` |
| `text.display` | `val DisplayTextStyle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp)` | `Type.kt` |
| `space.16` | `val Space16 = 16.dp` | `Spacing.kt` |
| `radius.lg` | `val RadiusLg = RoundedCornerShape(24.dp)` | `Shape.kt` |
| `motion.expand` | `val MotionExpand = tween(380, easing = FastOutSlowInEasing)` | `Motion.kt` |
| `blur.heavy` | `val BlurHeavy = 32.dp` | `Blur.kt` |
| `elevation.floating` | `val FloatingElevation = 8.dp` | `Elevation.kt` |

### 09.3 Adaptive Layout API

```kotlin
@Composable
fun TideTunesApp() {
    val windowSizeClass = currentWindowAdaptiveInfo()
    val navType = when (windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> NavigationType.BottomNav
        WindowWidthSizeClass.Medium -> NavigationType.BottomNav
        WindowWidthSizeClass.Expanded -> NavigationType.Rail
        else -> NavigationType.Sidebar
    }
    
    MiuixScaffold(
        bottomBar = {
            if (navType == NavigationType.BottomNav) {
                TideTunesBottomNav()
            }
        }
    ) {
        when (navType) {
            NavigationType.BottomNav -> CompactLayout()
            NavigationType.Rail -> ExpandedLayout()
            NavigationType.Sidebar -> LargeLayout()
        }
    }
}
```

### 09.4 Directory Structure (v3)

```
shared/
├── src/commonMain/kotlin/com/tidetunes/
│   ├── theme/
│   │   ├── Color.kt          # Brand + Light/Dark color tokens
│   │   ├── Type.kt           # Type scale
│   │   ├── Shape.kt          # Radius tokens
│   │   ├── Spacing.kt        # Space tokens
│   │   ├── Motion.kt         # Motion tokens
│   │   ├── Blur.kt           # Blur tokens
│   │   ├── Elevation.kt      # Elevation tokens
│   │   └── Theme.kt          # TideTunesTheme entry
│   ├── components/
│   │   ├── MiniPlayer.kt
│   │   ├── NowPlaying.kt
│   │   ├── TrackRow.kt
│   │   ├── AlbumCard.kt
│   │   ├── SourceCard.kt
│   │   ├── SettingsGroup.kt
│   │   ├── LyricsPanel.kt
│   │   ├── QueueSheet.kt
│   │   └── GradientPlayButton.kt
│   ├── features/
│   │   ├── home/
│   │   ├── search/
│   │   ├── library/
│   │   ├── settings/
│   │   ├── sources/
│   │   └── player/
│   ├── source/               # SourceAdapter architecture
│   │   ├── SourceAdapter.kt
│   │   ├── SourceType.kt
│   │   └── adapters/
│   │       ├── WebDAVAdapter.kt
│   │       ├── SMBAdapter.kt
│   │       ├── OneDriveAdapter.kt
│   │       ├── PlexAdapter.kt
│   │       ├── EmbyAdapter.kt
│   │       ├── JellyfinAdapter.kt
│   │       ├── NavidromeAdapter.kt
│   │       └── LocalAdapter.kt
│   └── App.kt
├── src/androidMain/
├── src/iosMain/
├── src/desktopMain/
```

### 09.5 compose-miuix-ui Integration

```kotlin
// Settings page using compose-miuix-ui
@Composable
fun SettingsScreen() {
    MiuixScrollColumn {
        SmallTitle("Transfer & Download")
        CardGroup {
            SuperArrow(
                title = "Download Quality",
                summary = "Lossless (FLAC)",
                onClick = { /* show dropdown */ }
            )
            SuperSwitch(
                title = "Auto-download",
                checked = autoDownload,
                onCheckedChange = { }
            )
        }
        
        SmallTitle("Playback")
        CardGroup {
            SuperSwitch(title = "Gapless Playback", ...)
            SuperSlider(title = "Crossfade", value = crossfade, ...)
            SuperArrow(title = "Equalizer", onClick = { })
        }
        
        SmallTitle("Appearance")
        CardGroup {
            SuperArrow(title = "Theme", summary = "Follow System", ...)
            SuperSwitch(title = "Dynamic Color", ...)
        }
    }
}
```

---

## Figma AI System Prompt

> 以下 Prompt 作为 Figma Make / AI 设计工具的 System Prompt，所有页面基于此生成。

```text
Create a production-ready cross-platform Design System for a music application called "TideTunes".

Design language:
- Xiaomi HyperOS / MIUI (compose-miuix-ui)
- Apple Music Information Architecture
- Material 3 Dynamic Color
- Compose Multiplatform

The goal is NOT to create static mockups.
The goal is to create a reusable responsive design system suitable for Android, iOS and Desktop development.

BRAND:
  Product Name: TideTunes
  Tagline: One Library. Every Source.
  Keywords: Minimal, Elegant, Immersive, Content First, Music First, Adaptive, Native, Cross Platform, Plugin Driven

FOUNDATION:
  Spacing: 4, 8, 12, 16, 20, 24, 32, 40, 48
  Radius: 12, 18, 24, 28, 36, full
  Elevation: Surface, Card, Popup, Floating, Overlay
  Blur: None, Light(8), Medium(16), Heavy(32), Ultra(48)
  Typography: Display(34/700), Headline(28/700), Title1(22/600), Title2(18/600), Title3(16/500), Body(14/400), Callout(13/400), Caption(12/400), Micro(10/600)

COLOR:
  Primary: #FF5B8A
  Secondary: #7A6CFF
  Gradient: linear(90°, #FF5B8A → #7A6CFF)
  Support: Blue(#4D99FF), Orange(#FF994D), Green(#33CC99), Yellow(#FFD94D)
  Light: bg #FFFFFF, elevated #F7F8FA, text #1A1A1F
  Dark: bg #0B0B0F, elevated #16161D, text #F5F5F7
  Dynamic Color from Album Artwork

COMPONENTS:
  Buttons, Navigation Bar, Navigation Rail, Sidebar, Search Bar, Top App Bar,
  Mini Player, Player Controls, Album Card, Artist Card, Playlist Card,
  Music Card, Source Card, Settings Group, Preference Item, Switch, Slider,
  Dialog, Bottom Sheet, Snackbar, Queue, Lyrics Panel
  All Auto Layout, All Variants, All Dev Mode ready

RESPONSIVE:
  Breakpoints: Compact(0-599), Medium(600-839), Expanded(840-1279), Large(1280+), XL(1600+)
  Navigation: Bottom Nav → Rail → Sidebar (adaptive)
  Cards: adaptive grid columns
  Mini Player: persistent across all sizes

NAVIGATION:
  4 Tabs: Home, Search, Library, Settings
  No "Now Playing" tab — opens from Mini Player only

HOME: Hero Banner, Continue Listening, Recently Added, Recommended Albums/Artists/Playlists, Pinned Playlists, Recently Played

SEARCH: Global Search, Recent Search, Trending, Results (All/Albums/Artists/Songs/Folders/Sources)

LIBRARY: Songs, Albums, Artists, Genres, Folders, Playlists, Favorites, Downloads, History, Recently Added, Recently Played, Lossless, Hi-Res, Sources

SOURCE MANAGER: Each Source is a Card (WebDAV, SMB, OneDrive, Google Drive, Dropbox, Emby, Plex, Jellyfin, Navidrome, Local) with Status, Music Count, Storage, Sync, Logs, Settings

SETTINGS: HyperOS style — Grouped Cards, Large rounded corners, Soft dividers, NO Material Settings UI. Groups: Transfer & Download, Playback, Library, Lyrics, Appearance, Plugins, Advanced, About

NOW PLAYING: Large Artwork, Dynamic Blur, Gradient Background, Lyrics, Queue, Audio Output, Favorite, Download, EQ, Share, Shared Element Transition from Mini Player

MOTION: HyperOS rhythm, Spring animation, Hero transition, Shared Element, Blur Morph, Artwork zoom

DESKTOP: Apple Music inspired — Toolbar + Sidebar + Content + Lyrics/Queue Panel + Mini Player, all resizable

DEV MODE: Reusable components, no duplicated layers, proper naming, Variables, Design Tokens, Component Properties, ready for Compose Multiplatform
```

---

## Version History

| Version | Date | Changes |
|---|---|---|
| v1.0 | 2026-06-24 | 首版交付：5 Tab、基础设计 Token、15 页面 |
| v3.0 | 2026-07-05 | 完全重构：4 Tab（Apple Music IA）、新品牌色 (#FF5B8A/#7A6CFF)、HyperOS 设计语言、Source 架构、Adaptive 断点系统、Compose Mapping、compose-miuix-ui 集成 |
| v4.0 | 2026-07-06 | 响应式多设备系统升级：7 种设备类别 (Phone / Foldable / Tablet / iPhone / iPad / Car / Desktop) × 5 个断点 (Compact/Medium/Expanded/Large/XL) × 4 个 Mini Player 状态 (Compact/Expanded/Floating/Full) × MIUI HyperOS 设置页；新增 `TideTunes Responsive — Multi-Device System` Frame（3400×5800，5:2）|

---

## 10 Responsive Multi-Device System (v4)

> **设计哲学**：One Design System · Every Device — 同一套设计语言、组件 Token、交互语法在 7 种设备类别中按平台惯例自然变体。

### 10.1 Breakpoint System

| Breakpoint | Width Range | Device Class | Navigation | Layout | Mini Player |
|---|---|---|---|---|---|
| **Compact** | 0 – 599 dp / pt | Android Phone, iPhone, Foldable Cover | Bottom Navigation Bar (4 Tab) | 1 列 | Compact Bar（底部 dock，h≈64dp） |
| **Medium** | 600 – 839 dp / pt | Foldable Expanded, Small Tablet | Navigation Rail（72dp） | 2 列 | Floating Dock（绝对定位，glassmorphism） |
| **Expanded** | 840 – 1279 dp / pt | Android Tablet, iPad, Android Auto | Navigation Rail (80dp) + Sidebar (240dp) | 2-3 列 | Floating Dock（绝对定位） |
| **Large** | 1280 – 1599 px | Desktop (1280×800, 1440×900) | Left Sidebar (220px) + Title Bar | 3-5 列 + Right Panel | Floating Dock（绝对定位，桌面底部） |
| **XL** | 1600+ px | Desktop (1920×1080, 4K) | Left Sidebar (260px) + Title Bar | 4-6 列 + Right Panel (Lyrics) | Floating Dock + Now Playing Modal |

### 10.2 Navigation Architecture (Apple Music IA)

```
4 个 Tab (移除 Playing Tab):
├── Home          — 推荐 / 今日精选 / 最近播放
├── Search        — 搜索 / 浏览 / 流派
├── Library       — 资料库（专辑 / 艺人 / 歌单 / 下载）
└── Settings      — 设置 / Source 管理 / 偏好

→ Mini Player 是进入 Now Playing 的唯一入口
   Compact Bar → 展开手势 → Expanded Bar / Floating Dock → 点击 → Full Now Playing (Modal)
```

### 10.3 Mini Player State System

| State | Trigger | Visual | Use Case |
|---|---|---|---|
| **1. Compact Bar** | 默认 / 选中内容 Tab | 底部细条（h≈64dp），封面+标题+播放+下一首 | Phone (iOS/Android)、Foldable Cover |
| **2. Expanded Bar** | 向上滑动 Mini Player | 较宽条（h≈80dp），+ 进度条 + 上一首 + 队列 | Tablet、Foldable Expanded |
| **3. Floating Dock** | Desktop 布局 | 浮动小圆角胶囊（绝对定位，glassmorphism），居中底部 | Desktop、TV（未来） |
| **4. Full Now Playing** | 点击 Mini Player / 展开手势 | 全屏模态，深色渐变背景 + 大封面 + 完整控件 + 歌词 | 全部设备（共享元素动画） |

**共享元素动画**：封面 + 标题在 4 个状态间平滑过渡，Blur Morph 背景渐变。

### 10.4 MIUI HyperOS Settings (Settings Page)

- **布局**：分组卡片样式（CardGroup），每组独立圆角卡片
- **大圆角**：卡片 cornerRadius 16-28dp
- **大行距**：行高 56-64dp，垂直 padding 12dp
- **图标**：左侧 32×32 圆角方块 + 彩色背景（透明 12%）+ 居中字母/符号
- **控件**：右侧 SuperSwitch / 数值标签 / chevron / 状态文字
- **柔和阴影**：DROP_SHADOW a=0.04, blur=12, offset=(0,2)
- **轻模糊**：BACKGROUND_BLUR blurRadius=20
- **分割线**：stroke 1px color #F0F0F0 strokeAlign INSIDE（卡片内）
- **5 个分组**：
  1. **Transfer & Download**：Cellular Streaming (Switch) / Cache Size (5GB) / Streaming Quality (Hi-Res) / Wi-Fi Only (Off)
  2. **Playback**：Repeat Mode (All) / Shuffle (On) / Loudness Normalization / Crossfade / Sleep Timer
  3. **Library**：Source Manager (3) / Scan Library / Metadata Editor
  4. **Appearance**：Theme (Auto) / Accent Color (Pink) / Dynamic Background (On) / Now Playing Style (Glass)
  5. **Advanced**：Audio Output / Network Diagnostics / Storage / About

### 10.5 Device Layout Matrix

| Device | Viewport | Navigation Pattern | Columns | Mini Player |
|---|---|---|---|---|
| **Android Phone** | 360×780 | Bottom Nav (4 Tab) | 1 列 + 横排卡片 2 | Bottom Dock |
| **iPhone** | 375×780 | Bottom Tab Bar (毛玻璃) | 1 列 + Hero + 列表 | Bottom Dock (毛玻璃) |
| **Foldable Cover** | 320×680 | Bottom Nav (4 Tab) | 1 列 | Bottom Dock |
| **Foldable Expanded** | 680×780 | Navigation Rail (72dp) | 2 列 | Floating Dock |
| **Android Tablet** | 960×680 | Nav Rail (80dp) + Sidebar (240dp) | 3 列 | Floating Dock |
| **iPad** | 960×680 | Sidebar (220dp) + Main + 可选 Queue | Hero + 2×2 网格 | Floating Dock |
| **Android Car** | 960×480 (横屏) | 左侧大按钮（48dp 触控目标） | 大封面 + 控件 | 中心底部 |
| **Desktop** | 1280×720 | Title Bar + Left Sidebar (220px) + Right Panel | 3-5 列 + Queue/Lyrics | Floating Dock + Now Playing Modal |

### 10.6 关键约束

- **禁止固定布局**：所有组件必须支持流体缩放和自适应布局切换
- **平台特定导航模式**：Phone → Bottom Nav；Tablet → Rail+Sidebar；Desktop → Sidebar；Car → 简化大按钮
- **条件渲染**：根据 dp/pt 宽度选择导航结构和内容密度
- **Mini Player 全局持久**：4 状态共享，覆盖任何页面
- **共享元素动画**：封面 + 标题贯穿所有状态，使用 Blur Morph 过渡
- **Settings 分组卡片**：MIUI HyperOS 风格，5 个独立 CardGroup
- **Day/Night 视觉一致性**：断点适配同时支持双主题

### 10.7 Frame Reference

| Frame | ID | 尺寸 | 位置 |
|---|---|---|---|
| TideTunes DS v3 — Overview | 2:2 | 1440×3600 | x:0, y:0 |
| TideTunes DS v3 — Compose Mapping | 3:2 | 1440×2000 | x:1540, y:0 |
| **TideTunes Responsive — Multi-Device System** | **5:2** | **3400×5800** | **x:0, y:3700** |
| TideTunes DS v5 — Figma Make Alignment (Frame 容器) | 5:853 | 3400×5800 | x:0, y:9700 |

---

## 11. Figma Make Alignment (v5 增量)

> **来源**：https://www.figma.com/make/X30WgdPxOW9skIgTUCgk4b/Design-System-for-TideTunes
> **抓取时间**：2026-07-06
> **抓取方式**：Playwright 截图 + 文本提取（Figma Make 屏蔽 REST API，仅 images 端点可访问）
> **目标**：把 Figma Make 提供的 AI 生成设计系统与 v3/v4 已有规范对齐，识别互补点和冲突点

### 11.1 Figma Make 元数据

| 字段 | Figma Make 给出 | 与 v3/v4 关系 |
|---|---|---|
| **文件类型** | `editorType: "make"`（AI 生成的 React 组件原型，非 Figma 原生文件） | 不可用 Figma REST API 解析 |
| **文件状态** | Beta · Version 1 | — |
| **生成方式** | AI 把 prompt 翻译成 React + CSS 代码，用 iframe 嵌入 Figma | 不可编辑节点树 |
| **渲染目标** | Web（Plus Jakarta Sans + JetBrains Mono + theme.css） | v5 已落地 Plus Jakarta Sans + JetBrains Mono |

### 11.2 Figma Make 设计令牌

#### 11.2.1 品牌色（**完全一致** ✓）

| Token | Hex | 与 v3/v4 对比 |
|---|---|---|
| **TidePink** | `#FF5B8A` | ✓ v3/v4 一致 |
| **TidePurple** | `#7A6CFF` | ✓ v3/v4 一致 |
| Orange | `#FF994D` | ✓ v5 截图采样 |
| Green | `#33CC99` | ✓ v5 截图采样 |
| Blue | `#4D99FF` | ✓ v5 截图采样 |
| Yellow | `#FFD94D` | ✓ v5 截图采样 |

**结论**：品牌色与 v3/v4 100% 对齐，无需调整。

#### 11.2.2 渐变对（8 个）

Figma Make 声明使用 8 个渐变对。基于 `Design/.workbuddy/screenshots/5_853.png` 像素采样，具体 token 已合并到 02.3：
- 90° `#7A6CFF → #FF5B8A`
- 90° `#FF5B8A → #7A6CFF`
- 90° `#FF5B8A → #FF994D`
- 90° `#FF994D → #FFD94D`
- 90° `#33CC99 → #4D99FF`
- 90° `#4D99FF → #7A6CFF`
- 90° `#FF5B8A → #FFD94D`
- 90° `#4D99FF → #33CC99 → #7A6CFF`

**决策**：第 8 张截图卡片视觉上包含 Blue / Green / Purple 三段 stop，作为三色 blend 保留，不强行改写为二色 pair。

#### 11.2.3 圆角（**已对齐** ✓）

| 等级 | Figma Make | v3/v4 | 决策建议 |
|---|---|---|---|
| sm | 12 | 12 | ✓ 已对齐 |
| md | 20 | 20 | ✓ 已对齐 |
| lg | 28 | 28 | ✓ 已对齐 |
| xl | 36 | 36 | ✓ 已对齐 |

**状态**：`TideTunesShapes` 已统一为 Figma Make 标准（sm=12、md=20、lg=28、xl=36）；`TideCardSurface`、`TideDialog`、`TideChip`、`TideSettingsGroup` 等组件及 app bar 外壳均通过 `TideTunesTokens.shapes` 引用这些值，无差异。

#### 11.2.4 字体（**新发现**）

| 用途 | Figma Make | v3/v4 | 决策建议 |
|---|---|---|---|
| Display/标题 | **Plus Jakarta Sans** | 平台默认字体 | **已升级为 Plus Jakarta Sans** |
| Body | Plus Jakarta Sans | 平台默认字体 | **已升级为 Plus Jakarta Sans** |
| Badges/数据 | **JetBrains Mono** | 平台 monospace | **已升级为 JetBrains Mono** |

**实现**：`core.presentation.theme.TideTunesFontFamilies` 提供 `Sans` / `Mono`，`TideTunesTheme` 全局 `TextStyles` 使用 Plus Jakarta Sans，`QualityBadge` 使用 JetBrains Mono。

#### 11.2.5 暗色/亮色模式

Figma Make 通过 CSS 自定义属性实现完整 dark/light 切换，与 v4 的 token 系统一致。**当前 App 状态**：`SettingsRepository.settings.themeMode` 已通过 `shared.Root` 接入 `core.presentation.theme.TideTunesTheme`，支持 System / Light / Dark 三档；`TideTunesTheme` 使用 `ThemeController` 注入 `TideTunesLightColors` / `TideTunesDarkColors`，并按显式主题模式计算 `effectiveDarkTheme`，避免系统深色状态覆盖用户选择。**剩余设计资产项**：Figma / Design token 的 CSS custom properties 自动切换脚本仍未落地。

### 11.3 组件库对齐

#### 11.3.1 Primitives（基础组件）

| 组件 | Figma Make | v3/v4 | 差异 |
|---|---|---|---|
| **TideButton** | 5 variants (primary/secondary/tertiary/ghost/danger) | `core.presentation.components.TideButton` | ✓ 5 variants 已实现 |
| **TideIconButton** | icon action buttons | `core.presentation.components.TideIconButton` | ✓ 已实现（旧入口委托） |
| **TidePlayerControlButton** | Mini Player / Player Controls | `core.presentation.components.TidePlayerControlButton` | ✓ 已实现（Mini Player / Now Playing 主控接入） |
| **TideTextButton** | action text buttons | `core.presentation.components.TideTextButton` | ✓ 已实现（旧入口委托） |
| **TideActionPill** | compact icon + text action pills | `core.presentation.components.TideActionPill` | ✓ 已实现（Home quick actions 接入） |
| **TideSwitch** | ✓ | `core.presentation.components.TideSwitch` | ✓ 已实现（Settings rows 接入） |
| **TideSlider** | ✓ | `core.presentation.components.TideSlider` | ✓ 已实现（Now Playing 进度条接入） |
| **TideTabs** | 3 variants (line/pill/segmented) | `core.presentation.components.TideTabs` | ✓ 3 variants 已实现 |
| **TideFab** | gradient FAB | `core.presentation.components.TideFab` | ✓ 已实现（Import / Playlists 接入） |
| **TideCheckbox** | ✓ | `core.presentation.components.TideCheckbox` | ✓ 已实现（Import / AppCheckbox 接入） |
| **TideTextField** | ✓ | `core.presentation.components.TideTextField` | ✓ 已实现（Settings / WebDAV / Form 接入） |
| **QualityBadge** | ✓ (FLAC/Hi-Res/Atmos) | — | **v5 新增**（音频质量标识） |

**TideButton Compose 定义**：`core.presentation.components.TideButton` 提供 `Primary` / `Secondary` / `Tertiary` / `Ghost` / `Danger` 五个 variants，基于 Miuix Button 与 TideTunes tokens，使用 `radius.full` 胶囊形态；`AppButton` 作为兼容入口已委托到该组件并保留 colors override；Settings 确认弹窗已接入 `Danger`，输入保存弹窗与 Onboarding 主 CTA 已接入 `Primary`。

**TideIconButton Compose 定义**：`core.presentation.components.TideIconButton` 提供 `Default` / `Surface` / `Primary` / `Error` / `ErrorFilled` 五个 icon action variants，并保留 Small / Medium / Touch / Large 四档尺寸；`Touch` 为 PDS 44dp touch target，`Surface` 使用 `surfaceContainerHigh + primary icon`；`TideTunesIconButton` 与 `AppIconButton` 作为兼容入口已委托到该组件，Import / Source Editor / Playlist / Now Playing 等既有按钮行为保持不变；`FormText` 密码可见性 trailing icon 与 Home header search action 已接入。

**TidePlayerControlButton Compose 定义**：`core.presentation.components.TidePlayerControlButton` 提供 `Mini` / `Large` 两档尺寸与 `Secondary` / `Primary` 两种 player control variants；Mini Player 上一首/下一首使用 30dp secondary control，Mini Player 播放/暂停使用 34dp brand gradient control，Now Playing 主播放/暂停使用 64dp brand gradient control，均保留原 disabled state 与图标尺寸。

**TideTextButton Compose 定义**：`core.presentation.components.TideTextButton` 提供 `Default` / `Primary` / `PrimaryFilled` / `Error` 四个 action text button variants，并保留 Medium / Small 两档高度；`TideTunesTextButton` 与 `AppTextButton` 作为兼容入口已委托到该组件；Downloads task actions（Pause / Resume / Retry / Cancel）与 Onboarding Skip / Back 已直接接入该组件，现有页面行为保持不变。

**TideActionPill Compose 定义**：`core.presentation.components.TideActionPill` 提供 48dp 高度、`radius.full`、`surfaceContainerHigh + outline` 的 icon + text 胶囊 action，使用 17dp icon、6dp icon/text gap 与 `footnote1` 文本；Home quick actions（资料库 / 下载 / 睡眠）已接入，保留原有导航/动作行为。

**TideSwitch Compose 定义**：`core.presentation.components.TideSwitch` 作为 Miuix `Switch` 的 Design System canonical wrapper，保留平台化交互、enabled state 与 cross-platform 行为；Settings switch rows 已直接接入，`AppSwitch` 保留为兼容入口并委托到 `TideSwitch`。

**TideTabs Compose 定义**：`core.presentation.components.TideTabs` 提供 `Line` / `Pill` / `Segmented` 三个 variants，支持 disabled tab、badge/count、180ms active state 动画与 `Role.Tab` selectable semantics；Library 分类条已接入 `Pill` variant，Create Playlist 模式切换已接入 `Segmented` variant。

**TideFab Compose 定义**：`core.presentation.components.TideFab` 提供 56dp 圆形 FAB，使用品牌 Pink→Purple 渐变、1dp 高亮边框与 `Role.Button` semantics；Import selection confirm FAB 与 Playlists adjust confirm FAB 已接入，业务层不再直接使用 Miuix `FloatingActionButton`。

**TideCheckbox Compose 定义**：`core.presentation.components.TideCheckbox` 提供 20dp checkbox，使用 `radius.xs`、1dp outline、Primary checked fill、disabled state 与 `Role.Checkbox` toggle semantics；Import entry selection 与 `AppCheckbox` wrapper 已接入，保留 `TideTunesCheckbox` 作为兼容入口。

**TideTextField Compose 定义**：`core.presentation.components.TideTextField` 作为 Miuix `TextField` 的 Design System canonical wrapper，统一 label-as-placeholder、enabled、singleLine、keyboard options、visual transformation 与 trailing icon API；Settings cache input、WebDAV account dialog 与 `FormText` 已接入，`AppTextField` 保留为兼容入口并委托到 `TideTextField`。

**QualityBadge Compose 定义**：`core.presentation.components.QualityBadge` 提供 `FLAC` / `Hi-Res` / `Dolby Atmos` 三档，使用 `radius.full` 胶囊、品牌/辅助色低透明底色、1dp 边框、`footnote2 + TideTunesFontFamilies.Mono` 文本。首页推荐曲目已作为首个 App UI 落点。

**TideTrackNumberBadge Compose 定义**：`core.presentation.components.TideTrackNumberBadge` 提供 40dp 圆形 track / queue 序号 badge，默认使用 `secondaryContainer`，active 状态使用 TidePink → TidePurple 渐变，并统一使用 JetBrains Mono 数字；Album / Artist / Playlist / Queue track rows 已接入。

**TideSlider Compose 定义**：`core.presentation.components.TideSlider` 提供 4dp track、品牌渐变 fill、12dp thumb、active thumb scale、实时拖动/点击与 accessibility progress semantics；`bufferedValue` 支持播放器进度条的缓冲层，`service.playback.presentation.nowplaying.MusicSlider` 已接入该公共组件。

#### 11.3.2 Music Cards（音乐卡片）

| 卡片 | Figma Make | v3/v4 | 状态 |
|---|---|---|---|
| **CardSurface** | ✓ | `TideCardSurface` | ✓ 已落地（Downloads task cards / Source Manager cards / Search cards / Browse music cards / Playlist cards / Library track cards / Home recommended tracks / Album & Artist detail cards / Import & Source Editor panels 接入） |
| **AlbumCard** | ✓ | ✓ | ✓ 已对齐 |
| **ArtistCard** | ✓ | ✓ | ✓ 已对齐 |
| **PlaylistCard** | ✓ | ✓ | ✓ 已对齐 |
| **MusicCard** (single track) | ✓ | ✓ | ✓ 已对齐 |
| **MusicArtworkTile** | ✓ | `TideMusicArtworkTile` | ✓ 已落地（Home 推荐曲目接入） |
| **TrackListRow** | ✓ | `TideTrackListRow` | ✓ 已落地（Genre / Radio / Recently Added / Recently Played track rows 接入） |
| **SourceCard** | ✓ | ✓ | ✓ 已对齐 |

**TideMusicArtworkTile Compose 定义**：`core.presentation.components.TideMusicArtworkTile` 提供 single-track music card 的 48dp 彩色封面占位 tile，使用 `radius.md`、track accent color → 45% alpha 的线性渐变；Home 推荐曲目的占位封面已接入，保留原有点击与 `QualityBadge` 展示行为。

**TideCardSurface Compose 定义**：`core.presentation.components.TideCardSurface` 提供 Figma Card / Elevated Card 的基础 surface shell，统一 `surfaceContainer` 背景、1dp `outline`、`radius.lg` 默认圆角、可配置 content padding、可选 click handler 与紧凑卡片 `fillMaxWidth` opt-out；`AppCard` 兼容入口已委托到该组件；Downloads task cards、Source Manager cards、Search status / category / result cards、Browse album / artist cards、Playlist empty / grid cards、Playlist detail empty state、Library track cards、Home recommended track container、Album track rows、Artist album cards、Artist track rows、Import entry / warning panels、Source Editor config panel 与 Onboarding central panel 已接入，保留原状态、进度、metadata、artwork、reorder、row click 与 action 布局。

**TideTrackListRow Compose 定义**：`core.presentation.components.TideTrackListRow` 提供列表页通用 track surface，基于 `TideCardSurface` 统一 `radius.lg`、`surfaceContainer`、1dp `outline` 与 12dp padding，内部使用标题 `body1 + SemiBold`、副标题/时长 `footnote1` 与可选 trailing action；Genre、Radio、Recently Added、Recently Played track rows 已接入，保留原播放与下载行为。

**SourceCard App UI 对齐**：`feature.sources.presentation.SourceCard` 已按 Source Manager 规格展示 `Status`、source type、`Music Count`、`Storage` 信息，卡片 shell 使用 `TideCardSurface`，`Status` 使用 `TideStatusBadge`，并在卡片底部使用 `TideChip` action strip 露出 `Sync` / `Logs` / `Settings` affordance；当前不新增业务 action，`Sync` / `Logs` 为 disabled affordance，整卡点击仍保持进入现有 Source Settings / Editor 行为。

#### 11.3.3 Layout（布局组件）

| 组件 | Figma Make | v3/v4 | 状态 |
|---|---|---|---|
| **TopBar** | ✓ | `TideTopBar` | ✓ 已落地（Settings page top bars / AppTopBar 兼容入口接入） |
| **SectionHeader** | ✓ | `TideSectionHeader` | ✓ 已落地（Album / Artist / Browse / Home / Playlist section headers 接入） |
| **PageHeader** | ✓ | `TidePageHeader` | ✓ 已落地（Browse / Downloads / Genre / Library / Queue / Radio / Search / Recently Added / Recently Played 接入） |
| **DetailHeaderSurface** | ✓ | `TideDetailHeaderSurface` | ✓ 已落地（Album / Artist / Playlist header 接入） |
| **SettingsGroup** | ✓ | `TideSettingsGroup` | ✓ 已落地（Settings sections 委托到 core 分组卡片） |
| **PreferenceRow** | ✓ | `TidePreferenceRow` | ✓ 已落地（Settings entry / choice / switch / info / danger rows 接入） |
| ListItem | ✓ | ✓ | ✓ |
| **SearchBar** | ✓ | ✓ `TideSearchBar` | ✓ 已落地（Search Page 接入） |
| **Chip / Tag** | ✓ | ✓ `TideChip` | ✓ 已落地（Search suggestions 接入） |
| **ChipSection** | ✓ | `TideChipSection` | ✓ 已落地（Search recent / trending suggestions、Browse genres 接入） |
| **Dialog** | ✓ | ✓ `TideDialog` | ✓ 已落地（业务 Dialog 统一接入） |
| **BottomSheet** | ✓ | ✓ `TideBottomSheet` | ✓ 已落地（AppBottomSheet 兼容入口接入） |
| **EmptyState** | ✓ | ✓ `TideEmptyState` | ✓ 已落地 |
| **StatusCard** | ✓ | `TideStatusCard` | ✓ 已落地（Album / Artist / Browse / Genre / Lyrics / Queue / Radio / Recently Added / Recently Played 状态接入） |
| **Toast** | ✓ | ✓ `TideToast` | ✓ 已落地 |
| **SkeletonBlock** | ✓ | ✓ `SkeletonBlock` | ✓ 已落地 |
| **IconBadge** | ✓ | ✓ `TideIconBadge` | ✓ 已落地（Downloads / Search marker / category grid / Library track marker 接入） |
| **LoadingIndicator** | ✓ | ✓ `TideLoadingIndicator` | ✓ 已落地（状态卡 / Search loading 接入） |
| **LinearProgressIndicator** | ✓ | ✓ `TideLinearProgressIndicator` | ✓ 已落地（Downloads task progress 接入） |

**TideSearchBar Compose 定义**：`core.presentation.components.TideSearchBar` 使用 `surfaceContainerHigh` 胶囊输入面、1dp outline、品牌色 cursor，内置 `ImeAction.Search` 与 clear action；Search Page 已接入，保持 `QueryChanged` / `SubmitSearch` / `ClearQuery` 行为不变。

**TideTopBar Compose 定义**：`core.presentation.components.TideTopBar` 提供紧凑页面导航顶部栏，统一 48dp leading slot、`title3` 标题、8dp 横向页面内距、可配置高度与 trailing actions；`TideTopBarBackButton` 使用 `TideChevron` 与 48dp touch target。Settings 子页面顶部栏与 `AppTopBar` 兼容入口已委托到该组件。

**TidePageHeader Compose 定义**：`core.presentation.components.TidePageHeader` 提供普通页面顶部标题区，统一 `title2 + Bold`、`footnote1` subtitle、`onBackground` / `onBackgroundVariant` 文本色、4dp 标题间距与可选 trailing action；Browse、Downloads、GenreTracks、Library、Queue、Radio、Search、Recently Added、Recently Played 已接入。详情页 hero header 继续使用 `TideDetailHeaderSurface`，歌词居中标题保持专属布局。

**TideSectionHeader Compose 定义**：`core.presentation.components.TideSectionHeader` 提供内容区 section title，支持 Standard / Compact / Subtle 三种局部变体、metadata count、accent metadata 与 trailing action；Album / Artist / Playlist 内部 track/release sections、Browse 分类 sections、Home 内容 sections 已接入，`AppSectionHeader` 兼容入口已委托到该组件。

**TideSettingsGroup Compose 定义**：`core.presentation.components.TideSettingsGroup` 提供 HyperOS Settings 分组卡片，使用可选 section label、`surfaceContainer` 背景、`radius.lg` 圆角、1dp outline 与 8dp header/content 间距；Settings 页面所有 `SettingsSection` 兼容入口已委托到该组件。

**TidePreferenceRow Compose 定义**：`core.presentation.components.TidePreferenceRow` 提供 Settings Preference 行骨架，支持 title、summary、enabled alpha、leading marker、trailing control/disclosure、danger title color 与 soft divider indent；Settings entry / choice / switch / info / danger rows 已接入，业务事件和可用态保持不变。

**TideChip Compose 定义**：`core.presentation.components.TideChip` 提供 default / selected / disabled states，使用 `radius.full` 胶囊、32dp 最小高度、1dp outline、`footnote1` 文本；`AppChip` 作为兼容入口已委托到该组件，Search Page 的 recent/trending suggestion chips、Browse genre chips、Lyrics metadata chips 已接入。

**TideChipSection Compose 定义**：`core.presentation.components.TideChipSection` 提供 `TideSectionHeader + FlowRow + TideChip` 的 reusable chip group，统一 10dp 标题/内容间距、8dp chip wrap 间距，支持 metadata count、accent metadata 与可选 trailing action；Search Page 的 Recent Search / Suggestions / Trending 区块和 Browse Genres 区块已接入，保留原 history 清理、suggestion selection 与 genre navigation 行为。

**TideChevron Compose 定义**：`core.presentation.components.TideChevron` 提供 Left / Right disclosure icon，替代页面中用于返回、进入详情、路径分隔的纯文本 `<` / `>`；Settings TopBar、Settings rows、SourceCard trailing indicator 与 Import breadcrumb 已接入，保留原有点击行为与业务事件。

**TideDetailHeaderSurface Compose 定义**：`core.presentation.components.TideDetailHeaderSurface` 提供音乐详情页头部容器，使用 `radius.xl`、`tertiaryContainer → surfaceContainer` 线性渐变、品牌低透明边框与可配置内边距/间距；Album / Artist / Playlist header 已接入，页面内部 artwork、菜单、播放按钮与业务事件保持各自实现。

**TideStatusBadge Compose 定义**：`core.presentation.components.TideStatusBadge` 提供 Neutral / Info / Success / Warning / Error / Accent 六种状态 tone，使用 `radius.full` 胶囊、状态点、低透明底色与 1dp 边框；SourceCard status、Downloads task status 与 Search remote failure count 已接入。

**TideIconBadge Compose 定义**：`core.presentation.components.TideIconBadge` 提供 44dp、`radius.md` 的 icon / marker badge，支持 `Neutral`（surfaceContainerHigh + secondary content）、`Surface`（tertiaryContainer + primary content）、accent surface marker（support color 低透明底色 + 同色 marker）与 `Brand`（TidePink → TidePurple + white content）视觉形态；Downloads task row 图标、Search status card marker / loading 容器、Search category marker、Search track marker、Library track marker 已接入。

**TideStatusCard Compose 定义**：`core.presentation.components.TideStatusCard` 提供 loading / error / empty 状态面板，默认使用 `radius.lg`、`surfaceContainer + outline`、240dp 最小高度、24dp padding 与居中 title/message/action 布局，并支持配置圆角、surface / outline alpha、内容间距与 message 行数以覆盖半透明 lyrics surface 等变体；Album、Artist、Browse、GenreTracks、Lyrics、Queue、Radio、Recently Added、Recently Played 的状态面板已接入，保留原 loading、retry / refresh 与空状态行为。

**TideDialog Compose 定义**：`core.presentation.components.TideDialog` 提供居中 overlay 容器，使用 `radius.lg`、`surfaceContainer`、1dp outline、20dp 内边距与 280–520dp 宽度约束；`AppDialog` 作为兼容入口已委托到该组件，Settings confirm/input、WebDAV account、scan failure dialogs，Playlist create/edit dialogs，sleep timer modal，以及 `core.presentation.components.ConfirmDialog` 已接入，保持原有 dismiss / confirm 行为不变；业务层不再直接使用平台 `androidx.compose.ui.window.Dialog`。

**TideBottomSheet Compose 定义**：`core.presentation.components.TideBottomSheet` 作为 Miuix `WindowBottomSheet` 的 Design System canonical wrapper，统一 show/title/dismiss/content API；`AppBottomSheet` 兼容入口已委托到该组件，后续业务 bottom sheet 不再直接依赖 Miuix window API。

**Feedback / Loading Compose 定义**：`core.presentation.components.FeedbackComponents` 提供 `TideEmptyState` / `TideToast` / `SkeletonBlock`；`TideEmptyState` 基于 `TideCardSurface` 统一空状态卡片 shell，并支持 marker 或 icon visual；`core.presentation.components.TideLoadingIndicator` 提供统一 circular loading，默认 32dp / 3dp stroke / Primary 色，支持 size、strokeWidth 与 color override；`core.presentation.components.TideLinearProgressIndicator` 提供统一 linear progress，默认 Primary 色并保留 Miuix progress 行为。当前 App UI 落点：Library 空状态与 Downloads 空状态复用 `TideEmptyState`，全局 `ToastFrame` 复用 `TideToast`，`MediaSkeleton` 复用 `SkeletonBlock`，Album / Artist / Browse / Genre / Lyrics / Radio / Recently Added / Recently Played 状态卡与 Search loading 复用 `TideLoadingIndicator`，Downloads task progress 复用 `TideLinearProgressIndicator`。

#### 11.3.4 Player（播放器组件）

| 组件 | Figma Make | v3/v4 | 状态 |
|---|---|---|---|
| **MiniPlayer** | frosted glass, spring animation | ✓ 已实现 | ✓ |
| **FullPlayer** | blur bg, lyrics/queue/EQ tabs | ✓ Now Playing Modal | ✓ |

### 11.4 App Pages

| Page | Figma Make 描述 | v3/v4 实现 | 状态 |
|---|---|---|---|
| **Home** | rotating hero banner, horizontal scroll sections (albums/artists/playlists/recent) | ✓ Good Evening + 5 列网格 | ✓ |
| **Search** | live filter, category grid, trending songs | ✓ 搜索栏 + 类目卡 + 趋势 | ✓ |
| **Library** | tabbed: Songs, Albums, Artists, Playlists, Folders, Sources | ✓ Songs + Folders 可交互，其他分类占位 | ✓ Folders Tab 已落地 Presentation 入口 |
| **Settings** | all settings groups with live controls (switches, sliders) | ✓ MIUI 5 分组 | ✓ |

**状态**：v5 Library Page 已补 **Folders Tab**，选中后展示目录浏览空状态和 `LibraryFolder` 导入入口；目录树数据源接入仍依赖后续 LibraryState / repository 扩展。

**Onboarding UI 对齐**：`feature.onboarding.presentation.OnboardingScreen` 的中央面板已接入 `TideCardSurface`，主操作接入 `TideButton.Primary`，Skip / Back 接入 `TideTextButton.Default`；保留原 Welcome / Add Sources / Ready 三步流程与导航事件。

### 11.5 Design System Docs

| Section | Figma Make 内容 | v3/v4 状态 |
|---|---|---|
| **Foundation** | color swatches, gradient pairs, type scale, spacing blocks, radius demo, elevation levels | ✓ 已有，但可补充 **elevation levels** |
| **Components** | every component showcased with all variants in a clean grid | ✓ Frame 5:2 已展示主要组件 |
| **Patterns** | Mini Player, Nav Bar, Nav Rail, Top App Bar, device layout cards | ✓ 4 状态 + 7 设备 |

### 11.6 Layout Behavior（**已对齐** ✓）

| 模式 | Figma Make 描述 | v4 实现 | 状态 |
|---|---|---|---|
| **Desktop (lg+)** | 240px sidebar with App + DS navigation sections | `SidebarBar`（240dp / 260dp XL，tokenized 外壳 + brand pill） | ✓ |
| **Desktop (md-exp)** | Navigation rail with icon + label items | `NavigationRailBar`（72dp / 80dp，tokenized 外壳 + brand pill） | ✓ |
| **Mobile** | Bottom navigation bar with active indicator animation | `BottomBar`（胶囊 active indicator + animateDpAsState） | ✓ |
| **Dark mode toggle** | in sidebar / top bar | `TideTunesTheme` 三档 System/Light/Dark | ✓ |

**实现**：`shared.widgets.appbar.BottomBar` 使用独立的胶囊背景作为 active indicator，通过 `animateDpAsState` 按 `HomeTab.index` 在四个底部 Tab 间平移；`NavigationRailBar` / `SidebarBar` 使用 tokenized `surfaceContainer` + `outline` 外壳与品牌低透明渐变 active pill。动画时长复用 `TideTunesTokens.motion.standardMillis`，保持 Home / Search / Library / Settings 的 IA 不变。

### 11.7 v5 Frame 5:853 当前状态

**已创建**：
- 5:853 — Frame 容器（3400×5800，x:0, y:9700，深紫灰底色 #F7F7FA）
- 5:854 — Header 区域（深色 #14101F，320 高）
- 5:855 — Badge "TideTunes DS v5 · Figma Make Alignment"

**待填充内容**（按优先级）：
1. **Foundation 卡片** — 颜色 swatches + 8 渐变对 + 字体栈（Plus Jakarta Sans / JetBrains Mono）
2. **组件库 3×4 网格** — TideButton (5 variants) / TideSwitch / TideSlider / TideTabs / QualityBadge / AlbumCard / ArtistCard / PlaylistCard / MusicCard / SourceCard / EmptyState / Toast / SkeletonBlock / SettingsItem
3. **App Pages 预览** — Home + Search + Library (6 Tabs) + Settings 缩略图
4. **Layout Behavior 对比** — Desktop 240px sidebar vs Mobile bottom nav + active indicator animation spec
5. **差异点对比表** — Figma Make vs v3/v4 完整对比

### 11.8 行动清单（按优先级）

#### P0 — 立即执行
- [x] 从 Figma Make 截图提取 8 个渐变对的具体 hex 和角度，更新到 PDS 2.3 节
- [x] 升级圆角 token 12/20/28/36 到 v4
- [x] 字体栈添加 Plus Jakarta Sans + JetBrains Mono
- [x] 补 QualityBadge 组件定义（FLAC / Hi-Res / Dolby Atmos 三档）

#### P1 — 本周完成
- [ ] Frame 5:853 填充完整的 Foundation + Components 网格
- [x] 补 TideTopBar 并接入 Settings top bars / AppTopBar 兼容入口
- [x] 补 TideButton 5 variants（primary/secondary/tertiary/ghost/danger）
- [x] 收口 AppButton / AppCard / AppChip / AppDialog 兼容入口到 Tide 组件
- [x] 补 TideIconButton 并接入 TideTunesIconButton / AppIconButton / Form trailing icon / Home header search
- [x] 补 TidePlayerControlButton 并接入 Mini Player / Now Playing 主播放控制
- [x] 补 TideTextButton 并接入 TideTunesTextButton / AppTextButton 兼容入口
- [x] 补 TideActionPill 并接入 Home quick actions
- [x] 补 TideSwitch 并接入 Settings rows
- [x] 补 TideTabs 3 variants（line/pill/segmented）并接入 Library / Create Playlist
- [x] 补 TideFab 渐变 FAB 并接入 Import / Playlists
- [x] 补 TideCheckbox 并接入 Import / AppCheckbox
- [x] 补 TideTextField 并接入 Settings / WebDAV / Form
- [x] 补 TideSearchBar 公共组件并接入 Search Page
- [x] 补 TideChip 公共组件并接入 Search suggestions / Browse genres / Lyrics metadata
- [x] 补 TideChipSection 并接入 Search recent / trending suggestions / Browse genres
- [x] 补 TideChevron 公共组件并替换 Settings / SourceCard / Import breadcrumb 文本箭头
- [x] 补 TideStatusBadge 公共组件并接入 SourceCard / Downloads status / Search failure count
- [x] 补 TideStatusCard 并接入 Album / Artist / Browse / Genre / Lyrics / Queue / Radio / Recently Added / Recently Played 状态面板
- [x] 补 TideIconBadge 公共组件并接入 Downloads / Search marker / Search category grid / Library track marker
- [x] 补 TideCardSurface 并接入 Downloads task cards / Source Manager cards / Search cards / Browse music cards / Playlist cards / Library track cards / Home recommended tracks / Album & Artist detail cards / Import & Source Editor panels
- [x] 补 TideTrackNumberBadge 公共组件并接入 Album / Artist / Playlist / Queue track rows
- [x] 补 TideMusicArtworkTile 并接入 Home 推荐曲目占位封面
- [x] 补 TideTrackListRow 并接入 Genre / Radio / Recently Added / Recently Played track rows
- [x] 补 TidePageHeader 并接入 Browse / Downloads / Genre / Library / Queue / Radio / Search / Recently Added / Recently Played
- [x] 补 TideSectionHeader 并接入 Album / Artist / Browse / Home / Playlist sections
- [x] 补 TideDetailHeaderSurface 并接入 Album / Artist / Playlist detail header
- [x] 补 TideSettingsGroup / TidePreferenceRow 并接入 Settings sections / rows
- [x] 补 TideDialog 公共组件并接入业务 Dialog
- [x] 补 TideBottomSheet 公共组件并接入 AppBottomSheet 兼容入口
- [x] 补 EmptyState / Toast / SkeletonBlock 三个缺失组件
- [x] 补 TideLoadingIndicator 并接入状态卡 / Search loading
- [x] 补 TideLinearProgressIndicator 并接入 Downloads task progress
- [x] 补 TideSlider 公共组件并接入 Now Playing 进度条
- [x] Library Page 添加 Folders Tab（Presentation 入口）
- [x] SourceCard 补 Status / Music Count / Storage / TideChip action strip（Sync / Logs / Settings）展示结构

#### P2 — 下迭代
- [x] active indicator animation transition spec
- [ ] Side-by-side 对比 Frame（Figma Make vs Ardot v4）
- [ ] 自动 dark/light mode 切换脚本（基于 CSS custom properties）

### 11.9 抓取与解析技术记录

**Figma Make 访问限制**：
- REST API：`/v1/files/{key}` 返回 `400: File type not supported by this endpoint` (editorType: "make")
- REST API：`/v1/images/{key}`（无 ids 参数）返回 2 个节点 ID 的 PNG 签名 URL
- Web 端：完整内容仅对登录用户可见，Playwright headless 被 `ERR_CONNECTION_CLOSED` 拦截 WASM
- 公开 share token `?t=...` 在 headless 下仍被识别为 bot

**抓取策略**：
1. Playwright Chromium headless 抓取 Figma 文档侧栏 + Home 视图主区域
2. `document.body.innerText` 提取 AI 生成的元数据描述（包含完整组件清单）
3. PNG 节点导出 + 后续图片分析识别具体色值和组件规格

**待改进**：
- 用户登录态 cookies 注入（可访问完整 6 个 section）
- 直接拿 share link（带 s= 公参）可让 Playwright 通过 Figma 内部 iframe 访问

---

**v5 状态**：Frame 5:853 已完成构建，共 6 个 Section / ~300 个节点：

| Section | 节点 ID | 内容 |
|---|---|---|
| Header | 5:854 | Badge + Title + Subtitle + Description |
| 01 Brand Tokens | 7:4 | 8 色块 + 8 渐变对 + 4 圆角规格 |
| 02 App Pages | 7:40 | Home / Search / Library (14 tabs) / Settings (8 groups) 缩略图 |
| 03 Cover Page | 8:47 | 渐变 hero + Logo + Tagline + 9 原则 chips + CTA |
| 04 Components | 8:75 | 5 TideButton 变体 + Switch/Slider/Badge + 5 Music Cards + 7 Layout 组件 + MiniPlayer + FullPlayer |
| 05 Layout Behavior | 8:219 | Desktop 3-panel (Toolbar+Sidebar+Content+RightPanel) + Mobile bottom nav + 5 Breakpoints |
| 06 Compose Mapping | 8:295 | 14 组件映射 (Figma → @Composable) + 4 Motion 映射 (Spring/Tween/Keyframes/Repeatable) |
