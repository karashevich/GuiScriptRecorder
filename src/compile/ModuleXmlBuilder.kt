package compile

/**
 * @author Sergey Karashevich
 */
object ModuleXmlBuilder{

    fun module(function: () -> String): String = "<modules>\n${function.invoke()}\n</modules>"

    fun modules(outputDir: String, function: () -> String): String = "<module name=\"main\" outputDir=\"$outputDir\" type=\"java-production\">\n${function.invoke()}\n</module>"

    fun addSource(path: String) = "<sources path=\"$path\"/>"

    fun addClasspath(path: String) = "<classpath path=\"$path\"/>"


    fun build(outputDir: String, classPath: List<String>) =
            module {
            modules(outputDir =  outputDir) {
              classPath.map {path -> addClasspath(path)}.joinToString("\n")
            }
        }
}