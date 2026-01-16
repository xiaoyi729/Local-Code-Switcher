package com.zxy.plugin.qoder.ui

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * 变更审查对话框
 * 左侧显示变更文件列表，右侧显示选中文件的 Diff
 */
class ChangeReviewDialog(
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
        val qoderFilePath: String,
        val ideaFilePath: String,
        val changeType: ChangeType,
        var accepted: Boolean = false
    )
    
    enum class ChangeType {
        MODIFIED, CREATED, DELETED
    }
    
    init {
        title = "Qoder IDE 变更审查 - ${changes.size} 个文件"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        mainPanel.preferredSize = Dimension(800, 600)
        
        // 核心：直接添加文件列表面板，不再使用分割面板
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
        
        // 监听复选框变化 (核心修复: 实时同步状态)
        tableModel.addTableModelListener { e ->
            if (e.column == 0 && e.firstRow >= 0 && e.firstRow < changes.size) {
                val value = tableModel.getValueAt(e.firstRow, 0) as? Boolean ?: false
                changes[e.firstRow].accepted = value
            }
        }
        
        // 添加双击事件进入 Diff
        table.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2 && table.selectedRow >= 0) {
                    val selectedIndex = table.selectedRow
                    showDiffAndInteractiveSync(changes[selectedIndex])
                }
            }
        })
        
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
        statsLabel.text = " 共 ${changes.size} 个待处理变更 "
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
            val qoderFile = File(change.qoderFilePath)
            val ideaFile = File(change.ideaFilePath)
            
            if (!qoderFile.exists()) return
            
            val vfsLocal = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
            val vIdeaFile = vfsLocal.refreshAndFindFileByIoFile(ideaFile) ?: return
            
            // 创建 Diff 内容
            val diffContentFactory = DiffContentFactory.getInstance()
            
            // Qoder 端：只读内容
            val qoderContent = qoderFile.readText()
            val qoderDiffContent = diffContentFactory.create(project, qoderContent, vIdeaFile.fileType)
            
            // IDEA 端：关联真实文件，使其可编辑 (>> 按钮会生效)
            val ideaDiffContent = diffContentFactory.create(project, vIdeaFile)
            
            val request = SimpleDiffRequest(
                "选择性同步: ${change.relativePath}",
                ideaDiffContent,    // 左侧：IDEA (可编辑)
                qoderDiffContent,   // 右侧：Qoder (只读)
                "IDEA 项目 (本地)",
                "Qoder IDE (来源)"
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
        val qoderFile = File(change.qoderFilePath)
        val ideaFile = File(change.ideaFilePath)
        
        if (qoderFile.exists() && ideaFile.exists()) {
            // 关键：强制刷新 VFS 并保存所有文档，确保磁盘上的文件是最新的
            val vfsLocal = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
            val vIdeaFile = vfsLocal.refreshAndFindFileByIoFile(ideaFile)
            if (vIdeaFile != null) {
                com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().saveAllDocuments()
                vIdeaFile.refresh(false, false)
            }

            val qoderContent = qoderFile.readText().replace("\r\n", "\n").trim()
            val ideaContent = ideaFile.readText().replace("\r\n", "\n").trim()
            
            if (qoderContent == ideaContent) {
                // 完全一致，从列表中移除
                val index = changes.indexOf(change)
                if (index >= 0) {
                    changes.removeAt(index)
                    tableModel.removeRow(index)
                    updateStats() // 修复: 更新统计文字
                    
                    if (changes.isEmpty()) {
                        JOptionPane.showMessageDialog(contentPane, "所有变更已同步完成！")
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
        
        val syncAllBtn = JButton("一键接受所有变更")
        syncAllBtn.addActionListener {
            val result = JOptionPane.showConfirmDialog(
                contentPane,
                "确定要将 Qoder 的所有变更覆盖到 IDEA 项目中吗？",
                "确认全部同步",
                JOptionPane.YES_NO_OPTION
            )
            
            if (result == JOptionPane.YES_OPTION) {
                // 执行全部同步逻辑
                syncFiles(changes.toList())
            }
        }
        
        val syncSelectedBtn = JButton("接受勾选的变更")
        syncSelectedBtn.addActionListener {
            val selected = changes.filter { it.accepted }
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(contentPane, "请先勾选需要同步的文件")
                return@addActionListener
            }
            syncFiles(selected)
        }
        
        panel.add(syncSelectedBtn)
        panel.add(syncAllBtn)
        
        return panel
    }

    /**
     * 批量同步文件
     */
    private fun syncFiles(filesToSync: List<FileChange>) {
        com.intellij.openapi.application.ApplicationManager.getApplication().runWriteAction {
            filesToSync.forEach { change ->
                try {
                    val qoderFile = File(change.qoderFilePath)
                    val ideaFile = File(change.ideaFilePath)
                    
                    if (qoderFile.exists()) {
                        ideaFile.parentFile?.mkdirs()
                        qoderFile.copyTo(ideaFile, overwrite = true)
                        
                        val vfsLocal = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                        val vFile = vfsLocal.refreshAndFindFileByIoFile(ideaFile)
                        vFile?.refresh(false, false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // 同步完成后从列表中移除并刷新
        val syncPaths = filesToSync.map { it.qoderFilePath }.toSet()
        val iterator = changes.iterator()
        while (iterator.hasNext()) {
            if (syncPaths.contains(iterator.next().qoderFilePath)) {
                iterator.remove()
            }
        }
        
        refreshTableData()
        
        if (changes.isEmpty()) {
            JOptionPane.showMessageDialog(contentPane, "所有变更已同步完成！")
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
