package com.zxy.plugin.qoder.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 插件设置界面配置
 */
class QoderSettingsConfigurable : Configurable {
    
    private var mainPanel: JPanel? = null
    private val qoderPathField = TextFieldWithBrowseButton()
    private val qoderProjectPathField = TextFieldWithBrowseButton()
    private val keepWindowCheckBox = JBCheckBox("打开 Qoder IDE 时保持 IDEA 窗口")
    
    override fun getDisplayName(): String = "Qoder IDE Switcher"
    
    override fun createComponent(): JComponent {
        val settings = QoderSettingsState.getInstance()
        
        // 配置 Qoder IDE 可执行文件选择器
        qoderPathField.addBrowseFolderListener(
            "选择 Qoder IDE 路径",
            "请选择 Qoder IDE 的安装目录或可执行文件",
            null,
            FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor()
        )
        
        // 配置 Qoder 项目目录选择器
        qoderProjectPathField.addBrowseFolderListener(
            "选择 Qoder IDE 项目根目录",
            "请选择 Qoder IDE 打开的项目根目录（与当前 IDEA 项目是同一 Git 仓库的不同克隆）",
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
        
        // 设置初始值
        qoderPathField.text = settings.qoderIdePath
        qoderProjectPathField.text = settings.qoderProjectPath
        keepWindowCheckBox.isSelected = settings.keepIdeaWindowOpen
        
        // 构建表单
        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Qoder IDE 可执行文件路径: "), qoderPathField, 1, false)
            .addComponent(JBLabel("例如: C:\\Program Files\\Qoder\\qoder.exe 或 /Applications/Qoder.app"))
            .addSeparator()
            .addLabeledComponent(JBLabel("Qoder IDE 项目根目录: "), qoderProjectPathField, 1, false)
            .addComponent(JBLabel("Qoder IDE 打开的项目路径（与当前 IDEA 项目是同一仓库的不同本地克隆）"))
            .addSeparator()
            .addComponent(keepWindowCheckBox)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        
        return mainPanel!!
    }
    
    override fun isModified(): Boolean {
        val settings = QoderSettingsState.getInstance()
        return qoderPathField.text != settings.qoderIdePath ||
                qoderProjectPathField.text != settings.qoderProjectPath ||
                keepWindowCheckBox.isSelected != settings.keepIdeaWindowOpen
    }
    
    override fun apply() {
        val settings = QoderSettingsState.getInstance()
        settings.qoderIdePath = qoderPathField.text
        settings.qoderProjectPath = qoderProjectPathField.text
        settings.keepIdeaWindowOpen = keepWindowCheckBox.isSelected
    }
    
    override fun reset() {
        val settings = QoderSettingsState.getInstance()
        qoderPathField.text = settings.qoderIdePath
        qoderProjectPathField.text = settings.qoderProjectPath
        keepWindowCheckBox.isSelected = settings.keepIdeaWindowOpen
    }
}
