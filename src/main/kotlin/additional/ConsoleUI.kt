package additional

interface UserInterface {
    fun displayMessage(message: String)
    fun getUserInput(): Int?
}

class ConsoleUI : UserInterface {

    override fun displayMessage(message: String) {
        println(message)
    }

    override fun getUserInput(): Int? {
        print("Введи номер ответа: ")
        return readlnOrNull()?.trim()?.toIntOrNull()
    }
}