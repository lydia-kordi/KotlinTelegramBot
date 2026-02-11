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

class StudyTrainer(private val dictionary: MutableList<Word>, private val ui: UserInterface) {
    fun start() {
        if (!canStartStudy()) return

        while (true) {
            val notLearnedList = dictionary.filterNotLearnedWords()
            if (notLearnedList.isEmpty()) {
                ui.displayMessage("Поздравляем! Все слова в словаре выучены!")
                return
            }

            val correctAnswer = notLearnedList.random()
            val questionWords = createQuestionWords(notLearnedList, dictionary, correctAnswer)

            ui.displayQuestion(correctAnswer.text, questionWords.map { it.translate })

            when (val userInput = ui.getUserInput()) {
                0 -> return
                in 1..(questionWords.size) -> userInput?.let {
                    handleAnswer(questionWords[it - 1], correctAnswer)
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

    private fun handleAnswer(selectedWord: Word, correctAnswer: Word) {
        if (selectedWord.translate == correctAnswer.translate) {
            ui.displayMessage("Правильно!")
            correctAnswer.correctAnswersCount++
            saveWordsToFile(dictionary, "words.txt")
        } else {
            ui.displayMessage("Неправильно! ${correctAnswer.text} – это ${correctAnswer.translate}")
        }
    }

    private fun createQuestionWords(
        notLearnedList: List<Word>,
        dictionary: List<Word>,
        correctAnswer: Word,
    ): List<Word> {
        val remainingWords = if (notLearnedList.size > MIN_UNLEARNED_WORDS) {
            notLearnedList.shuffledMinus(correctAnswer, 3)
        } else {
            dictionary.shuffledMinus(correctAnswer, 3)
        }
        return (listOf(correctAnswer) + remainingWords).shuffled()
    }
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

