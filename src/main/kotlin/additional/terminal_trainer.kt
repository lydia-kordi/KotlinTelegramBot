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

fun printStatistics(dictionary: MutableList<Word>) {
    val learnedWords = dictionary.filter { it.correctAnswersCount >= 3 }
    val totalCount = dictionary.size
    val learnedCount = learnedWords.size
    val percent = if (totalCount > 0) (learnedCount * 100) / totalCount else 0

    println("Выучено $learnedCount из $totalCount слов | $percent%")
    println()
}

fun studyWords(dictionary: MutableList<Word>) {
    while (dictionary.any { it.correctAnswersCount < 3 }) {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < 3 }
        if (notLearnedList.size < 4) {
            println("Недостаточно слов для выбора вариантов. Возможно, нужно добавить больше слов.")
            return
        }

        val questionWords = notLearnedList.shuffled().take(4).toMutableList()
        val correctAnswer = questionWords.random()

        println()
        println("${correctAnswer.text}:")
        questionWords.mapIndexed { index, word -> println("${index + 1} - ${word.translate}") }

        print("Введите номер ответа: ")
        val userInput = readLine()?.toIntOrNull()

        if (userInput != null && userInput in 1..4) {
            val selectedTranslation = questionWords[userInput - 1].translate
            if (selectedTranslation == correctAnswer.translate) {
                println("Правильно!")
                correctAnswer.correctAnswersCount += 1
            } else {
                println("Неправильно. Правильный ответ: ${correctAnswer.translate}")
            }
        } else {
            println("Введите число от 1 до 4.")
        }

        if (dictionary.all { it.correctAnswersCount >= 3 }) {
            println("Все слова в словаре выучены.")
            return
        }
    }
}