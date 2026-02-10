package additional

import java.io.File
import java.io.IOException

fun loadDictionary(): MutableList<Word> {
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
    if (dictionary.size < 4) {
        println("В словаре недостаточно слов для начала изучения. Добавьте минимум 4 слова.")
        return
    }

    while (true) {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < 3 }.toMutableList()

        if (notLearnedList.isEmpty()) {
            println()
            println("Поздравляем! Все слова в словаре выучены!")
            println()
            return
        }

        var questionWords = notLearnedList.toMutableList()

        if (questionWords.size < 4) {
            val learnedList = dictionary.filter { it.correctAnswersCount >= 3 }
            val additionalWords = learnedList.shuffled().take(4 - questionWords.size)
            questionWords.addAll(additionalWords)
        }

        questionWords = questionWords.shuffled().take(4).toMutableList()
        val correctAnswer = questionWords.random()

        println()
        println("${correctAnswer.text}:")
        questionWords.mapIndexed { index, word ->
            println("${index + 1} - ${word.translate}")
        }
        println(" ----------")
        println("0 - Меню")

        print("Введите номер ответа: ")
        val userInput = readlnOrNull()?.trim()?.toIntOrNull()

        if (userInput == 0) {
            println("Возврат в главное меню.")
            return
        }

        if (userInput != null && userInput in 1..questionWords.size) {
            val selectedTranslation = questionWords[userInput - 1].translate
            if (selectedTranslation == correctAnswer.translate) {
                println("Правильно!")
                correctAnswer.correctAnswersCount += 1

                saveWordsToFile(dictionary)
            } else {
                println("Неправильно. Правильный ответ: ${correctAnswer.translate}")
            }
        } else {
            println("Введи вариант ответа от 1 до 4.")
        }
        println()
        println("Поздравляю! Ты выучил все слова в словаре!")
        println()
    }
}

fun saveWordsToFile(dictionary: List<Word>) {
    val wordsFile = File("words.txt")
    wordsFile.printWriter().use { out ->
        dictionary.forEach { word ->
            out.println("${word.text}|${word.translate}|${word.correctAnswersCount}")
        }
    }
}