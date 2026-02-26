package additional

import java.io.File

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

data class Statistics(
    val learned: Int,
    val total: Int,
    val percent: Int,
)

data class AnswerResult(
    val isCorrect: Boolean,
    val correctWord: Word,
)

class LearnWordsTrainer(private val fileName: String) {

    private val dictionary: MutableList<Word> = loadDictionary()

    private sealed class LearningState {
        data object Idle : LearningState()
        data class Asking(val question: Question) : LearningState()
    }

    private var state: LearningState = LearningState.Idle

    fun canStartStudy(): Boolean = dictionary.size >= MIN_TOTAL_WORDS

    fun startLearning(): Question? {

        if (!canStartStudy()) return null

        val currentState = state
        if (currentState is LearningState.Asking) {
            return currentState.question
        }

        val notLearnedList = dictionary.filterNotLearnedWords()
        if (notLearnedList.isEmpty()) return null

        val question = getNextQuestion(notLearnedList)
        state = LearningState.Asking(question)

        return question
    }

    private fun loadDictionary(): MutableList<Word> {
        val userFile = File(fileName)

        if (!userFile.exists()) {
            val templateFile = File(WORDS_FILE_NAME)
            require(templateFile.exists()) {
                "Базовый словарь $WORDS_FILE_NAME не найден"
            }
            templateFile.copyTo(userFile)
        }

        return userFile.readLines().mapNotNull { it.toWord() }.toMutableList()
    }

    fun submitAnswer(index: Int): AnswerResult? {

        val currentState = state
        if (currentState !is LearningState.Asking) return null

        val question = currentState.question
        val selectedWord = question.variants.getOrNull(index) ?: return null

        val isCorrect = selectedWord == question.correctAnswer

        if (isCorrect) {
            question.correctAnswer.correctAnswersCount++
            saveDictionary()
        }

        state = LearningState.Idle

        return AnswerResult(
            isCorrect = isCorrect, correctWord = question.correctAnswer
        )
    }

    private fun saveDictionary() {
        File(fileName).printWriter().use { out ->
            dictionary.forEach {
                out.println("${it.text}|${it.translate}|${it.correctAnswersCount}")
            }
        }
    }

    fun getStatistics(): Statistics {
        val total = dictionary.size
        val learned = dictionary.count {
            it.correctAnswersCount >= MIN_RIGHT_ANSWERED
        }

        val percent = if (total > 0) (learned * 100) / total else 0

        return Statistics(
            learned = learned,
            total = total,
            percent = percent,
        )
    }

    private fun getNextQuestion(notLearnedList: List<Word>): Question {
        val correct = notLearnedList.random()

        val others = if (notLearnedList.size >= MIN_UNLEARNED_WORDS) notLearnedList.shuffledMinus(correct, 3)
        else dictionary.shuffledMinus(correct, 3)

        return Question(
            variants = (listOf(correct) + others).shuffled(),
            correctAnswer = correct,
        )
    }

    fun resetProgress() {
        dictionary.forEach {
            it.correctAnswersCount = 0
        }
        saveDictionary()
    }
}

fun List<Word>.filterNotLearnedWords() = filter { it.correctAnswersCount < MIN_RIGHT_ANSWERED }.toMutableList()

fun List<Word>.shuffledMinus(
    exclude: Word,
    limit: Int,
) = filter { it != exclude }.shuffled().take(limit)

fun String.toWord(): Word? {
    val parts = split("|")
    return if (parts.size >= 2) {
        Word(
            parts[0],
            parts[1],
            parts.getOrNull(2)?.toIntOrNull() ?: 0,
        )
    } else null
}