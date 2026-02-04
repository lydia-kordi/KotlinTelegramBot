package additional

data class Word(
    val text: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)
