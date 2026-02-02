package additional

import java.io.File
import java.io.IOException

fun loadDictionary(): List<Word> {
    val wordsFile = File("words.txt")
    val dictionary = mutableListOf<Word>()

    try {
        val lines = wordsFile.readLines()
        for (line in lines) {
            val parts = line.split("|")
            if (parts.size >= 2) {
                val text = parts[0]
                val translate = parts[1]
                val correctAnswersCount = parts.getOrNull(2)?.toIntOrNull() ?: 0

                val wordEntry = Word(text, translate, correctAnswersCount)
                dictionary.add(wordEntry)
            }
        }
    } catch (e: IOException) {
        println("Ошибка при чтении файла: ${e.message}")
    }
    return dictionary
}

fun main() {
    val dictionary = loadDictionary()

    while (true) {
        println("Меню:")
        println("1 - Учить слова")
        println("2 - Статистика")
        println("0 - Выход")
        print("Введите число: ")

        val input = readlnOrNull()

        when (input) {
            "1" -> println("Вы выбрали 'Учить слова'.")
            "2" -> println("Вы выбрали 'Статистика'.")
            "0" -> {
                println("Выход из программы.")
                break
            }

            else -> println("Введите число 1, 2 или 0.")
        }
    }
}