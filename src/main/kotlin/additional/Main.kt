package additional

fun main() {
    val dictionary = loadDictionary("words.txt")
    val ui = ConsoleUI()
    val trainer = LearnWordsTrainer(dictionary, ui)

    while (true) {
        println("Меню:")
        println("1 - Учить слова")
        println("2 - Статистика")
        println("0 - Выход")
        print("Введите число: ")

        val input = readlnOrNull()

        when (input) {
            "1" -> {
                println()
                println("Ты выбрал 'Учить слова'.")
                trainer.start()
            }

            "2" -> {
                println()
                printStatistics(dictionary)
            }

            "0" -> {
                println()
                println("Выход из программы.")
                return
            }

            else -> println("Введи число 1, 2 или 0.")
        }
    }
}