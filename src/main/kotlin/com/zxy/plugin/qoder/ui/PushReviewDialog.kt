package com.zxy.plugin.qoder.ui

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.DoubleClickListener
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * 推送变更审查对话框
 * 用于展示 IDEA → Qoder 的变更并支持推送
 */
class PushReviewDialog(
    private val project: Project,
    private val changes: MutableList<FileChange>
) : DialogWrapper(project) {
    
    private val tableModel = object : DefaultTableModel(
        arrayOf("✓", "文件路径", "状态"),
        0
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean = column == 0
        override fun getColumnClass(columnIndex: Int): Class<*> = if (columnIndex == 0) java.lang.Boolean::class.java else String::class.java
    }
    private val table = JBTable(tableModel)
    private var selectAllCheckBox: JCheckBox? = null
    private val statsLabel = JLabel()
    
    data class FileChange(
        val relativePath: String,
        val ideaFilePath: String,
        val qoderFilePath: String,
        val changeType: ChangeType,
        var accepted: Boolean = false
    )
    
    enum class ChangeType {
        MODIFIED, CREATED, DELETED
    }
    
    init {
        title = "推送到 Qoder IDE - ${changes.size} 个文件"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        mainPanel.preferredSize = Dimension(800, 600)
        
        // 核心：直接添加文件列表面板
        val listPanel = createFileListPanel()
        mainPanel.add(listPanel, BorderLayout.CENTER)
        
        // 底部：一键操作区域
        val bottomPanel = createBottomPanel()
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)
        
        return mainPanel
    }
    
    /**
     * 创建文件列表面板
     */
    private fun createFileListPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        // 顶部：全选和统计
        val topPanel = JPanel(BorderLayout())
        selectAllCheckBox = JCheckBox("全选")
        selectAllCheckBox?.addActionListener {
            val selected = selectAllCheckBox?.isSelected ?: false
            for (i in 0 until tableModel.rowCount) {
                tableModel.setValueAt(selected, i, 0)
                if (i < changes.size) {
                    changes[i].accepted = selected
                }
            }
        }
        
        updateStats()
        topPanel.add(selectAllCheckBox, BorderLayout.WEST)
        topPanel.add(statsLabel, BorderLayout.EAST)
        panel.add(topPanel, BorderLayout.NORTH)
        
        // 配置表格
        table.setShowGrid(false)
        table.columnModel.getColumn(0).preferredWidth = 30
        table.columnModel.getColumn(0).maxWidth = 30
        table.columnModel.getColumn(1).preferredWidth = 300
        table.columnModel.getColumn(2).preferredWidth = 60
        
        // 添加数据
        refreshTableData()
        
        // 监听复选框变化
        tableModel.addTableModelListener { e ->
            if (e.column == 0 && e.firstRow >= 0 && e.firstRow < changes.size) {
                val value = tableModel.getValueAt(e.firstRow, 0) as? Boolean ?: false
                changes[e.firstRow].accepted = value
            }
        }
        
        // 添加双击事件进入 Diff
        object : DoubleClickListener() {
            override fun onDoubleClick(e: MouseEvent): Boolean {
                val row = table.rowAtPoint(e.point)
                if (row >= 0) {
                    val modelRow = table.convertRowIndexToModel(row)
                    // 如果双击的是复选框列，则不触发 Diff
                    if (table.columnAtPoint(e.point) == 0) return false
                    
                    val relativePath = tableModel.getValueAt(modelRow, 1) as? String ?: return false
                    val change = changes.find { it.relativePath == relativePath } ?: return false
                    showDiffAndInteractiveSync(change)
                    return true
                }
                return false
            }
        }.installOn(table)
        
        val scrollPane = JBScrollPane(table)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        // 底部增加操作提示
        val tipLabel = JLabel(" 💡 提示: 双击文件行可进入交互式 Diff 进行部分同步 ")
        tipLabel.foreground = com.intellij.ui.JBColor.GRAY
        tipLabel.font = com.intellij.util.ui.JBUI.Fonts.smallFont()
        panel.add(tipLabel, BorderLayout.SOUTH)
        
        return panel
    }

    /**
     * 更新统计标签
     */
    private fun updateStats() {
        statsLabel.text = " 共 ${changes.size} 个待推送变更 "
    }

    /**
     * 刷新表格数据
     */
    private fun refreshTableData() {
        val rowCount = tableModel.rowCount
        for (i in rowCount - 1 downTo 0) {
            tableModel.removeRow(i)
        }
        
        changes.forEach { change ->
            tableModel.addRow(arrayOf(
                change.accepted,
                change.relativePath,
                when (change.changeType) {
                    ChangeType.MODIFIED -> "修改"
                    ChangeType.CREATED -> "新增"
                    ChangeType.DELETED -> "删除"
                }
            ))
        }
        updateStats()
    }
    
    /**
     * 显示 Diff 并允许选择性同步
     */
    private fun showDiffAndInteractiveSync(change: FileChange) {
        try {
            val ideaFile = File(change.ideaFilePath)
            val qoderFile = File(change.qoderFilePath)
            
            // 如果 IDEA 文件不存在且不是删除类型，则无法对比
            if (!ideaFile.exists() && change.changeType != ChangeType.DELETED) {
                JOptionPane.showMessageDialog(contentPane, "找不到源文件: ${change.ideaFilePath}")
                return
            }
            
            val vfsLocal = LocalFileSystem.getInstance()
            var vQoderFile = vfsLocal.refreshAndFindFileByPath(change.qoderFilePath)
            
            // 如果文件不存在（CREATED 类型），先创建一个空文件以便支持 Diff 编辑
            if (vQoderFile == null && change.changeType == ChangeType.CREATED) {
                com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
                    try {
                        qoderFile.parentFile?.mkdirs()
                        qoderFile.createNewFile()
                        vQoderFile = vfsLocal.refreshAndFindFileByIoFile(qoderFile)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            val diffContentFactory = DiffContentFactory.getInstance()
            val fileType = vQoderFile?.fileType ?: FileTypeManager.getInstance().getFileTypeByFileName(change.relativePath)
            
            // IDEA 端内容（左侧 - 来源）
            val ideaDiffContent = if (ideaFile.exists()) {
                val ideaContent = ideaFile.readText()
                diffContentFactory.create(project, ideaContent, fileType)
            } else {
                diffContentFactory.createEmpty()
            }
            
            // Qoder 端内容（右侧 - 目标，可编辑）
            val qoderDiffContent = if (vQoderFile != null) {
                diffContentFactory.create(project, vQoderFile!!)
            } else {
                diffContentFactory.createEmpty()
            }
            
            val request = SimpleDiffRequest(
                "推送审查: ${change.relativePath}",
                ideaDiffContent,    // 左侧：IDEA (来源)
                qoderDiffContent,   // 右侧：Qoder (目标，可编辑)
                "IDEA 项目 (来源)",
                "Qoder IDE (目标)"
            )
            
            // 弹出 Diff 窗口
            DiffManager.getInstance().showDiff(project, request)
            
            // 窗口关闭后，重新检查文件是否已完全同步
            refreshFileStatus(change)
            
        } catch (e: Exception) {
            e.printStackTrace()
            JOptionPane.showMessageDialog(contentPane, "无法启动 Diff 窗口: ${e.message}")
        }
    }

    /**
     * 刷新单个文件的状态
     */
    private fun refreshFileStatus(change: FileChange) {
        val ideaFile = File(change.ideaFilePath)
        val qoderFile = File(change.qoderFilePath)
        
        if (ideaFile.exists() && qoderFile.exists()) {
            // 关键：强制刷新 VFS 并保存所有文档，确保磁盘上的文件是最新的
            val vfsLocal = LocalFileSystem.getInstance()
            val vQoderFile = vfsLocal.refreshAndFindFileByIoFile(qoderFile)
            if (vQoderFile != null) {
                com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().saveAllDocuments()
                vQoderFile.refresh(false, false)
            }

            val ideaContent = ideaFile.readText().replace("\r\n", "\n").trim()
            val qoderContent = qoderFile.readText().replace("\r\n", "\n").trim()
            
            if (ideaContent == qoderContent) {
                // 完全一致，从列表中移除
                val index = changes.indexOf(change)
                if (index >= 0) {
                    changes.removeAt(index)
                    tableModel.removeRow(index)
                    updateStats()
                    
                    if (changes.isEmpty()) {
                        JOptionPane.showMessageDialog(contentPane, "所有变更已推送完成！")
                        close(OK_EXIT_CODE)
                    }
                }
            } else {
                // 仍有差异，保持原样
                table.repaint()
            }
        }
    }
    
    override fun createActions(): Array<Action> {
        return arrayOf(cancelAction)
    }

    /**
     * 创建底部操作面板
     */
    private fun createBottomPanel(): JComponent {
        val panel = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT))
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        val pushAllBtn = JButton("一键推送所有变更")
        pushAllBtn.addActionListener {
            val result = JOptionPane.showConfirmDialog(
                contentPane,
                "确定要将 IDEA 的所有变更推送到 Qoder IDE 项目中吗？",
                "确认全部推送",
                JOptionPane.YES_NO_OPTION
            )
            
            if (result == JOptionPane.YES_OPTION) {
                // 执行全部推送逻辑
                pushFiles(changes.toList())
            }
        }
        
        val pushSelectedBtn = JButton("推送勾选的变更")
        pushSelectedBtn.addActionListener {
            val selected = changes.filter { it.accepted }
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(contentPane, "请先勾选需要推送的文件")
                return@addActionListener
            }
            pushFiles(selected)
        }
        
        panel.add(pushSelectedBtn)
        panel.add(pushAllBtn)
        
        return panel
    }

    /**
     * 批量推送文件
     */
    private fun pushFiles(filesToPush: List<FileChange>) {
        ApplicationManager.getApplication().runWriteAction {
            filesToPush.forEach { change ->
                try {
                    val ideaFile = File(change.ideaFilePath)
                    val qoderFile = File(change.qoderFilePath)
                    
                    if (ideaFile.exists()) {
                        qoderFile.parentFile?.mkdirs()
                        ideaFile.copyTo(qoderFile, overwrite = true)
                        
                        val vfsLocal = LocalFileSystem.getInstance()
                        val vFile = vfsLocal.refreshAndFindFileByIoFile(qoderFile)
                        vFile?.refresh(false, false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // 推送完成后从列表中移除并刷新
        val pushPaths = filesToPush.map { it.ideaFilePath }.toSet()
        val iterator = changes.iterator()
        while (iterator.hasNext()) {
            if (pushPaths.contains(iterator.next().ideaFilePath)) {
                iterator.remove()
            }
        }
        
        refreshTableData()
        
        if (changes.isEmpty()) {
            JOptionPane.showMessageDialog(contentPane, "所有变更已推送完成！")
            close(OK_EXIT_CODE)
        }
    }

    /**
     * 获取接受的变更
     */
    fun getAcceptedChanges(): List<FileChange> {
        return changes.filter { it.accepted }
    }
}
