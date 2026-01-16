package com.zxy.plugin.qoder.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.ConcurrentHashMap

/**
 * 项目级别的 Qoder 服务 (精简版)
 * 仅保留必要的 VFS 状态管理
 */
@Service(Service.Level.PROJECT)
class QoderProjectService(private val project: Project) : Disposable {
    
    // 监控中的文件 (保留接口兼容性)
    private val monitoredFiles = ConcurrentHashMap<String, Long>()
    
    /**
     * 判断是否正在监控该文件 (始终返回 true 或根据基本规则)
     */
    fun isMonitoring(virtualFile: VirtualFile): Boolean {
        return true
    }
    
    /**
     * 保存文件快照 (已废弃)
     */
    fun saveFileSnapshot(virtualFile: VirtualFile) {
    }
    
    /**
     * 检查文件是否有变更 (已废弃，改由 DirectorySyncService 处理)
     */
    fun hasFileChanged(virtualFile: VirtualFile): Boolean {
        return false
    }

    override fun dispose() {
        monitoredFiles.clear()
    }
}
