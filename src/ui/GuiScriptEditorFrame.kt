package ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.ui.EdtInvocationManager
import compile.KotlinCompileUtil
import java.awt.Container
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.util.function.Consumer
import javax.swing.AbstractAction
import javax.swing.JFrame
import javax.swing.SwingUtilities

open class GuiScriptEditorFrame() {

    val frameName = "GUI Script Editor"
    val myFrame: JFrame
    val guiScriptEditorPanel: GuiScriptEditorPanel

    init {
        myFrame = JFrame(frameName)
        myFrame.preferredSize = Dimension(500, 800)

        //create editor if needed
        GuiScriptEditor.createEditor()
        guiScriptEditorPanel = GuiScriptEditorPanel()
        val myContentPanel = guiScriptEditorPanel.panel as Container

        myFrame.contentPane = myContentPanel
        myFrame.pack()
        myFrame.isVisible = true

        installRunAction()
    }

    fun updateStatus(status: String) {
        val statusHandler: (String) -> Unit = { status ->
            if (status.startsWith("<long>")) {
                guiScriptEditorPanel.updateStatusWithProgress(status.substring(6))
            } else {
                guiScriptEditorPanel.stopProgress()
                guiScriptEditorPanel.updateStatus(status)
            }
        }

        if (EdtInvocationManager.getInstance().isEventDispatchThread) statusHandler.invoke(status)
        else SwingUtilities.invokeAndWait { statusHandler.invoke(status) }
    }

    fun updateStatusWithProgress(status: String) = guiScriptEditorPanel.updateStatusWithProgress(status)
    fun stopProgress() = guiScriptEditorPanel.stopProgress()

    val myNotifier: Consumer<String> = object : Consumer<String> {
        override fun accept(str: String?) {
            updateStatus(str!!)
        }
    }

    fun installRunAction() {
//        ApplicationManager.getApplication().messageBus.connect().subscribe(AppTopics.DAEMON, object : DaemonListener {
//            override fun daemonListener(status: String) {
//                updateStatus(status)
//            }
//        })
        guiScriptEditorPanel.setRunButtonAction(object : AbstractAction("Run") {
            override fun actionPerformed(e: ActionEvent?) {
                updateStatus("<long>Run current script")
                ApplicationManager.getApplication().executeOnPooledThread { KotlinCompileUtil.compileAndEvalCodeWithNotifier(GuiScriptEditor.getCode(), myNotifier) }
            }
        })
    }

}

class StatusUpdater(val myFrame: GuiScriptEditorFrame) {

    fun update(status: String) = myFrame.updateStatus(status)
}