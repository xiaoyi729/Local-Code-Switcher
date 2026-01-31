package com.zxy.plugin.qoder.actions

import com.zxy.plugin.qoder.services.QoderDirectorySyncService
import com.zxy.plugin.qoder.settings.QoderSettingsState
import com.zxy.plugin.qoder.ui.PushReviewDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages

/**
 * 将 IDEA 中的修改推送到 Qoder IDE 的 Action
 * 扫描 IDEA 项目中的变更，并同步到 Qoder IDE 项目
 */
class PushToQoderAction : AnAction("推送到 Qoder IDE") {
    
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
            object : com.intellij.openapi.progress.Task.Backgroundable(project, "正在扫描 IDEA 变更...", false) {
                override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                    val syncService = project.service<QoderDirectorySyncService>()
                    val changes = syncService.scanChangesFromIdea().toMutableList()
                    
                    ApplicationManager.getApplication().invokeLater {
                        if (changes.isEmpty()) {
                            Messages.showInfoMessage(project, "没有检测到需要推送到 Qoder IDE 的变更", "推送结果")
                        } else {
                            val dialog = PushReviewDialog(project, changes)
                            dialog.show()
                        }
                    }
                }
            }
        )
    }
}
