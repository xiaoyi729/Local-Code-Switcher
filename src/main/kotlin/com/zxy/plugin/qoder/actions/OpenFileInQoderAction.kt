package com.zxy.plugin.qoder.actions

import com.zxy.plugin.qoder.services.QoderProjectService
import com.zxy.plugin.qoder.settings.QoderSettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import java.io.File

/**
 * 打开当前文件到 Qoder IDE
 */
class OpenFileInQoderAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val settings = QoderSettingsState.getInstance()
        
        // 检查 Qoder IDE 路径是否配置
        if (settings.qoderIdePath.isEmpty()) {
            Messages.showErrorDialog(
                project,
                "请先在设置中配置 Qoder IDE 路径\n路径: File → Settings → Tools → Qoder IDE Switcher",
                "Qoder IDE 路径未配置"
            )
            return
        }
        
        // 检查路径是否存在
        val qoderPath = File(settings.qoderIdePath)
        if (!qoderPath.exists()) {
            Messages.showErrorDialog(
                project,
                "Qoder IDE 路径不存在: ${settings.qoderIdePath}\n请检查设置",
                "路径错误"
            )
            return
        }
        
        try {
            // 获取项目服务
            val projectService = project.service<QoderProjectService>()
            
            // 保存文件快照 (已废弃，保留占位)
            projectService.saveFileSnapshot(virtualFile)
            
            // 打开 Qoder IDE
            val filePath = virtualFile.path
            val command = buildCommand(settings.qoderIdePath, filePath)
            
            Runtime.getRuntime().exec(command)
            
            // 显示通知
            Messages.showInfoMessage(
                project,
                "已在 Qoder IDE 中打开文件:\n${virtualFile.name}\n\n" +
                        "在 Qoder IDE 中编辑完成后保存,\n请回到 IDEA 使用 'Tools -> 同步 Qoder 变更' 查看修改。",
                "Qoder IDE 已启动"
            )
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                project,
                "启动 Qoder IDE 失败:\n${ex.message}",
                "启动失败"
            )
        }
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = project != null && file != null && !file.isDirectory
    }
    
    private fun buildCommand(qoderPath: String, filePath: String): Array<String> {
        val os = System.getProperty("os.name").lowercase()
        
        return when {
            os.contains("win") -> {
                // Windows
                if (qoderPath.endsWith(".exe")) {
                    arrayOf(qoderPath, filePath)
                } else {
                    arrayOf("$qoderPath\\qoder.exe", filePath)
                }
            }
            os.contains("mac") -> {
                // macOS
                if (qoderPath.endsWith(".app")) {
                    arrayOf("open", "-a", qoderPath, filePath)
                } else {
                    arrayOf(qoderPath, filePath)
                }
            }
            else -> {
                // Linux
                arrayOf(qoderPath, filePath)
            }
        }
    }
}
