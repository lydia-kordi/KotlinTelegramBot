package additional

data class Word(
    val text: String,
    val translate: String,
    val correctAnswersCount: Int = 0,
)
