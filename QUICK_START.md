# Local Code Switcher Plugin - 快速使用指南

## 快速开始

### 1. 安装 Gradle Wrapper

如果还没有 Gradle Wrapper，请运行：

```bash
# Windows PowerShell
gradle wrapper

# macOS/Linux
gradle wrapper
```

### 2. 构建插件

```bash
# Windows
gradlew.bat buildPlugin

# macOS/Linux
./gradlew buildPlugin
```

构建完成后，插件文件位于：`build/distributions/local-code-switcher-1.0.0.zip`

### 3. 安装到 IDEA

1. 打开 IntelliJ IDEA
2. 进入 `File → Settings → Plugins` (Windows/Linux) 或 `IntelliJ IDEA → Preferences → Plugins` (macOS)
3. 点击齿轮图标 ⚙️
4. 选择 `Install Plugin from Disk...`
5. 选择 `build/distributions/local-code-switcher-1.0.0.zip`
6. 重启 IDEA

### 4. 配置插件

1. 打开 `File → Settings → Tools → Qoder IDE Switcher`
2. 设置 Qoder IDE 路径：
   - Windows: `C:\Program Files\Qoder\qoder.exe`
   - macOS: `/Applications/Qoder.app`
3. 设置 Qoder IDE 项目根目录：
   - 指向 Qoder 中打开的项目文件夹（与当前 IDEA 项目是同一仓库的不同克隆）。
4. 点击 `Apply` → `OK`

### 5. 开始使用

**第一步：在 IDEA 中发起跳转**
- 在编辑器或项目视图右键文件 → 选择 "Open in Qoder IDE"。

**第二步：在 Qoder 中编写代码**
- 利用 Qoder 的 AI 自动完成代码修改并**保存**。

**第三步：回到 IDEA 同步变更**
- 点击菜单 `Tools → Qoder IDE 同步 → 同步 Qoder 变更`。
- 或使用快捷键 `Ctrl + Alt + S`。
- 在弹出窗口中双击文件进入 Diff，点击 `>>` 符号按需接受修改。

## 工作流程

```
┌──────────────────────────────────────────────────────────┐
│  1. 在 IDEA 中发起跳转 (Right Click -> Open in Qoder)   │
│     ↓                                                    │
│  2. 在 Qoder 中 AI 编码并保存文件                        │
│     ↓                                                    │
│  3. 回到 IDEA 触发同步 (Ctrl + Alt + S)                 │
│     ↓                                                    │
│  4. 交互式 Diff 审查并接受变更 (Double Click & >>)       │
│     ↓                                                    │
│  5. 列表自动刷新，同步完成                               │
└──────────────────────────────────────────────────────────┘
```

## 核心功能

### ✅ 已实现的功能

- [x] IDE 切换 (IDEA → Qoder IDE)
- [x] 跨目录手动扫描
- [x] 交互式审查窗口
- [x] 双击直达交互式 Diff
- [x] 手术刀式代码合并 (>> 操作)
- [x] 批量一键同步
- [x] 极简配置界面 (零后台开销)
- [x] 跨平台支持 (Windows/macOS/Linux)

### 🚀 可扩展功能

- [ ] 双向切换 (Qoder IDE → IDEA)
- [ ] 冲突解决机制
- [ ] 变更历史记录
- [ ] 批量文件处理
- [ ] 自定义同步策略
- [ ] 通知提醒
- [ ] Git 集成

## 开发模式运行

如果你想在开发模式下测试插件：

```bash
# Windows
gradlew.bat runIde

# macOS/Linux
./gradlew runIde
```

这会启动一个带有插件的新 IDEA 实例。

## 故障排查

### 问题：找不到 Qoder IDE
**解决方案**：
1. 确认 Qoder IDE 已安装
2. 检查路径配置是否正确
3. Windows 用户：确保路径以 `.exe` 结尾
4. macOS 用户：确保路径指向 `.app` 文件

### 问题：文件同步不工作
**解决方案**：
1. 检查"启用自动同步"选项
2. 确认文件扩展名在监控列表中
3. 尝试手动刷新 (`File → Reload All from Disk`)

### 问题：差异对比不显示
**解决方案**：
1. 确认"同步后显示差异"选项已启用
2. 检查文件是否真的有变更
3. 查看 IDEA 日志：`Help → Show Log in Explorer`

## 技术支持

如有问题，请查看：
- 完整文档：[README.md](README.md)
- Issue 追踪：提交到项目 Issue 页面

---

**祝使用愉快！** 🎉
