package ui

import com.intellij.openapi.Disposable
import components.GuiRecorderComponent
import java.awt.Container
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities

class GuiScriptEditorFrame: Disposable {

    override fun dispose() {
        guiScriptEditorPanel.releaseEditor()
        GuiRecorderComponent.disposeFrame()
    }

    val frameName = "GUI Script Editor"
    val myFrame: JFrame
    private val guiScriptEditorPanel: GuiScriptEditorPanel

    init {
        myFrame = JFrame(frameName)
        myFrame.preferredSize = Dimension(500, 800)

        //create editor if needed
        guiScriptEditorPanel = GuiScriptEditorPanel()
        val myContentPanel = guiScriptEditorPanel.panel as Container

        myFrame.contentPane = myContentPanel
        myFrame.pack()
        myFrame.isVisible = true

        GuiRecorderComponent.registerFrame(this)
        myFrame.addWindowListener(object: WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                dispose()
            }
        })
    }

    fun toFront() { SwingUtilities.invokeLater { myFrame.toFront(); myFrame.repaint() } }

    fun getGuiScriptEditorPanel() = guiScriptEditorPanel

    fun getEditor() = guiScriptEditorPanel.editor

    fun setSyncToEditor(toSync: Boolean) {
        guiScriptEditorPanel.syncToEditor = toSync
    }

    fun isSyncToEditor() = guiScriptEditorPanel.syncToEditor
}
