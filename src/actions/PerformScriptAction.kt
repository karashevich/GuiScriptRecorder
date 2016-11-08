package actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import compile.KotlinCompileUtil

/**
 * @author Sergey Karashevich
 */

class PerformScriptAction: AnAction(){

    companion object{
        val LOG = Logger.getInstance(PerformScriptAction::class.java)
    }

    override fun actionPerformed(p0: AnActionEvent?) {
        LOG.info("Compile and evaluate current script buffer")
        KotlinCompileUtil.compileAndEvalScriptBuffer()
    }

}
