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
| `color.brand.gradient` | 品牌渐变 | `linear-gradient(135°, #FF5B8A → #7A6CFF)` | 播放键、进度条、沉浸背景 |
| `color.brand.gradientSoft` | 柔和渐变 | `linear-gradient(135°, #FF5B8A → #7A6CFF)` @ 40% | 卡片悬浮、徽章 |
| `color.support.blue` | 辅助蓝 | `#3B82F6` | 信息提示、链接 |
| `color.support.orange` | 辅助橙 | `#F59E0B` | 警告、Hi-Res 标识 |
| `color.support.green` | 辅助绿 | `#10B981` | 成功、下载完成 |

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
| Android | Roboto / MiSans | Noto Sans SC |
| iOS | SF Pro Display / SF Pro Text | PingFang SC |
| Desktop (macOS) | SF Pro | PingFang SC |
| Desktop (Windows) | Segoe UI | Microsoft YaHei |

> 不打包自定义字体，保证原生质感与零额外体积。HyperOS 设备自动使用 MiSans。

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

### 02.3 Adaptive Width Tokens

| Token | Compact | Medium | Expanded | Large | XL |
|---|---|---|---|---|---|
| `width.content` | fill - 32dp | fill - 40dp | 600dp | 800dp | 1000dp |
| `width.sidebar` | 0 (hidden) | 0 (hidden) | 240dp | 260dp | 280dp |
| `width.detailPanel` | 0 (full screen) | 0 (full screen) | 360dp | 400dp | 480dp |
| `width.miniPlayer` | fill - 32dp | fill - 40dp | fill - 280dp | 480dp | 560dp |
| `width.card.min` | 140dp | 140dp | 160dp | 180dp | 200dp |
| `width.card.max` | 180dp | 200dp | 220dp | 240dp | 280dp |

### 02.4 Touch Target

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
| Background | `linear-gradient(135°, #FF5B8A → #7A6CFF)` |
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
| Type | Segmented (underline) / Pill |
| Underline | 3dp, brand gradient |
| Active | `text.title3` brand color |
| Inactive | `text.title3` `text.tertiary` |
| Animation | 180ms underline slide |

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
| Search Bar | `SearchBar` | `SearchBar` | Input |
| Lyrics Panel | Custom Layout | `LyricsLayout`（自定义） | Player |
| Queue Panel | `LazyColumn` | `QueueList`（自定义） | Player |
| Adaptive Grid | `LazyVerticalGrid` | `AdaptiveGrid`（自定义） | Layout |

#### 扩展映射（compose-miuix-ui 原生组件）

| Figma 组件 | Compose Multiplatform | compose-miuix-ui | 类别 |
|---|---|---|---|
| Sidebar | `NavigationDrawer` | `NavigationDrawer`（自定义） | Navigation |
| Switch | `Switch` | `SuperSwitch` | Settings |
| Slider | `Slider` | `SuperSlider` | Settings |
| Dialog | `AlertDialog` | `SuperDialog` | Feedback |
| Dropdown | `DropdownMenu` | `SuperDropdown` | Settings |
| Checkbox | `Checkbox` | `SuperCheckbox` | Settings |
| Navigation | `NavController` | `Navigator` | Navigation |
| FAB | `FloatingActionButton` | `FloatingActionButton`（自定义渐变） | Action |
| Elevated Card | `ElevatedCard` | `FloatingCard` | Surface |
| Now Playing | Custom `NowPlayingScreen` | `NowPlayingScreen`（自定义） | Player |
| Track Row | Custom `TrackRow` | `TrackRow`（自定义） | Content |
| Bottom Sheet | `ModalBottomSheet` | `SuperBottomSheet`（自定义） | Overlay |
| Tab Row | `TabRow` | `TabRow`（自定义） | Navigation |
| Chip | `FilterChip` | `FilterChip`（自定义） | Input |
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
  Gradient: linear(135°, #FF5B8A → #7A6CFF)
  Support: Blue(#3B82F6), Orange(#F59E0B), Green(#10B981)
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
| **渲染目标** | Web（Plus Jakarta Sans + JetBrains Mono + theme.css） | v4 采用 Inter + Plus Jakarta Sans 可考虑 |

### 11.2 Figma Make 设计令牌

#### 11.2.1 品牌色（**完全一致** ✓）

| Token | Hex | 与 v3/v4 对比 |
|---|---|---|
| **TidePink** | `#FF5B8A` | ✓ v3/v4 一致 |
| **TidePurple** | `#7A6CFF` | ✓ v3/v4 一致 |
| Orange | — | ✓ v4 已加 |
| Green | — | ✓ v4 已加 |
| Blue | — | ✓ v4 已加 |
| Yellow | — | ✓ v4 已加 |

**结论**：品牌色与 v3/v4 100% 对齐，无需调整。

#### 11.2.2 渐变对（8 个）

Figma Make 声明使用 8 个渐变对，组合方式：`brand + support color`。需要从实际原型中提取具体角度和色阶。当前 v4 已有：
- 135° 线性渐变（brand → support）
- 180° 线性渐变（深色版）
- 径向渐变（高光强调）

**待办**：从 Figma Make 截图提取具体 8 对的 hex + 角度，合并到 v4 Frame 5:2 的 Gradient 系统。

#### 11.2.3 圆角（**有差异** ⚠️）

| 等级 | Figma Make | v3/v4 | 决策建议 |
|---|---|---|---|
| sm | 12 | 8 | **统一为 12**（与 Figma Make 对齐） |
| md | 20 | 16 | **统一为 20** |
| lg | 28 | 24 | **统一为 28** |
| xl | 36 | 32 | **统一为 36** |

**行动**：在 v4 PDS 中将 `--radius-*` 提升 4px，与 Figma Make 保持一致。

#### 11.2.4 字体（**新发现**）

| 用途 | Figma Make | v3/v4 | 决策建议 |
|---|---|---|---|
| Display/标题 | **Plus Jakarta Sans** | Inter | 考虑升级为 Plus Jakarta Sans |
| Body | — | Inter | 保持 Inter |
| Badges/数据 | **JetBrains Mono** | Inter | 升级为 JetBrains Mono |

**行动**：在 v5 中添加 Plus Jakarta Sans + JetBrains Mono 到字体栈，Inter 作为 fallback。

#### 11.2.5 暗色/亮色模式

Figma Make 通过 CSS 自定义属性实现完整 dark/light 切换，与 v4 的 token 系统一致。**当前差异**：v4 仅定义了 token 层级，**未实现自动切换**；Figma Make 的实现可参考。

### 11.3 组件库对齐

#### 11.3.1 Primitives（基础组件）

| 组件 | Figma Make | v3/v4 | 差异 |
|---|---|---|---|
| **TideButton** | 5 variants (primary/secondary/tertiary/ghost/danger) | TideButton 4 variants | **补 1 variant** (danger) |
| **TideSwitch** | ✓ | SuperSwitch | ✓ 概念一致 |
| **TideSlider** | ✓ | — | **v5 新增** |
| **TideTabs** | 3 variants (line/pill/segmented) | Miuix Tabs | ✓ 可借鉴 3 种样式 |
| **QualityBadge** | ✓ (FLAC/Hi-Res/Atmos) | — | **v5 新增**（音频质量标识） |

#### 11.3.2 Music Cards（音乐卡片）

| 卡片 | Figma Make | v3/v4 | 状态 |
|---|---|---|---|
| **AlbumCard** | ✓ | ✓ | ✓ 已对齐 |
| **ArtistCard** | ✓ | ✓ | ✓ 已对齐 |
| **PlaylistCard** | ✓ | ✓ | ✓ 已对齐 |
| **MusicCard** (single track) | ✓ | ✓ | ✓ 已对齐 |
| **SourceCard** | ✓ | ✓ | ✓ 已对齐 |

#### 11.3.3 Layout（布局组件）

| 组件 | Figma Make | v3/v4 | 状态 |
|---|---|---|---|
| SectionHeader | ✓ | ✓ | ✓ |
| SettingsSection | ✓ | ✓ (分组卡片) | ✓ |
| SettingsItem | ✓ | ✓ (SuperArrow/SuperSwitch) | ✓ |
| ListItem | ✓ | ✓ | ✓ |
| **EmptyState** | ✓ | ✗ | **v5 新增** |
| **Toast** | ✓ | ✗ | **v5 新增** |
| **SkeletonBlock** | ✓ | ✗ | **v5 新增** |

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
| **Library** | tabbed: Songs, Albums, Artists, Playlists, Folders, Sources | ✓ 5 个 Tab | ⚠️ 缺 **Folders** Tab |
| **Settings** | all settings groups with live controls (switches, sliders) | ✓ MIUI 5 分组 | ✓ |

**行动**：在 v5 Library Page 中补 **Folders Tab**（按目录浏览本地音乐文件树）。

### 11.5 Design System Docs

| Section | Figma Make 内容 | v3/v4 状态 |
|---|---|---|
| **Foundation** | color swatches, gradient pairs, type scale, spacing blocks, radius demo, elevation levels | ✓ 已有，但可补充 **elevation levels** |
| **Components** | every component showcased with all variants in a clean grid | ✓ Frame 5:2 已展示主要组件 |
| **Patterns** | Mini Player, Nav Bar, Nav Rail, Top App Bar, device layout cards | ✓ 4 状态 + 7 设备 |

### 11.6 Layout Behavior（**待整合**）

| 模式 | Figma Make 描述 | v4 实现 | 状态 |
|---|---|---|---|
| **Desktop (lg+)** | 240px sidebar with App + DS navigation sections | ✓ Desktop mockup (5:489) | ✓ 240px 侧栏 |
| **Mobile** | Bottom navigation bar with active indicator animation | ✓ Android Phone (5:97), iPhone (5:182) | ✓ 底部 Tab |
| **Dark mode toggle** | in sidebar / top bar | ✓ 在 Frame 2:2 / 5:2 中 | ✓ |

**新增概念**：Figma Make 提到 **active indicator animation**（活动指示器动画），v4 静态显示，v5 应补充 transition spec。

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
- [ ] 从 Figma Make 截图提取 8 个渐变对的具体 hex 和角度，更新到 PDS 2.3 节
- [ ] 升级圆角 token 12/20/28/36 到 v4
- [ ] 字体栈添加 Plus Jakarta Sans + JetBrains Mono
- [ ] 补 QualityBadge 组件定义（FLAC / Hi-Res / Dolby Atmos 三档）

#### P1 — 本周完成
- [ ] Frame 5:853 填充完整的 Foundation + Components 网格
- [ ] 补 EmptyState / Toast / SkeletonBlock 三个缺失组件
- [ ] Library Page 添加 Folders Tab

#### P2 — 下迭代
- [ ] active indicator animation transition spec
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

**v5 状态**：Frame 5:853 已创建容器并放置 Header，下一步填充 Foundation 卡片和组件网格。

