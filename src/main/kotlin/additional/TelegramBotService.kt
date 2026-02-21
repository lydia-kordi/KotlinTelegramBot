package additional

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTICS_CLICKED = "show_statistics_clicked"

private const val CALLBACK_DATA_BACK = "back"
private const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

class TelegramBotService(
    private val botToken: String,
    private val trainer: LearnWordsTrainer,
) {

    private val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateID: Int): String {
        val url = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateID"
        val request = HttpRequest.newBuilder().uri(URI.create(url)).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun handleStart(chatId: String) {
        val response = buildMenuResponse()
        sendResponse(chatId, null, response)
    }

    fun handleCallback(
        chatId: String,
        messageId: Int,
        callbackData: String,
    ) {
        when {
            callbackData == LEARN_WORDS_CLICKED -> {
                checkNextQuestionAndSend(chatId)
            }

            callbackData == STATISTICS_CLICKED -> {
                val response = buildStatisticsResponse()
                sendResponse(chatId, messageId, response)
            }

            callbackData == CALLBACK_DATA_BACK -> {
                val response = buildMenuResponse()
                sendResponse(chatId, messageId, response)
            }

            callbackData.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
                val index = callbackData
                    .removePrefix(CALLBACK_DATA_ANSWER_PREFIX)
                    .toIntOrNull() ?: return

                val correctWord = trainer.getCorrectAnswer()
                val isCorrect = trainer.submitAnswer(index)
                val nextQuestion = trainer.startLearning()

                val response = buildLearningResponse(
                    isCorrect,
                    correctWord,
                    nextQuestion
                )

                sendResponse(chatId, messageId, response)
            }
        }
    }

    private fun checkNextQuestionAndSend(chatId: String) {


        if (!trainer.canStartStudy()) {
            val response = BotResponse(
                text = "📚 Для начала обучения добавьте не менее 4 слов в словарь.",
                keyboard = listOf(listOf(backButton()))
            )
            sendResponse(chatId, null, response)
            return
        }

        val question = trainer.startLearning()

        val response = if (question == null) {
            BotResponse(
                text = "🎉 Отлично! Все слова в словаре выучены 👏",
                keyboard = listOf(listOf(backButton()))
            )
        } else {
            BotResponse(
                text = buildQuestionText(question),
                keyboard = buildQuestionKeyboard(question)
            )
        }

        sendResponse(chatId, null, response)
    }

    private fun buildMenuResponse(): BotResponse {
        val text = "Выбери действие:"

        val keyboard = listOf(
            listOf(TelegramButton("📚 Учить слова", LEARN_WORDS_CLICKED)),
            listOf(TelegramButton("📊 Статистика", STATISTICS_CLICKED))
        )

        return BotResponse(text, keyboard)
    }

    private fun buildStatisticsResponse(): BotResponse {
        val statistics = trainer.getStatistics()

        val text =
            "Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%"

        val keyboard = listOf(listOf(backButton()))

        return BotResponse(text, keyboard)
    }

    private fun buildLearningResponse(
        isCorrect: Boolean,
        correctWord: Word?,
        nextQuestion: Question?,
    ): BotResponse {

        val resultText = buildAnswerResultText(isCorrect, correctWord)

        if (nextQuestion == null) {
            return BotResponse(
                text = "$resultText\n\n🎉 Все слова выучены! УРА!",
                keyboard = listOf(listOf(backButton()))
            )
        }

        val text = """
$resultText

${buildQuestionText(nextQuestion)}
        """.trimIndent()

        val keyboard = buildQuestionKeyboard(nextQuestion)

        return BotResponse(text, keyboard)
    }

    private fun buildAnswerResultText(
        isCorrect: Boolean,
        correctWord: Word?,
    ): String {
        return if (isCorrect) {
            "✅ Верно!"
        } else {
            "❌ Неверно!\n\n${correctWord?.text} — это ${correctWord?.translate}."
        }
    }

    private fun buildQuestionText(question: Question): String {
        return """
🤓 Как переводится слово <code>${question.correctAnswer.text}</code>?
        """.trimIndent()
    }

    private fun buildQuestionKeyboard(
        question: Question,
    ): List<List<TelegramButton>> {

        val answerRows = question.variants
            .mapIndexed { i, word ->
                TelegramButton(word.translate, "$CALLBACK_DATA_ANSWER_PREFIX$i")
            }
            .chunked(2)

        return answerRows + listOf(listOf(backButton()))
    }

    private fun backButton(): TelegramButton {
        return TelegramButton("🔙 Назад", CALLBACK_DATA_BACK)
    }

    fun sendResponse(
        chatId: String,
        messageId: Int?,
        response: BotResponse,
    ) {
        if (messageId != null) {
            editMessageWithKeyboard(chatId, messageId, response.text, response.keyboard)
        } else {
            sendMessageWithKeyboard(chatId, response.text, response.keyboard)
        }
    }

    private fun sendMessageWithKeyboard(
        chatId: String,
        text: String,
        keyboardRows: List<List<TelegramButton>>,
    ) {

        val keyboardJson = keyboardRows.joinToString(",") { it.toJsonRow() }

        val body = """
            {
              "chat_id": $chatId,
              "text": "$text",
              "parse_mode": "HTML",
              "reply_markup": {
                "inline_keyboard": [
                  $keyboardJson
                ]
              }
            }
        """.trimIndent()

        sendJson("$TELEGRAM_BASE_URL$botToken/sendMessage", body)
    }

    private fun editMessageWithKeyboard(
        chatId: String,
        messageId: Int,
        text: String,
        keyboardRows: List<List<TelegramButton>>,
    ) {

        val keyboardJson = keyboardRows.joinToString(",") { it.toJsonRow() }

        val body = """
            {
              "chat_id": $chatId,
              "message_id": $messageId,
              "text": "$text",
              "parse_mode": "HTML",
              "reply_markup": {
                "inline_keyboard": [
                  $keyboardJson
                ]
              }
            }
        """.trimIndent()

        sendJson("$TELEGRAM_BASE_URL$botToken/editMessageText", body)
    }

    private fun sendJson(url: String, body: String) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        client.send(request, HttpResponse.BodyHandlers.ofString())
    }
}

data class TelegramButton(
    val text: String,
    val callbackData: String,
)

data class BotResponse(
    val text: String,
    val keyboard: List<List<TelegramButton>>,
)

fun TelegramButton.toJson(): String = """
{
  "text": "$text",
  "callback_data": "$callbackData"
}
""".trimIndent()

fun List<TelegramButton>.toJsonRow(): String =
    "[${this.joinToString(",") { it.toJson() }}]"