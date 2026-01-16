package com.zxy.plugin.qoder.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * 插件设置状态持久化
 */
@State(
    name = "com.zxy.plugin.qoder.settings.QoderSettingsState",
    storages = [Storage("QoderSwitcherSettings.xml")]
)
@Service
class QoderSettingsState : PersistentStateComponent<QoderSettingsState> {
    
    /** Qoder IDE 可执行文件路径 */
    var qoderIdePath: String = ""
    
    /** Qoder IDE 项目根目录路径（与 IDEA 项目是同一 Git 仓库的不同本地克隆） */
    var qoderProjectPath: String = ""
    
    /** 是否在打开 Qoder 时保持 IDEA 窗口 */
    var keepIdeaWindowOpen: Boolean = true
    
    override fun getState(): QoderSettingsState {
        return this
    }
    
    override fun loadState(state: QoderSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
    
    companion object {
        fun getInstance(): QoderSettingsState {
            return service()
        }
    }
}
