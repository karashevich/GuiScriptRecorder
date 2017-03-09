package ui

import ScriptGenerator
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.ProjectManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.EditorTextField

/**
 * @author Sergey Karashevich
 */
class GuiScriptEditor() {

    var myEditor: EditorEx? = null
    var syncEditor = false

    fun getPanel() = myEditor!!.component

    init {
        val editorFactory = EditorFactory.getInstance()
        val editorDocument = editorFactory.createDocument(ScriptGenerator.getScriptBuffer())
        val editor = (editorFactory.createEditor(editorDocument, ProjectManager.getInstance().defaultProject) as EditorEx)
        EditorTextField.SUPPLEMENTARY_KEY.set(editor, java.lang.Boolean.TRUE)
        editor.colorsScheme = EditorColorsManager.getInstance().globalScheme
        val settings = editor.settings
        settings.isLineNumbersShown = true
        settings.isWhitespacesShown = true
        settings.isLineMarkerAreaShown = false
        settings.isIndentGuidesShown = false
        settings.isFoldingOutlineShown = false
        settings.additionalColumnsCount = 0
        settings.additionalLinesCount = 0
        settings.isRightMarginShown = true

        val pos = LogicalPosition(0, 0)
        editor.caretModel.moveToLogicalPosition(pos)
        editor.highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(LightVirtualFile("a.kt"), editor.colorsScheme, null)

        myEditor = editor

        //let editor be synchronised by default
        syncEditor = true
        val editorImpl = (myEditor as EditorImpl)

//        Disposer.register(editorImpl.disposable, Disposable {
//            GuiRecorderComponent.getFrame()!!.getGuiScriptEditorPanel().createAndAddGuiScriptEditor()
//            editorImpl
//        })
    }

    //Editor should be realised before Application is closed
    fun releaseEditor() {
        EditorFactory.getInstance().releaseEditor(myEditor!!)
        myEditor = null
    }

}