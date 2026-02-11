package additional

fun main() {
    val dictionary = loadDictionary("words.txt")
    val ui = ConsoleUI()
    val trainer = StudyTrainer(dictionary, ui)
    trainer.start()
}