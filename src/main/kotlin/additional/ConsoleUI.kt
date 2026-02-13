package additional

interface UserInterface {
    fun displayMessage(message: String)
    fun displayQuestion(text: String, options: List<String>)
    fun getUserInput(): Int?
}

class ConsoleUI : UserInterface {
    override fun displayMessage(message: String) {
        println(message)
    }

    override fun displayQuestion(text: String, options: List<String>) {
        println("\n$text:")
        options.forEachIndexed { index, option ->
            println("${index + 1} - $option")
        }
        println(" ----------\n0 - Меню")
    }

    override fun getUserInput(): Int? {
        print("Введи номер ответа: ")
        return readlnOrNull()?.trim()?.toIntOrNull()
    }
}