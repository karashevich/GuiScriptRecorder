package ui

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import util.ClassLoaderUtil

/**
 * @author Sergey Karashevich
 */
class GuiScriptEditor {

    var myEditor: EditorEx? = null
    var syncEditor = false

    fun getPanel() = myEditor!!.component

    init {
        val editorFactory = EditorFactory.getInstance()
        val editorDocument = editorFactory.createDocument("")
        val editor = (editorFactory.createEditor(editorDocument) as EditorEx)
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
        editor.highlighter = ClassLoaderUtil.getEditorHighLighter(editor)

        myEditor = editor

        //let editor be synchronised by default
        syncEditor = true
    }

    //Editor should be realised before Application is closed
    fun releaseEditor() {
        EditorFactory.getInstance().releaseEditor(myEditor!!)
        myEditor = null
    }

}