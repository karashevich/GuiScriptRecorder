package ui

import compile.DaemonNotifier
import compile.KotlinCompileUtil
import java.awt.Container
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JFrame

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

    fun updateStatus(status: String) = guiScriptEditorPanel.updateStatus(status)
    fun updateStatusWithProgress(status: String) = guiScriptEditorPanel.updateStatusWithProgress(status)
    fun stopProgress() = guiScriptEditorPanel.stopProgress()

    val myNotifier = object : DaemonNotifier {
        override fun eventDispatched(event: String) {
            updateStatus(event)
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
                KotlinCompileUtil.compileAndEvalCodeWithNotifier(GuiScriptEditor.getCode(), myNotifier)
            }
        })
    }

}

class StatusUpdater(val myFrame: GuiScriptEditorFrame) {

    fun update(status: String) = myFrame.updateStatus(status)
}