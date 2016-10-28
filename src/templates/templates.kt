package templates

/**
 * @author Sergey Karashevich
 */
fun wrapWithTestMethod(body: String) = "fun testCurrent() { $body }"