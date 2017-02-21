import com.intellij.testGuiFramework.generators.*
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.*

/**
 * @author Sergey Karashevich
 */


object ContextChecker {
    private val globalContextGenerators: List<ContextCodeGenerator<*>> = Generators.getGlobalContextGenerators()
    private val localContextGenerators: List<ContextCodeGenerator<*>> = Generators.getLocalContextCodeGenerator()
    private val writer: (String) -> Unit = {code -> ScriptGenerator.addToScript(code)}
    private val contextTree: ContextTree = ContextTree(writer)

    private fun getGlobalApplicableContext(component: Component, me: MouseEvent, cp: Point): Context? {
        val applicableContextGenerator = globalContextGenerators.filter { generator -> generator.accept(component) }.sortedByDescending(ContextCodeGenerator<*>::priority).firstOrNull() ?: return null
        return applicableContextGenerator.buildContext(component, me, cp)
    }

    private fun getLocalApplicableContext(component: Component, me: MouseEvent, cp: Point): Context? {
        val applicableContextGenerator = localContextGenerators.filter { generator -> generator.accept(component) }.sortedByDescending(ContextCodeGenerator<*>::priority).firstOrNull() ?: return null
        return applicableContextGenerator.buildContext(component, me, cp)
    }

    fun getContextDepth(): Int = contextTree.getSize()

    fun clearContext() {
        contextTree.clear()
    }

    fun checkContext(component: Component, me: MouseEvent, cp: Point) {
        val globalContext = getGlobalApplicableContext(component, me, cp)
        val localContext = getLocalApplicableContext(component, me, cp)
        if (globalContext != null) contextTree.addContext(globalContext, me)
        if (localContext != null) contextTree.addContext(localContext, me)
    }

    private class ContextTree(val writeFun: (String) -> Unit) {

        private val myContextsTree: ArrayList<Context> = ArrayList()
        private var lastContext: Context? = null

        private fun Context.isTheSameContext(): Boolean {
            if (myContextsTree.size == 0) return false
            when (this.originalGenerator) {
                is GlobalContextCodeGenerator -> {
                    val lastGlobalContext: Context = getLastGlobalContext() ?: return false
                    return lastGlobalContext.component == this.component
                }
                is LocalContextCodeGenerator -> {
                    if (lastContext!!.originalGenerator is GlobalContextCodeGenerator) return false
                    return this.component == lastContext!!.component
                }
                else -> throw UnsupportedOperationException("Error: Unidentified context generator type!")
            }

        }

        private fun getLastGlobalContext(): Context? = myContextsTree.filter { context -> context.originalGenerator is GlobalContextCodeGenerator }.lastOrNull()

        private fun removeLastContext() {
            if (myContextsTree.isEmpty()) throw Exception("Error: unable to remove context from empty context tree")
            if (myContextsTree.size == 1) {
                myContextsTree.clear()
                lastContext = null
            } else {
                myContextsTree.removeAt(myContextsTree.lastIndex)
                lastContext = myContextsTree.elementAt(myContextsTree.lastIndex)
            }
            writeFun("}")
        }

        private fun checkAliveContexts(me: MouseEvent) {
            for (i in (0..myContextsTree.lastIndex)) {
                if (!myContextsTree.get(i).isAlive(me)) {
                    // from i to myContextsTree.lastIndex contexts should be dropped
                    while (myContextsTree.lastIndex >= i) removeLastContext()
                    break
                }
            }
        }

        private fun Context.isAlive(me: MouseEvent): Boolean {
            when (this.originalGenerator) {
                is GlobalContextCodeGenerator -> {
                    return (this.component.isShowing && this.component.isEnabled)
                }
                is LocalContextCodeGenerator -> {
                    return (this.component.isEnabled && this.component.isShowing && this.component.contains(me.point))
                }
                else -> throw UnsupportedOperationException("Error: Unidentified context generator type!")
            }
        }

        fun addContext(context: Context, me: MouseEvent) {
            checkAliveContexts(me)
            if (context.isTheSameContext()) return //do nothing if the context is the same
            lastContext = context
            writeFun(context.code)
            myContextsTree.add(context)
        }

        fun getSize() = myContextsTree.size

        fun clear() {
            lastContext = null
            myContextsTree.clear()
        }

    }

}


