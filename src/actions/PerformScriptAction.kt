package actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import compile.KotlinCompileUtil
import components.GuiRecorderComponent
import ui.Notifier
import java.util.function.Consumer

/**
 * @author Sergey Karashevich
 */

class PerformScriptAction: AnAction(null, "Run GUI Script", AllIcons.Actions.Execute){

    companion object{
        val LOG = Logger.getInstance(PerformScriptAction::class.java)
    }

    override fun actionPerformed(p0: AnActionEvent?) {
        LOG.info("Compile and evaluate current script buffer")
        Notifier.updateStatus("<long>Compiling and performing current script")
        val editor = GuiRecorderComponent.getEditor()

        //we wrapping it in lambda consumer because of different classloader problem in CompileDaemon class.
        val myNotifier: Consumer<String> = Consumer<String> { statusMessage -> Notifier.updateStatus(statusMessage) }

//        ApplicationManager.getApplication().executeOnPooledThread { KotlinCompileUtil.compileAndEvalCodeWithNotifier(editor.document.text, myNotifier) }
        KotlinCompileUtil.compileAndRun(editor.document.text)
    }

}
