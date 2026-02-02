package additional

fun printStatistics(dictionary: List<Word>) {
    val learnedWords = dictionary.filter { it.correctAnswersCount >= 3 }
    val totalCount = dictionary.size
    val learnedCount = learnedWords.size
    val percent = if (totalCount > 0) (learnedCount * 100) / totalCount else 0

    println("Выучено $learnedCount из $totalCount слов | $percent%")
    println()
}