package components

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ApplicationComponent

/**
 * @author Sergey Karashevich
 */
object GuiRecorderComponent : ApplicationComponent, Disposable {

    enum class States {IDLE, COMPILING, COMPILATION_ERROR, COMPILATION_DONE, RUNNING, RUNNING_ERROR, TEST_INIT}

    var myState: States = States.IDLE;

    override fun dispose() {
    }

    private var myFrame: ui.GuiScriptEditorFrame? = null

    override fun getComponentName() = "GuiRecorderComponent"

    override fun disposeComponent() {

    }

    override fun initComponent() {

    }

    fun getState() = myState

    fun setState(yaState: States) { myState = yaState}

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