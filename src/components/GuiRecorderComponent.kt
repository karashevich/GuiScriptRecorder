package components

import actions.StartStopRecAction
import com.intellij.openapi.components.ApplicationComponent

/**
 * @author Sergey Karashevich
 */
object GuiRecorderComponent : ApplicationComponent {

    private var myFrame: ui.GuiScriptEditorFrame? = null

    override fun getComponentName() = "GuiRecorderComponent"

    override fun disposeComponent() {

    }

    override fun initComponent() {
        val recAction = StartStopRecAction()
        recAction.setSelected(null, true)
    }

    fun getFrame() = myFrame

    fun getEditor() = myFrame!!.getGuiScriptEditorPanel().editor

    fun registerFrame(frame: ui.GuiScriptEditorFrame) {
        myFrame = frame
    }

    fun disposeFrame(){
        myFrame = null
    }

}