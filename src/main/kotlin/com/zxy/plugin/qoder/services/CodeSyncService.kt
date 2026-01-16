package com.zxy.plugin.qoder.services

import com.zxy.plugin.qoder.settings.QoderSettingsState
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.schedule

/**
 * 代码同步服务
 * 负责监听文件变更并触发差异对比
 */
@Service(Service.Level.APP)
class CodeSyncService {
    
    // 待处理的文件变更事件
    private val pendingChanges = ConcurrentHashMap<String, VFileContentChangeEvent>()
    
    // 定时器用于延迟处理变更
    private val timer = Timer("QoderSyncTimer", true)
    
    /**
     * 注册文件变更监听器 (已废弃，改为手动同步)
     */
    fun registerFileListener(project: Project) {
        // 之前实时监听逻辑已移除，改由 ManualSyncQoderChangesAction 处理
    }
    
    /**
     * 处理文件变更事件
     */
    private fun handleFileChange(project: Project, event: VFileContentChangeEvent) {
        val settings = QoderSettingsState.getInstance()
        val file = event.file
        
        // 检查文件是否在监控列表中
        val projectService = project.getService(QoderProjectService::class.java)
        if (!projectService.isMonitoring(file)) {
            return
        }
        
        // 添加到待处理队列
        pendingChanges[file.path] = event
        
        // 延迟处理 (不再使用延迟)
        processPendingChange(project, file.path)
    }
    
    /**
     * 处理待处理的变更
     */
    private fun processPendingChange(project: Project, filePath: String) {
        val event = pendingChanges.remove(filePath) ?: return
        val file = event.file
        
        val projectService = project.getService(QoderProjectService::class.java)
        
        // 检查文件是否真的有变更
        if (projectService.hasFileChanged(file)) {
            // 显示差异对比
            showDiff(project, file)
            
            // 更新快照
            projectService.saveFileSnapshot(file)
        }
    }
    
    /**
     * 显示文件差异对比
     */
    fun showDiff(project: Project, virtualFile: VirtualFile) {
        ApplicationManager.getApplication().invokeLater {
            try {
                // 保存当前文档
                val documentManager = FileDocumentManager.getInstance()
                val document = documentManager.getDocument(virtualFile)
                if (document != null) {
                    documentManager.saveDocument(document)
                }
                
                // 刷新文件
                virtualFile.refresh(false, false)
                
                // 获取快照内容
                val projectService = project.getService(QoderProjectService::class.java)
                val oldContent = getSnapshotContent(project, virtualFile.path)
                val newContent = String(virtualFile.contentsToByteArray())
                
                // 创建差异对比请求
                val diffContentFactory = DiffContentFactory.getInstance()
                val oldDiffContent = diffContentFactory.create(oldContent)
                val newDiffContent = diffContentFactory.create(project, virtualFile)
                
                val request = SimpleDiffRequest(
                    "Qoder IDE 同步变更对比: ${virtualFile.name}",
                    oldDiffContent,
                    newDiffContent,
                    "原始版本",
                    "Qoder 修改后"
                )
                
                // 显示差异窗口
                DiffManager.getInstance().showDiff(project, request)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 获取快照内容
     */
    private fun getSnapshotContent(project: Project, filePath: String): String {
        // 这里应该从快照中读取，简化处理返回空字符串
        // 实际应该在 QoderProjectService 中保存完整内容
        return try {
            val file = java.io.File(filePath)
            if (file.exists()) {
                // 可以添加备份文件机制
                ""
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 立即同步所有待处理的变更
     */
    fun flushPendingChanges(project: Project) {
        pendingChanges.keys.toList().forEach { filePath ->
            processPendingChange(project, filePath)
        }
    }
    
    /**
     * 清除待处理的变更
     */
    fun clearPendingChanges() {
        pendingChanges.clear()
    }
}
