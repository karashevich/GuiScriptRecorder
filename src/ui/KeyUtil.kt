package ui

import java.awt.event.KeyEvent.*

/**
 * @author Sergey Karashevich
 */
object KeyUtil {

    fun patch(ch: Char) =
            when (ch) {
                '\b' -> getKeyText(VK_BACK_SPACE)
                '\t' -> getKeyText(VK_TAB)
                '\n' -> getKeyText(VK_ENTER)
                '\u0018' -> getKeyText(VK_CANCEL)
                '\u001b' -> getKeyText(VK_ESCAPE)
                '\u007f' -> getKeyText(VK_DELETE)
                else -> ch.toString()
            }
}