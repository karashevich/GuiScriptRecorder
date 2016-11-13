package components

import actions.StartPauseRecAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ApplicationComponent

/**
 * @author Sergey Karashevich
 */
object GuiRecorderComponent : ApplicationComponent, Disposable {

    override fun dispose() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var myFrame: ui.GuiScriptEditorFrame? = null

    override fun getComponentName() = "GuiRecorderComponent"

    override fun disposeComponent() {

    }

    override fun initComponent() {
        val recAction = StartPauseRecAction()
        recAction.setSelected(null, true)
    }

    fun getFrame() = myFrame

    fun getEditor() = myFrame!!.getGuiScriptEditorPanel().editor

    fun registerFrame(frame: ui.GuiScriptEditorFrame) {
        myFrame = frame
    }

    fun unregisterFrame(){
        if (myFrame != null)
            myFrame!!.dispose()
    }

    fun disposeFrame(){
        myFrame = null
    }

}