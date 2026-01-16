package com.zxy.plugin.qoder.actions

import com.zxy.plugin.qoder.services.QoderDirectorySyncService
import com.zxy.plugin.qoder.settings.QoderSettingsState
import com.zxy.plugin.qoder.ui.ChangeReviewDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * 手动触发 Qoder 项目全量同步的 Action
 */
class ManualSyncQoderChangesAction : AnAction("同步 Qoder 变更") {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        val settings = QoderSettingsState.getInstance()
        if (settings.qoderProjectPath.isEmpty()) {
            Messages.showWarningDialog(
                project,
                "请先在设置中配置 Qoder IDE 项目根目录",
                "配置缺失"
            )
            return
        }
        
        // 使用后台任务进行扫描，避免 UI 卡死
        com.intellij.openapi.progress.ProgressManager.getInstance().run(
            object : com.intellij.openapi.progress.Task.Backgroundable(project, "正在扫描 Qoder 变更...", false) {
                override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                    val syncService = project.service<QoderDirectorySyncService>()
                    val changes = syncService.scanChanges().toMutableList()
                    
                    ApplicationManager.getApplication().invokeLater {
                        if (changes.isEmpty()) {
                            Messages.showInfoMessage(project, "没有检测到 Qoder IDE 项目的变更", "同步结果")
                        } else {
                            val dialog = ChangeReviewDialog(project, changes)
                            dialog.show() // 使用 show() 即可，因为逻辑在交互中完成
                        }
                    }
                }
            }
        )
    }
    
    /**
     * 应用变更
     */
    private fun applyChanges(
        project: com.intellij.openapi.project.Project,
        changes: List<ChangeReviewDialog.FileChange>
    ) {
        if (changes.isEmpty()) {
            return
        }
        
        var successCount = 0
        var failCount = 0
        
        ApplicationManager.getApplication().runWriteAction {
            changes.forEach { change ->
                try {
                    val qoderFile = File(change.qoderFilePath)
                    val ideaFile = File(change.ideaFilePath)
                    
                    if (!qoderFile.exists()) {
                        failCount++
                        return@forEach
                    }
                    
                    // 确保目标目录存在
                    ideaFile.parentFile?.mkdirs()
                    
                    // 复制文件
                    qoderFile.copyTo(ideaFile, overwrite = true)
                    
                    // 刷新 VFS
                    val vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ideaFile)
                    vFile?.refresh(false, false)
                    
                    successCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                    failCount++
                }
            }
        }
        
        // 显示结果
        val message = if (failCount == 0) {
            "成功同步 $successCount 个文件"
        } else {
            "成功同步 $successCount 个文件，$failCount 个文件失败"
        }
        
        Messages.showInfoMessage(project, message, "同步完成")
    }
}
