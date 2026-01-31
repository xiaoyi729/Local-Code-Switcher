package com.zxy.plugin.qoder.services

import com.zxy.plugin.qoder.settings.QoderSettingsState
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * Qoder 目录同步服务
 * 监控 Qoder IDE 项目目录的文件变更，并同步到 IDEA 项目
 */
@Service(Service.Level.PROJECT)
class QoderDirectorySyncService(private val project: Project) {
    
    /**
     * 判断是否应该忽略该目录
     */
    private fun shouldIgnoreDirectory(path: Path): Boolean {
        val name = path.fileName.toString()
        return name.startsWith(".") || 
               name == "build" || 
               name == "target" || 
               name == "node_modules" ||
               name == "out"
    }
    
    /**
     * 判断是否应该监控该文件
     */
    private fun shouldMonitorFile(path: Path): Boolean {
        if (!Files.isRegularFile(path)) {
            return false
        }
        
        val fileName = path.fileName.toString()
        val extension = fileName.substringAfterLast('.', "")
        
        // 忽略没有扩展名的文件和部分二进制/系统文件
        val ignoredExtensions = setOf("exe", "dll", "so", "class", "jar", "png", "jpg", "jpeg", "gif", "pdf")
        return extension.isNotEmpty() && !ignoredExtensions.contains(extension.lowercase())
    }
    
    /**
     * 手动扫描 Qoder 项目目录，查找所有变更的文件
     */
    fun scanChanges(): List<com.zxy.plugin.qoder.ui.ChangeReviewDialog.FileChange> {
        val settings = QoderSettingsState.getInstance()
        
        if (settings.qoderProjectPath.isEmpty()) {
            return emptyList()
        }
        
        val qoderDir = File(settings.qoderProjectPath)
        if (!qoderDir.exists() || !qoderDir.isDirectory) {
            return emptyList()
        }
        
        val ideaDir = File(project.basePath ?: return emptyList())
        if (!ideaDir.exists() || !ideaDir.isDirectory) {
            return emptyList()
        }
        
        val changes = mutableListOf<com.zxy.plugin.qoder.ui.ChangeReviewDialog.FileChange>()
        
        // 递归扫描 Qoder 项目目录
        scanDirectory(qoderDir.toPath(), Paths.get(settings.qoderProjectPath), ideaDir.toPath(), changes)
        
        return changes
    }
    
    /**
     * 递归扫描目录
     */
    private fun scanDirectory(
        dir: Path,
        qoderRoot: Path,
        ideaRoot: Path,
        changes: MutableList<com.zxy.plugin.qoder.ui.ChangeReviewDialog.FileChange>
    ) {
        try {
            Files.walk(dir, 1).use { paths ->
                paths.filter { it != dir }.forEach { path ->
                    if (Files.isDirectory(path)) {
                        if (!shouldIgnoreDirectory(path)) {
                            scanDirectory(path, qoderRoot, ideaRoot, changes)
                        }
                    } else if (Files.isRegularFile(path)) {
                        if (shouldMonitorFile(path)) {
                            checkFileChange(path, qoderRoot, ideaRoot, changes)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略读取错误
        }
    }
    
    /**
     * 检查文件是否有变更
     */
    private fun checkFileChange(
        qoderFile: Path,
        qoderRoot: Path,
        ideaRoot: Path,
        changes: MutableList<com.zxy.plugin.qoder.ui.ChangeReviewDialog.FileChange>
    ) {
        try {
            val relativePath = qoderRoot.relativize(qoderFile)
            val ideaFile = ideaRoot.resolve(relativePath)
            
            // 检查文件大小，避免读取超大文件
            if (Files.size(qoderFile) > 10 * 1024 * 1024) return // 忽略 > 10MB 的文件

            val qoderContent = try {
                Files.readString(qoderFile)
            } catch (e: Exception) {
                // 如果读取失败（可能是编码问题或二进制文件），则跳过
                return
            }
            
            val changeType: com.zxy.plugin.qoder.ui.ChangeReviewDialog.ChangeType
            val hasChange: Boolean
            
            if (!Files.exists(ideaFile)) {
                changeType = com.zxy.plugin.qoder.ui.ChangeReviewDialog.ChangeType.CREATED
                hasChange = true
            } else {
                val ideaContent = try {
                    Files.readString(ideaFile)
                } catch (e: Exception) {
                    return
                }
                
                // 规范化内容对比，忽略换行符差异
                val normQoder = qoderContent.replace("\r\n", "\n").trim()
                val normIdea = ideaContent.replace("\r\n", "\n").trim()
                
                if (normQoder != normIdea) {
                    changeType = com.zxy.plugin.qoder.ui.ChangeReviewDialog.ChangeType.MODIFIED
                    hasChange = true
                } else {
                    hasChange = false
                    changeType = com.zxy.plugin.qoder.ui.ChangeReviewDialog.ChangeType.MODIFIED
                }
            }
            
            if (hasChange) {
                changes.add(
                    com.zxy.plugin.qoder.ui.ChangeReviewDialog.FileChange(
                        relativePath = relativePath.toString(),
                        qoderFilePath = qoderFile.toString(),
                        ideaFilePath = ideaFile.toString(),
                        changeType = changeType
                    )
                )
            }
        } catch (e: Exception) {
            // 忽略单个文件的错误
        }
    }
    
    /**
     * 反向扫描：从 IDEA 项目扫描变更，用于推送到 Qoder IDE
     */
    fun scanChangesFromIdea(): List<com.zxy.plugin.qoder.ui.PushReviewDialog.FileChange> {
        val settings = QoderSettingsState.getInstance()
        
        if (settings.qoderProjectPath.isEmpty()) {
            return emptyList()
        }
        
        val ideaDir = File(project.basePath ?: return emptyList())
        if (!ideaDir.exists() || !ideaDir.isDirectory) {
            return emptyList()
        }
        
        val qoderDir = File(settings.qoderProjectPath)
        if (!qoderDir.exists() || !qoderDir.isDirectory) {
            return emptyList()
        }
        
        val changes = mutableListOf<com.zxy.plugin.qoder.ui.PushReviewDialog.FileChange>()
        
        // 递归扫描 IDEA 项目目录
        scanDirectoryFromIdea(ideaDir.toPath(), ideaDir.toPath(), qoderDir.toPath(), changes)
        
        return changes
    }
    
    /**
     * 递归扫描目录（反向：从 IDEA 到 Qoder）
     */
    private fun scanDirectoryFromIdea(
        dir: Path,
        ideaRoot: Path,
        qoderRoot: Path,
        changes: MutableList<com.zxy.plugin.qoder.ui.PushReviewDialog.FileChange>
    ) {
        try {
            Files.walk(dir, 1).use { paths ->
                paths.filter { it != dir }.forEach { path ->
                    if (Files.isDirectory(path)) {
                        if (!shouldIgnoreDirectory(path)) {
                            scanDirectoryFromIdea(path, ideaRoot, qoderRoot, changes)
                        }
                    } else if (Files.isRegularFile(path)) {
                        if (shouldMonitorFile(path)) {
                            checkFileChangeFromIdea(path, ideaRoot, qoderRoot, changes)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略读取错误
        }
    }
    
    /**
     * 检查文件是否有变更（反向：从 IDEA 到 Qoder）
     */
    private fun checkFileChangeFromIdea(
        ideaFile: Path,
        ideaRoot: Path,
        qoderRoot: Path,
        changes: MutableList<com.zxy.plugin.qoder.ui.PushReviewDialog.FileChange>
    ) {
        try {
            val relativePath = ideaRoot.relativize(ideaFile)
            val qoderFile = qoderRoot.resolve(relativePath)
            
            // 检查文件大小，避免读取超大文件
            if (Files.size(ideaFile) > 10 * 1024 * 1024) return // 忽略 > 10MB 的文件

            val ideaContent = try {
                Files.readString(ideaFile)
            } catch (e: Exception) {
                // 如果读取失败（可能是编码问题或二进制文件），则跳过
                return
            }
            
            val changeType: com.zxy.plugin.qoder.ui.PushReviewDialog.ChangeType
            val hasChange: Boolean
            
            if (!Files.exists(qoderFile)) {
                changeType = com.zxy.plugin.qoder.ui.PushReviewDialog.ChangeType.CREATED
                hasChange = true
            } else {
                val qoderContent = try {
                    Files.readString(qoderFile)
                } catch (e: Exception) {
                    return
                }
                
                // 规范化内容对比，忽略换行符差异
                val normIdea = ideaContent.replace("\r\n", "\n").trim()
                val normQoder = qoderContent.replace("\r\n", "\n").trim()
                
                if (normIdea != normQoder) {
                    changeType = com.zxy.plugin.qoder.ui.PushReviewDialog.ChangeType.MODIFIED
                    hasChange = true
                } else {
                    hasChange = false
                    changeType = com.zxy.plugin.qoder.ui.PushReviewDialog.ChangeType.MODIFIED
                }
            }
            
            if (hasChange) {
                changes.add(
                    com.zxy.plugin.qoder.ui.PushReviewDialog.FileChange(
                        relativePath = relativePath.toString(),
                        ideaFilePath = ideaFile.toString(),
                        qoderFilePath = qoderFile.toString(),
                        changeType = changeType
                    )
                )
            }
        } catch (e: Exception) {
            // 忽略单个文件的错误
        }
    }
}
