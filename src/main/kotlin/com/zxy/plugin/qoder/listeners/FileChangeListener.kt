package com.zxy.plugin.qoder.listeners

import com.zxy.plugin.qoder.services.CodeSyncService
import com.zxy.plugin.qoder.services.QoderProjectService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * 文件变更监听器
 * 监听文件打开、关闭等事件
 */
class FileChangeListener(private val project: Project) : FileEditorManagerListener {
    
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        // 文件打开时的处理
        checkAndNotifyChanges(file)
    }
    
    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        // 文件关闭时停止监控（可选）
        // val projectService = project.service<QoderProjectService>()
        // projectService.stopMonitoring(file)
    }
    
    /**
     * 检查并通知文件变更
     */
    private fun checkAndNotifyChanges(file: VirtualFile) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val projectService = project.service<QoderProjectService>()
            
            // 如果文件在监控中且有变更
            if (projectService.isMonitoring(file) && projectService.hasFileChanged(file)) {
                val syncService = service<CodeSyncService>()
                syncService.showDiff(project, file)
                projectService.saveFileSnapshot(file)
            }
        }
    }
}
