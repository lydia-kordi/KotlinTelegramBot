package additional

fun main() {
    val dictionary = loadDictionary()

    while (true) {
        println("Меню:")
        println("1 - Учить слова")
        println("2 - Статистика")
        println("0 - Выход")
        print("Введи число: ")

        val input = readlnOrNull()

        when (input) {
            "1" -> {
                println()
                println("Ты выбрал 'Учить слова'.")
                studyWords(dictionary)
            }

            "2" -> {
                println()
                printStatistics(dictionary)
            }

            "0" -> {
                println("Выход из программы.")
                return
            }

            else -> println("Введите число 1, 2 или 0.")
        }
    }
}