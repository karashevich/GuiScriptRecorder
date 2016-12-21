package util

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ui.JBUI
import java.awt.Font

/**
 * @author Sergey Karashevich
 */
object ClassLoaderUtil {

    val pluginClassLoader by lazy { compile.DaemonCompiler::class.java.classLoader }

    val ideaClassLoader by lazy { com.intellij.openapi.application.impl.ApplicationImpl::class.java.classLoader }

    fun getJBFontForLabel(): Font {
        val jbuiFontsClassName = JBUI.Fonts::class.java.name
        val jbuiFontsClass = ideaClassLoader.loadClass(jbuiFontsClassName)
        val methodWithParamsLabel = jbuiFontsClass.getMethod("label", Float::class.java)
        val methodLabel = jbuiFontsClass.getMethod("label")

        return if (SystemInfo.isMac) methodWithParamsLabel.invoke(null, 11f) as Font else methodLabel.invoke(null) as Font
    }

    fun getEditorHighLighter(editor: EditorEx): EditorHighlighter {
        val vfIdeaCls = ideaClassLoader.loadClass(VirtualFile::class.java.name)
        val prjClass = ideaClassLoader.loadClass(Project::class.java.name)
        //editor.highlighter = EditorHighlighterFactory.getInstance()
        // .createEditorHighlighter(ClassLoaderUtil.getLightVirtualFile("a.kt"), editor.colorsScheme, null)

        val editorHighlighterFactory = EditorHighlighterFactory.getInstance()
        val createEditorHighlighterMethod = editorHighlighterFactory.javaClass.getMethod("createEditorHighlighter", vfIdeaCls, EditorColorsScheme::class.java, prjClass)
        return createEditorHighlighterMethod.invoke(editorHighlighterFactory, getLightVirtualFile("a.kt"), editor.colorsScheme, null) as EditorHighlighter

    }

    //LightVirtualFile("a.kt")
    fun getLightVirtualFile(name: String): Any? {
        val lightVirtualFileClass = ideaClassLoader.loadClass(LightVirtualFile::class.java.name)
        return lightVirtualFileClass.getConstructor(String::class.java).newInstance(name)
    }

    //    IdeEventQueue.getInstance().addDispatcher(globalAwtProcessor, ApplicationManagerEx.getApplication())
    fun addDispatcherToIdeEventQueue(processor: IdeEventQueue.EventDispatcher) {

        val ideaDisposableClass = ideaClassLoader.loadClass(com.intellij.openapi.Disposable::class.java.name)
        val ideEventQueue = IdeEventQueue.getInstance()
        val ideEventQueueClass = ideEventQueue.javaClass

        val appManagerClass = ideaClassLoader.loadClass(ApplicationManagerEx::class.java.name)
        val appInstance = appManagerClass.getMethod("getApplicationEx").invoke(null)

        val addDispatcherMethod = ideEventQueueClass.getMethod("addDispatcher", IdeEventQueue.EventDispatcher::class.java, ideaDisposableClass)
        addDispatcherMethod.invoke(ideEventQueue, processor, appInstance)
    }

    fun runWriteCommandAction(runnable: Runnable){
        //WriteCommandAction.runWriteCommandAction(null, { document.insertString(document.textLength, str) })
        val prjClass = ideaClassLoader.loadClass(Project::class.java.name)
        WriteCommandAction::class.java.getMethod("runWriteCommandAction", prjClass, java.lang.Runnable::class.java).invoke(null, null, runnable)
    }

}