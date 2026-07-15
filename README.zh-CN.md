# TideTunes

[English](./README.md) · [简体中文](./README.zh-CN.md)

TideTunes 是一款使用 Kotlin Multiplatform、Compose Multiplatform、Rust 和 UniFFI 构建的本地优先、自托管音乐播放器。项目面向 Android、iOS 和 Desktop 提供统一的音乐库，同时通过清晰的音源边界隔离临时播放资源、账号凭据和 Provider 特有数据。

> [!IMPORTANT]
> TideTunes 仍在积极开发中。当前应用版本为 `0.3.0`；在稳定版本发布前，用户界面、数据库迁移和扩展 API 仍可能继续调整。

## 项目亮点

- 使用同一套 Kotlin 与 Compose 代码支持 **Android、iOS 和 Desktop**。
- 支持 **本地和 WebDAV 音源**，具备目录浏览、索引搜索、在线播放和下载能力。
- 使用 **Room KMP 统一曲库** 管理曲目、专辑、艺术家、流派、封面、歌词、播放列表、下载和同步状态。
- 提供 **自适应界面**：手机使用底部导航，中等窗口使用导航栏，大屏和桌面使用侧边栏布局。
- 使用统一播放抽象，并分别接入 Android Media3、iOS AVPlayer 和 Desktop Rust/rodio 播放引擎。
- 支持跨平台离线下载：Android WorkManager、iOS 后台 URLSession、Desktop 协程调度器。
- WebDAV 支持 Fast、Standard、Full 三种可选元数据扫描模式。
- 支持兼容 Lyrico Plugin API v1-v3 的 JavaScript 元数据插件，并在隔离的 QuickJS Runtime 中运行。
- Rust 后端负责远端存储、元数据解析、插件执行、桌面播放支持和 UniFFI 绑定。

## 当前功能

### 音乐库与浏览

- 首页、搜索、音乐库、设置四个一级入口。
- 支持曲目、专辑、艺术家、流派、播放列表、最近添加、最近播放、音乐电台、播放队列、歌词和正在播放页面。
- 基于 Room FTS 的本地曲库全文搜索，以及按音源账号索引的 Provider 搜索。
- 同一规范化曲目可以关联多个可播放来源文件。
- 播放列表持久化和稳定排序。
- 支持内嵌歌词、外挂歌词、封面元数据和原始音频标签。
- 支持手机、平板、大屏和桌面窗口的响应式导航布局。

### 音源支持

| 音源 | 浏览 | 搜索 | 播放 | 下载 | 增量同步 |
| --- | :---: | :---: | :---: | :---: | :---: |
| 本地 | 支持 | 支持 | 支持 | 支持 | 暂不支持 |
| WebDAV | 支持 | 支持 | 支持 | 支持 | 暂不支持 |

音源适配器负责鉴权、浏览、搜索和解析播放资源，不会直接写入规范化音乐表。

### WebDAV 元数据扫描模式

| 模式 | 行为 |
| --- | --- |
| **Fast** | 读取核心标签和音频属性，跳过内嵌封面、歌词和原始标签。 |
| **Standard** | 读取核心标签、音频属性和内嵌歌词，跳过封面和原始标签。新安装默认使用此模式。 |
| **Full** | 读取核心标签、音频属性、封面、歌词和原始元数据。 |

被跳过的可选元数据不会被删除。用户可以之后在设置中补全缺失封面或歌词，无需伪造远端文件变更，也不要求文件指纹发生变化。

### 播放与下载

- 统一的播放状态、播放进度、队列、播放模式和正在播放展示契约。
- 播放 URL、请求头、Cookie 和短期 Token 只在实际播放前解析，不写入 Room。
- Android 使用 Media3 和 MediaSession。
- iOS 使用 AVPlayer 播放引擎适配器。
- Desktop 使用 Rust/rodio 播放后端。
- 下载任务持久化，支持暂停、继续、重试、取消和进度更新。
- 平台下载调度器：
  - Android：WorkManager
  - iOS：后台 URLSession
  - Desktop：基于协程的调度器

### 兼容 Lyrico 的元数据插件

TideTunes 支持用户从本地导入实现 Lyrico Plugin API v1-v3 `MetaSource` 行为的 ZIP 插件。插件用于扩展歌曲元数据、封面和歌词查询，不会被当作通用播放 `MusicSource` 使用。

当前插件链路如下：

```text
插件 ZIP
  -> 校验与受限解压
  -> 基于 Room 的安装、配置和持久化
  -> 可观察的 MetaSource 注册表
  -> 延迟创建的独立 QuickJS Worker
  -> searchSongs / getLyrics / searchCovers
  -> 统一的 TideTunes 元数据结果
```

已经实现的插件能力包括：

- ZIP 导入、manifest 校验、更新、启用/禁用、配置、清理缓存和卸载。
- Lyrico v3 官方配置字段类型和条件显示。
- 手动、自动和批量查询权限。
- 结构化歌词、翻译歌词、罗马音歌词和多种原始歌词格式。
- 兼容真实 Lyrico 插件常见的歌曲、封面结果包装和字段别名。
- 每个插件独立 Runtime、内存/栈限制、超时、取消和中毒 Runtime 重建。
- 提供 HTTP、缓存、加密、Base64、字节、压缩、XML、日志、应用和运行时信息 Host API。
- HTTP 重定向和私有网络校验、响应大小限制，以及敏感日志过滤。

TideTunes 不内置或自动下载第三方插件 ZIP，插件文件由用户自行提供。详细兼容性和安全模型请参阅[插件运行时文档](./docs/plugin-runtime.md)。

## 架构

```mermaid
flowchart TD
    A[Android App] --> S[shared 应用装配层]
    I[iOS App] --> S
    D[Desktop App] --> S

    S --> F[feature 功能模块]
    S --> V[service 服务模块]
    S --> M[source 音源模块]
    S --> C[core 核心模块]
    S --> R[Room KMP / DataStore / Koin]
    S --> U[UniFFI 桥接]
    U --> X[Rust Workspace]

    M --> C
    V --> C
    F --> C
```

### 架构原则

1. **一个面向 UI 的统一数据库**  
   Android、iOS 和 Desktop 使用同一套 Room KMP Schema，并统一使用 bundled SQLite。

2. **规范化曲库与 Provider 无关**  
   曲目、专辑、艺术家、流派、歌词、封面、播放列表和下载记录不归属于 WebDAV 或其他特定 Provider。

3. **音源身份单独保存**  
   音源账号、曲库根目录、来源对象、同步游标、Provider 扩展属性和曲目来源引用单独建模，避免污染规范化音乐实体。

4. **临时播放资源不属于曲目元数据**  
   签名 URL、HTTP 请求头、Token、Cookie 和临时回环地址在播放时动态解析，不作为曲目字段持久化。

5. **功能模块依赖契约，而不是平台播放引擎**  
   commonMain 仅使用播放、下载、同步、音源和 Repository 接口。Media3、AVPlayer、rodio、Room 和 UniFFI 均保留在平台层或数据边界。

6. **元数据插件不是播放音源**  
   JavaScript 插件通过 `MetaSource` 提供元数据查询；本地和 WebDAV 通过 `MusicSource` 提供浏览和播放。

详细文档：

- [架构报告](./docs/architecture/final-architecture.md)
- [Room KMP 数据库结构](./docs/database/schema.md)
- [插件运行时](./docs/plugin-runtime.md)
- [测试报告](./docs/testing/test-report.md)

## 仓库结构

```text
TideTunes/
├── androidApp/                  Android 应用入口
├── desktopApp/                  Desktop JVM 应用入口
├── iosApp/                      SwiftUI 容器与 Xcode 工程
├── shared/                      应用装配、导航、DI、Room、数据层和平台 actual
├── core/
│   ├── domain/                  纯领域模型和 Repository 契约
│   ├── presentation/            共享设计系统和展示层工具
│   ├── lyrics-core/             共享歌词模型与处理逻辑
│   └── lyrics-ui/               共享歌词 UI
├── source/
│   ├── api/                     MusicSource 契约和注册表
│   ├── local/                   本地音源适配器
│   └── webdav/                  WebDAV 音源适配器
├── service/
│   ├── playback/domain/         播放引擎、控制器和队列契约
│   ├── playback/presentation/   正在播放和播放 UI 状态
│   ├── download/domain/         下载契约与 UseCase
│   ├── download/data/           持久化下载实现
│   ├── librarysync/domain/      曲库同步契约
│   └── librarysync/data/        同步持久化与协调逻辑
├── feature/                     首页、曲库、搜索、设置、音源、播放列表等功能
├── rust-libs/
│   ├── backend/                 面向 UniFFI 的后端门面
│   ├── async-runtime/           Rust 异步运行时支持
│   ├── storage-backend/         远端存储和扫描
│   ├── audio-metadata/          音频元数据提取
│   ├── plugin-runtime/          QuickJS 插件 Host
│   ├── order-key/               稳定排序键
│   └── uniffi-bindgen/          UniFFI 绑定生成辅助工具
├── build-logic/convention/      Gradle Convention Plugin
├── docs/                        架构、数据库、运行时和测试文档
├── Design/                      UI 设计参考与生成的设计资源
└── gradle/libs.versions.toml    依赖和插件版本目录
```

当前 Gradle 工程包含 Home、Search、Downloads、Settings、Playlist、Sources、Importing、Onboarding、Queue、Radio、Lyrics、Album、Artist、Browse、Library、Recently Added 和 Recently Played 等独立功能模块。

## 技术栈

| 范围 | 技术 |
| --- | --- |
| 共享语言 | Kotlin 2.4、Kotlin Multiplatform |
| UI | Compose Multiplatform、JetBrains Navigation Compose、Miuix |
| 依赖注入 | Koin |
| 数据持久化 | Room KMP、bundled SQLite、DataStore |
| 并发与序列化 | Coroutines、kotlinx.serialization、kotlinx.datetime |
| Android 播放 | AndroidX Media3 / MediaSession |
| iOS 宿主 | SwiftUI、UIKit Bridge、AVPlayer 播放适配器 |
| Desktop | Compose Desktop、JVM 21、Rust/rodio 播放 |
| Native 后端 | Rust、UniFFI、Gobley Gradle 集成 |
| 插件 | 基于 QuickJS 的 JavaScript Runtime、Lyrico Plugin API v1-v3 |
| CI | GitHub Actions、Gradle、Cargo |

## 开发环境要求

### 通用环境

- Git
- JDK 21
- Rust stable 工具链和 Cargo
- 较新的 Android Studio 或支持 Kotlin Multiplatform 的 IntelliJ IDEA

### Android

- Android SDK Platform 37 和兼容的 Build Tools
- 支持 Rust Android Target 的 Android NDK；当前 CI 使用 NDK `r28-beta2`
- 安装 Rust Android Target：

```bash
rustup target add aarch64-linux-android x86_64-linux-android
cargo install --locked cargo-ndk@3.5.4
```

Android 应用使用 `minSdk 29`、`targetSdk 34` 和 `compileSdk 37`。当前打包应用面向 `arm64-v8a`，共享 Native 构建还包含用于开发和测试的 `x86_64`。

### iOS

- macOS 和 Xcode
- iOS 16.0 或更高版本
- Apple Silicon，或 arm64 iOS Simulator 目标

Gradle 工程只定义了 `iosArm64` 和 `iosSimulatorArm64`，未配置 x86_64 Simulator Target。

### Linux Desktop

构建 Desktop 目标前需要安装 ALSA 开发头文件和 `pkg-config`：

```bash
sudo apt-get update
sudo apt-get install --yes libasound2-dev pkg-config
```

## 从源码构建

克隆仓库：

```bash
git clone https://github.com/JulyStar-Lv/TideTunes.git
cd TideTunes
```

### Android

构建 Debug APK：

```bash
./gradlew :androidApp:assembleDebug
```

APK 输出目录为 `androidApp/build/outputs/apk/`。

Release 构建需要配置 `androidApp/key.properties` 和有效的签名密钥。请勿提交签名凭据。

### Desktop

运行 Desktop 应用：

```bash
./gradlew :desktopApp:run
```

编译 Desktop 并执行共享 Desktop 测试：

```bash
./gradlew :desktopApp:compileKotlinDesktop :shared:desktopTest
```

为当前操作系统生成安装包：

```bash
./gradlew :desktopApp:packageDistributionForCurrentOS
```

Compose Desktop 已配置 DMG、MSI 和 DEB 输出格式。

### iOS

打开 Xcode 工程：

```bash
open iosApp/TideTunes.xcodeproj
```

选择 `TideTunes` Scheme 和 arm64 Simulator 或真机。Xcode Build Phase 会自动调用：

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

也可以通过命令行构建 Simulator：

```bash
xcodebuild \
  -project iosApp/TideTunes.xcodeproj \
  -scheme TideTunes \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  ARCHS=arm64 \
  ONLY_ACTIVE_ARCH=YES \
  CODE_SIGNING_ALLOWED=NO \
  build
```

### Rust Workspace

格式化、静态检查并测试 Rust Workspace：

```bash
cargo fmt --manifest-path rust-libs/Cargo.toml --all -- --check
cargo clippy --manifest-path rust-libs/Cargo.toml --workspace --all-targets -- -D warnings
cargo test --manifest-path rust-libs/Cargo.toml --workspace
```

## 测试与 CI

仓库中的 `Build validation` GitHub Actions Workflow 会在推送到 `main` 或向 `main` 创建 Pull Request 时运行。

当前 CI 检查包括：

- 使用 JDK 21、Android SDK 37、NDK 和 Rust Android Target 编译 Android Debug APK。
- 编译 Desktop Kotlin 目标并执行共享 Desktop 测试。
- Rust 格式化、Clippy、Workspace 单元测试和插件 Runtime 专项验证记录在测试报告中。
- Android、Desktop 和 iOS Simulator 的共享代码跨平台编译检查。

常用本地命令：

```bash
# 全仓库 Gradle 测试
./gradlew test

# Shared Desktop 测试
./gradlew :shared:desktopTest

# Android Shared 单元测试
./gradlew :shared:testDebugUnitTest

# iOS Simulator Shared 测试
./gradlew :shared:iosSimulatorArm64Test

# 跨平台编译检查
./gradlew \
  :shared:compileDebugKotlinAndroid \
  :desktopApp:compileKotlinDesktop \
  :shared:compileKotlinIosSimulatorArm64
```

部分 WebDAV Live Test 需要运行时提供账号凭据，任何 Secret 都不得提交到仓库。

## 开发约定

- 纯领域模型不得依赖 Compose、Room、Media3、AVFoundation、rodio 或 UniFFI 类型。
- Provider 特有字段应保存到来源实体或来源对象扩展属性，不要直接加入规范化 `track`。
- 短期有效的播放资源必须在播放边界动态解析。
- 功能 UI 优先使用不可变 State 和明确的 Action/Event 契约。
- 每次 Room Schema 变更都必须提供 Migration，并更新导出的 Schema。
- 禁止提交 WebDAV 密码、OAuth Token、插件 Secret、签名文件或第三方插件 ZIP。
- 提交 Pull Request 前应执行相关 Gradle 和 Cargo 检查。

## 当前限制

- 项目仍处于稳定版之前，开发版本之间不保证所有行为完全兼容。
- iOS Simulator 当前仅支持 arm64。
- 第三方 Lyrico 插件 ZIP 由用户自行提供，TideTunes 不负责分发。
- 配置的 include 目录会在构建 Bundle 时按确定顺序合并，运行时 `include(path)` 被有意禁用，插件不能任意读取本地文件。
- Android 正常生产进程退出依赖操作系统回收进程资源。
- Android Lint 当前存在已记录的仓库/工具链兼容问题，构建和单元测试仍是主要验证门禁。

## 路线图

近期工作重点包括：

- 继续提高真实 Lyrico 插件兼容性和插件诊断能力。
- 优化大曲库导入、增量同步、后台扫描和元数据补全性能。
- 持续改进自适应 UI/UX、无障碍能力和桌面交互。
- 扩展更多音源 Provider，并增强各 Provider 的同步能力。
- 完善安装包构建、自动发布和面向最终用户的使用文档。

以上路线图仅表示当前方向，可能随着架构和平台支持成熟度调整。

## 参与贡献

欢迎提交 Issue 和 Pull Request。提交修改前请确认：

1. 遵守现有模块和依赖边界。
2. 为行为变更新增或更新测试。
3. 执行相关 Gradle 和 Cargo 检查。
4. 同步记录数据库、插件 API、音源契约或平台要求的变化。
5. 不包含任何私密凭据、受版权保护的插件包或个人音乐库数据。

## 许可证

TideTunes 的大部分代码使用 [GNU General Public License v3.0](./LICENSE.md) 许可证。

[`tidetunes-order-key`](./rust-libs/order-key) Crate 可在 Apache License 2.0 或 MIT License 二选一的条款下使用。
