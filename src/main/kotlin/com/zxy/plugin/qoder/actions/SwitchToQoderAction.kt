package com.zxy.plugin.qoder.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/**
 * 工具栏快速切换 Action
 */
class SwitchToQoderAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        // 根据上下文智能选择是打开文件还是项目
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        
        when {
            editor != null && file != null && !file.isDirectory -> {
                // 编辑器中有打开的文件，使用文件 Action
                OpenFileInQoderAction().actionPerformed(e)
            }
            file != null -> {
                // 项目视图中选中了文件或目录，使用项目 Action
                OpenProjectInQoderAction().actionPerformed(e)
            }
            else -> {
                // 打开当前项目根目录
                e.project?.let { project ->
                    val baseDir = project.baseDir
                    if (baseDir != null) {
                        val newEvent = AnActionEvent(
                            e.inputEvent,
                            e.dataContext,
                            e.place,
                            e.presentation,
                            e.actionManager,
                            e.modifiers
                        )
                        OpenProjectInQoderAction().actionPerformed(newEvent)
                    }
                }
            }
        }
    }
    
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
