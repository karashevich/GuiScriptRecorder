package ui

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.editor.impl.EditorFactoryImpl
import com.intellij.openapi.editor.impl.EditorImpl
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JPanel

/**
 * @author Sergey Karashevich
 */
open class LightEditorWindow(editor: LightEditor){

    val frameName = "GUI Script Editor"
    val myFrame: JFrame
    val cr: ComponentResizer

    init {
        myFrame = JFrame(frameName)
        myFrame.isUndecorated = true // Remove title bar
        myFrame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        myFrame.setSize(350, 800)

        cr = ComponentResizer();
        cr.registerComponent(myFrame);
        cr.snapSize = Dimension(10, 10);
        cr.maximumSize = Dimension(10000, 1000);
        cr.minimumSize = Dimension(100, 100);

        val myContentPane = JPanel()
        myContentPane.layout = BoxLayout(myContentPane, BoxLayout.X_AXIS)
        myContentPane.size = Dimension(350, 800)
        myContentPane.add(editor.getContentComponent())

        myFrame.contentPane = myContentPane
        myFrame.pack()
        myFrame.isVisible = true
    }

    fun setVisible(visibility: Boolean) { myFrame.isVisible = visibility }
    fun isVisible() = myFrame.isVisible

}

class LightEditor() {

    val myDocument: Document
    val myEditor: Editor

    init{
        myDocument = EditorFactoryImpl.getInstance().createDocument("Here is a sample text.")
        myEditor = EditorFactoryImpl.getInstance().createEditor(myDocument)
    }

    fun getComponent() = myEditor.component
    fun getContentComponent() = myEditor.contentComponent
}