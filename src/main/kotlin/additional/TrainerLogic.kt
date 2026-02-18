package additional

import java.io.File
import java.io.IOException

const val MIN_TOTAL_WORDS = 4
const val MIN_RIGHT_ANSWERED = 3
const val MIN_UNLEARNED_WORDS = 3
const val WORDS_FILE_NAME = "words.txt"

data class Word(
    val text: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)

data class Statistics(val learned: Int, val total: Int)

fun List<Word>.filterNotLearnedWords() =
    filter { it.correctAnswersCount < MIN_RIGHT_ANSWERED }.toMutableList()

fun List<Word>.shuffledMinus(exclude: Word, limit: Int) =
    filter { it != exclude }.shuffled().take(limit)

fun String.toWord(): Word? {
    val parts = split("|")
    return if (parts.size >= 2) {
        Word(
            parts[0],
            parts[1],
            parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
    } else null
}

class LearnWordsTrainer(private val dictionary: MutableList<Word>) {

    private var currentQuestion: Question? = null

    fun canStartStudy(): Boolean =
        dictionary.size >= MIN_TOTAL_WORDS

    fun startLearning(): Question? {
        if (!canStartStudy()) return null

        val notLearnedList = dictionary.filterNotLearnedWords()

        if (notLearnedList.isEmpty()) return null

        val question = getNextQuestion(notLearnedList)
        currentQuestion = question
        return question
    }

    fun submitAnswer(index: Int): Boolean {
        val question = currentQuestion ?: return false
        val selected = question.variants.getOrNull(index) ?: return false

        val isCorrect = selected.translate == question.correctAnswer.translate

        if (isCorrect) {
            question.correctAnswer.correctAnswersCount++
            dictionary.saveToFile()
        }

        return isCorrect
    }

    fun getCorrectAnswer(): Word? =
        currentQuestion?.correctAnswer

    fun getStatistics(): Statistics {
        val learned = dictionary.count { it.correctAnswersCount >= MIN_RIGHT_ANSWERED }
        return Statistics(learned, dictionary.size)
    }

    private fun getNextQuestion(notLearnedList: List<Word>): Question {
        val correct = notLearnedList.random()

        val others =
            if (notLearnedList.size > MIN_UNLEARNED_WORDS)
                notLearnedList.shuffledMinus(correct, 3)
            else
                dictionary.shuffledMinus(correct, 3)

        return Question((listOf(correct) + others).shuffled(), correct)
    }
}

fun loadDictionary(fileName: String): MutableList<Word> {
    val wordsFile = File(fileName)
    return try {
        wordsFile.readLines().mapNotNull { it.toWord() }.toMutableList()
    } catch (e: IOException) {
        println("Ошибка чтения файла: ${e.message}")
        mutableListOf()
    }
}

fun List<Word>.saveToFile(fileName: String = WORDS_FILE_NAME) {
    File(fileName).printWriter().use { out ->
        forEach {
            out.println("${it.text}|${it.translate}|${it.correctAnswersCount}")
        }
    }
}
