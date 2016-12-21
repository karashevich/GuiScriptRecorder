package components

import com.intellij.openapi.components.ApplicationComponent

/**
 * @author Sergey Karashevich
 */
object GuiRecorderComponent : ApplicationComponent {

    override fun disposeComponent() {

    }

    private var myFrame: ui.GuiScriptEditorFrame? = null

    override fun getComponentName() = "GuiRecorderComponent"


    override fun initComponent() {

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