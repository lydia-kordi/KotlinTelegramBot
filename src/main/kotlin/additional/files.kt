package additional

import java.io.File
import java.io.IOException

fun main() {
    val wordsFile = File("words.txt")

    try {
        val lines = wordsFile.readLines()
        for (line in lines) {
            println(line)
        }
    } catch (e: IOException) {
        println("Ошибка при чтении файла: ${e.message}")
    }
}