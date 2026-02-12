package additional

import java.io.File
import java.io.IOException

const val MIN_TOTAL_WORDS = 4
const val MIN_RIGHT_ANSWERED = 3
const val MIN_UNLEARNED_WORDS = 3

data class Word(
    val text: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)

fun List<Word>.filterNotLearnedWords() = filter { it.correctAnswersCount < MIN_RIGHT_ANSWERED }.toMutableList()

fun List<Word>.shuffledMinus(exclude: Word, limit: Int) = filter { it != exclude }.shuffled().take(limit)

fun String.toWord(): Word? {
    val parts = split("|")
    return if (parts.size >= 2) {
        val text = parts[0]
        val translate = parts[1]
        val correctAnswersCount = parts.getOrNull(2)?.toIntOrNull() ?: 0
        Word(text, translate, correctAnswersCount)
    } else {
        null
    }
}

class LearnWordsTrainer(private val dictionary: List<Word>, private val ui: UserInterface) {

    fun start() {
        if (!canStartStudy()) return

        while (true) {
            val notLearnedList = dictionary.filterNotLearnedWords()
            if (notLearnedList.isEmpty()) {
                ui.displayMessage("Поздравляем! Все слова в словаре выучены!")
                return
            }

            val question = getNextQuestion(notLearnedList)
            ui.displayQuestion(question.correctAnswer.text, question.variants.map { it.translate })

            when (val userInput = ui.getUserInput()) {
                0 -> return
                in 1..(question.variants.size) -> {
                    userInput?.let {
                        checkAnswer(question.variants[it - 1], question.correctAnswer)
                    }
                }
                else -> ui.displayMessage("Введи номер ответа от 1 до 4.")
            }
        }
    }

    private fun canStartStudy(): Boolean {
        return if (dictionary.size < MIN_TOTAL_WORDS) {
            ui.displayMessage("В словаре недостаточно слов для начала изучения. Добавь минимум $MIN_TOTAL_WORDS слов(а).")
            false
        } else {
            true
        }
    }

    private fun checkAnswer(selectedWord: Word, correctAnswer: Word) {
        if (selectedWord.translate == correctAnswer.translate) {
            ui.displayMessage("Правильно!")
            correctAnswer.correctAnswersCount++
            saveWordsToFile(dictionary, "words.txt")
        } else {
            ui.displayMessage("Неправильно! ${correctAnswer.text} – это ${correctAnswer.translate}")
        }
    }

    private fun getNextQuestion(notLearnedList: List<Word>): Question {
        val correctAnswer = notLearnedList.random()
        val remainingWords = if (notLearnedList.size > MIN_UNLEARNED_WORDS) {
            notLearnedList.shuffledMinus(correctAnswer, 3)
        } else {
            dictionary.shuffledMinus(correctAnswer, 3)
        }
        val variants = (listOf(correctAnswer) + remainingWords).shuffled()
        return Question(variants, correctAnswer)
    }
}

fun printStatistics(dictionary: MutableList<Word>) {
    val learnedWords = dictionary.filter { it.correctAnswersCount >= MIN_RIGHT_ANSWERED }
    val totalCount = dictionary.size
    val learnedCount = learnedWords.size
    val percent = if (totalCount > 0) (learnedCount * 100) / totalCount else 0

    println("Выучено $learnedCount из $totalCount слов | $percent%")
    println()
}

fun loadDictionary(fileName: String): MutableList<Word> {
    val wordsFile = File(fileName)
    return try {
        wordsFile.readLines().mapNotNull { it.toWord() }.toMutableList()
    } catch (e: IOException) {
        println("Ошибка при чтении файла: ${e.message}")
        mutableListOf()
    }
}

fun saveWordsToFile(dictionary: List<Word>, fileName: String) {
    File(fileName).printWriter().use { out ->
        dictionary.forEach { word ->
            out.println("${word.text}|${word.translate}|${word.correctAnswersCount}")
        }
    }
}