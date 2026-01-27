# Local Code Switcher Plugin

一个 IntelliJ IDEA 插件，实现 IDEA 和 Qoder IDE（或其他本地 IDE）之间的无缝切换和代码同步功能。

## 功能特性

### 🔄 IDE 灵活切换
- 一键从 IDEA 切换到 Qoder IDE。
- 支持单文件、目录、或整个项目的跨 IDE 打开。
- 智能识别操作系统，支持 Windows、macOS 和 Linux。

### 📝 交互式手动同步 (核心)
- **非侵入式设计**：取消后台实时监控，改为由开发者手动触发同步，避免频繁干扰。
- **跨目录扫描**：支持两个不同本地路径下的同一 Git 项目克隆之间的内容比对。
- **变更审查窗口**：独立的 UI 窗口展示所有差异文件列表。
- **双击即达**：在列表中双击文件，直接进入交互式 Diff 视图。

### 🔍 智能 Diff 与选择性合并
- **内置 Diff 集成**：使用 IntelliJ IDEA 原生 Diff 引擎，操作习惯无缝衔接。
- **手术刀式合并**：支持通过 `>>` 符号选择性接受 AI 的部分修改，或一键全部接受。
- **状态实时跟踪**：只有当文件内容与 Qoder 完全一致后，才会从待处理列表中自动消失。

### ⚙️ 极简配置
- 只需配置 Qoder IDE 路径和 Qoder 项目根目录。
- 零后台开销，仅在点击同步时运行。

## 安装方法

### 方式一：从本地构建安装

1. **构建插件**
```bash
cd local-code-switcher
./gradlew buildPlugin
```

2. **安装插件**
   - 打开 IDEA
   - 进入 `File → Settings → Plugins`
   - 点击齿轮图标 → `Install Plugin from Disk...`
   - 选择 `build/distributions/local-code-switcher-1.0.0.zip`

### 方式二：从源码运行（开发模式）

```bash
./gradlew runIde
```

## 使用指南

### 1. 配置 Qoder IDE 路径

首次使用需要配置 Qoder IDE 的安装路径：

1. 打开 `File → Settings → Tools → Local Code Switcher`
2. 设置 Qoder IDE 路径：
   - **Windows**: `C:\Program Files\Qoder\qoder.exe`
   - **macOS**: `/Applications/Qoder.app`
   - **Linux**: `/usr/bin/qoder`
3. 根据需要调整其他配置项

### 2. 使用插件

#### 方式一：右键菜单
1. 在编辑器中右键点击文件
2. 选择 `Open in Qoder IDE`
3. Qoder IDE 将自动打开该文件

#### 方式二：项目视图
1. 在项目视图中右键点击文件或文件夹
2. 选择 `Open in Qoder IDE`
3. Qoder IDE 将打开选中的文件/文件夹

#### 方式三：工具栏
1. 点击工具栏中的 Qoder 图标
2. 将打开当前编辑的文件或选中的项目

### 3. 代码同步流程 (核心工作流)

1. **在 IDEA 中触发打开**
   - 使用右键菜单 `Open in Qoder IDE`。
   - IDEA 会启动 Qoder 并定位到对应文件。

2. **在 Qoder IDE 中完成 AI 编码**
   - 利用 Qoder 的 AI 能力进行代码生成或重构。
   - **保存文件**。

3. **回到 IDEA 手动同步**
   - 快捷键：`Ctrl + Alt + S` 或 菜单 `Tools → Qoder IDE 同步 → 同步 Qoder 变更`。
   - 插件将后台扫描 Qoder 目录与当前目录的差异。

4. **交互式审查与合并**
   - 在弹出的窗口中，双击感兴趣的文件。
   - 在 Diff 视图中使用 `>>` 合并代码。
   - 关闭 Diff 视图，已同步的文件会自动从列表中移除。

## 配置选项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| Qoder IDE 路径 | Qoder IDE 可执行文件的路径 | 空 |
| Qoder 项目根目录 | Qoder 打开的项目本地路径（与当前项目是同一仓库的克隆） | 空 |
| 保持 IDEA 窗口 | 打开 Qoder 时是否保持 IDEA 窗口 | 是 |

## 技术架构

### 核心组件

1. **QoderSettingsState** - 插件配置持久化。
2. **QoderDirectorySyncService** - 负责跨目录扫描，提取 IDEA 与 Qoder 之间的文件差异。
3. **ChangeReviewDialog** - 核心 UI 组件，管理变更列表、双击交互和同步状态刷新。
4. **Actions** - 手动触发入口。

### 技术栈

- **语言**: Kotlin
- **框架**: IntelliJ Platform SDK (2023.2+)
- **构建工具**: Gradle
- **UI 库**: Swing / IntelliJ JBComponents / Diff API

## 开发指南

### 环境要求

- JDK 17+
- IntelliJ IDEA 2023.2+
- Gradle 8.0+

### 开发步骤

1. **克隆代码**
```bash
git clone <repository-url>
cd local-code-switcher
```

2. **导入项目**
   - 使用 IDEA 打开项目
   - Gradle 会自动下载依赖

3. **运行插件**
```bash
./gradlew runIde
```

4. **调试插件**
   - 在 IDEA 中设置断点
   - 运行 `Run → Debug 'Run IDE'`

5. **构建发布版本**
```bash
./gradlew buildPlugin
```

### 项目结构

```
local-code-switcher/
├── src/main/
│   ├── kotlin/com/zxy/plugin/qoder/
│   │   ├── actions/          # 用户操作
│   │   ├── services/         # 核心服务
│   │   ├── settings/         # 配置管理
│   │   └── listeners/        # 事件监听
│   └── resources/
│       └── META-INF/
│           └── plugin.xml    # 插件配置
├── build.gradle.kts          # 构建配置
└── README.md                 # 文档
```

## 常见问题

### Q: Qoder IDE 无法启动
**A**: 检查以下几点：
1. 确认 Qoder IDE 路径配置正确
2. 确认 Qoder IDE 已正确安装
3. 检查是否有足够的系统权限

### Q: 文件同步不工作
**A**: 
1. 检查是否启用了"自动同步"选项
2. 确认文件扩展名在监控列表中
3. 查看 IDEA 日志是否有错误信息

### Q: 差异对比窗口不显示
**A**: 
1. 检查"同步后显示差异"选项是否启用
2. 确认文件确实有变更

## 更新日志

### Version 1.0.1 (2026-01-26)
- ✨ 统一 UI 名称为 "Local Code Switcher"
- ✅ 完善中英双语说明文档

### Version 1.0.0 (2026-01-16)
- ✨ 初始版本发布
- ✅ 支持 IDEA 到 Qoder IDE 的切换
- ✅ 实现代码自动同步
- ✅ 集成差异对比功能
- ✅ 完整的配置界面

## 许可证

MIT License

## 作者

- zxy

## 许可证

MIT License
