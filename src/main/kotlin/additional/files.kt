package additional

import java.io.File
import java.io.IOException

fun main() {
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

    for (entry in dictionary) {
        println("${entry.text}|${entry.translate}|${entry.correctAnswersCount}")
    }
}