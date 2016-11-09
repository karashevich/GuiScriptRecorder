package ui

import com.intellij.util.ui.EdtInvocationManager
import components.GuiRecorderComponent
import javax.swing.SwingUtilities

/**
 * @author Sergey Karashevich
 */
object Notifier{

    fun updateStatus(statusMessage: String){
        if (GuiRecorderComponent.getFrame() == null) return
        val guiScriptEditorPanel = GuiRecorderComponent.getFrame()!!.getGuiScriptEditorPanel()
        val statusHandler: (String) -> Unit = { status ->
            if (status.startsWith("<long>")) {
                guiScriptEditorPanel.updateStatusWithProgress(status.substring(6))
            } else {
                guiScriptEditorPanel.stopProgress()
                guiScriptEditorPanel.updateStatus(status)
            }
        }

        if (EdtInvocationManager.getInstance().isEventDispatchThread) statusHandler.invoke(statusMessage)
        else SwingUtilities.invokeAndWait { statusHandler.invoke(statusMessage) }
    }

}