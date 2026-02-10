package additional

import java.io.File
import java.io.IOException

const val MIN_TOTAL_WORDS = 4
const val MIN_RIGHT_ANSWERED = 3
const val MIN_UNLEARNED_WORDS = 3

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
    val learnedWords = dictionary.filter { it.correctAnswersCount >= MIN_RIGHT_ANSWERED }
    val totalCount = dictionary.size
    val learnedCount = learnedWords.size
    val percent = if (totalCount > 0) (learnedCount * 100) / totalCount else 0

    println("Выучено $learnedCount из $totalCount слов | $percent%")
    println()
}

fun studyWords(dictionary: MutableList<Word>) {
    if (dictionary.size < MIN_TOTAL_WORDS) {
        println("В словаре недостаточно слов для начала изучения. Добавь минимум $MIN_TOTAL_WORDS слов(а).")
        return
    }

    while (true) {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < MIN_RIGHT_ANSWERED }.toMutableList()

        if (notLearnedList.isEmpty()) {
            println()
            println("Поздравляем! Все слова в словаре выучены!")
            println()
            return
        }

        val correctAnswer = notLearnedList.random()
        val questionWords = mutableListOf(correctAnswer)

        val remainingWords = if (notLearnedList.size > MIN_UNLEARNED_WORDS) {
            notLearnedList.filter { it != correctAnswer }.shuffled().take(3)
        } else {
            dictionary.filter { it != correctAnswer }.shuffled().take(3)
        }

        questionWords.addAll(remainingWords)
        questionWords.shuffle()

        println()
        println("${correctAnswer.text}:")
        questionWords.mapIndexed { index, word ->
            println("${index + 1} - ${word.translate}")
        }
        println(" ----------")
        println("0 - Меню")

        print("Введи номер ответа: ")
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
                println("Неправильно! ${correctAnswer.text} – это ${correctAnswer.translate}")
            }
        } else {
            println("Введи номер ответа от 1 до 4.")
        }
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