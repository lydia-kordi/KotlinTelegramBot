package additional

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTICS_CLICKED = "show_statistics_clicked"
const val RESET_CLICKED = "reset_progress_clicked"

private const val CALLBACK_DATA_BACK = "back"
private const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

@Serializable
data class UpdateResponse(
    val ok: Boolean,
    val result: List<Update>,
)

@Serializable
data class Update(
    @SerialName("update_id") val updateId: Long,
    val message: Message? = null,
    @SerialName("callback_query") val callbackQuery: CallbackQuery? = null,
)

@Serializable
data class Message(
    @SerialName("message_id") val messageId: Long,
    val chat: Chat,
    val text: String? = null,
)

@Serializable
data class Chat(
    val id: Long,
)

@Serializable
data class CallbackQuery(
    val data: String,
    val message: Message,
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id") val chatId: Long,
    val text: String,
    @SerialName("parse_mode") val parseMode: String = "HTML",
    @SerialName("reply_markup") val replyMarkup: ReplyMarkup,
)

@Serializable
data class EditMessageRequest(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("message_id") val messageId: Int,
    val text: String,
    @SerialName("parse_mode") val parseMode: String = "HTML",
    @SerialName("reply_markup") val replyMarkup: ReplyMarkup,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard") val inlineKeyboard: List<List<InlineKeyboardButton>>,
)

@Serializable
data class InlineKeyboardButton(
    val text: String,
    @SerialName("callback_data") val callbackData: String,
)

private val json = Json { encodeDefaults = true }

class TelegramBotService(private val botToken: String) {

    private val trainers: HashMap<Long, LearnWordsTrainer> = HashMap()

    private val client: HttpClient = HttpClient.newBuilder().build()

    private fun getTrainer(chatId: Long): LearnWordsTrainer =
        trainers.getOrPut(chatId) {
            LearnWordsTrainer("words_$chatId.txt")
        }

    fun getUpdates(updateID: Int): String {
        val url = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateID"
        val request = HttpRequest.newBuilder().uri(URI.create(url)).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun handleStart(chatId: Long) {
        val response = buildMenuResponse()
        sendResponse(chatId, null, response)
    }

    fun handleCallback(
        chatId: Long,
        messageId: Int,
        callbackData: String,
    ) {
        when {
            callbackData == LEARN_WORDS_CLICKED -> {
                checkNextQuestionAndSend(chatId)
            }

            callbackData == RESET_CLICKED -> {
                val trainer = getTrainer(chatId)
                trainer.resetProgress()

                val response = buildMenuResponse().copy(
                    text = "♻️ Прогресс успешно сброшен!\n\nВыбери действие:"
                )

                sendResponse(chatId, messageId, response)
            }

            callbackData == STATISTICS_CLICKED -> {
                val response = buildStatisticsResponse(chatId)
                sendResponse(chatId, messageId, response)
            }

            callbackData == CALLBACK_DATA_BACK -> {
                val response = buildMenuResponse()
                sendResponse(chatId, messageId, response)
            }

            callbackData.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
                val index = callbackData.removePrefix(CALLBACK_DATA_ANSWER_PREFIX).toIntOrNull() ?: return

                val trainer = getTrainer(chatId)

                val result = trainer.submitAnswer(index)
                if (result == null) {
                    val response = BotResponse(
                        text = "Сессия устарела. Начни обучение заново 🙂", keyboard = listOf(listOf(backButton()))
                    )
                    sendResponse(chatId, messageId, response)
                    return
                }

                val nextQuestion = trainer.startLearning()

                val resultText = buildAnswerResultText(
                    result.isCorrect, result.correctWord
                )

                val response = if (nextQuestion == null) {
                    BotResponse(
                        text = "$resultText\n\n🎉 Все слова выучены! УРА!", keyboard = listOf(listOf(backButton()))
                    )
                } else {
                    BotResponse(
                        text = """
$resultText

${buildQuestionText(nextQuestion)}
            """.trimIndent(), keyboard = buildQuestionKeyboard(nextQuestion)
                    )
                }

                sendResponse(chatId, messageId, response)
            }
        }
    }

    private fun checkNextQuestionAndSend(chatId: Long) {

        val trainer = getTrainer(chatId)

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
                text = "🎉 Отлично! Все слова в словаре выучены 👏", keyboard = listOf(listOf(backButton()))
            )
        } else {
            BotResponse(
                text = buildQuestionText(question), keyboard = buildQuestionKeyboard(question)
            )
        }

        sendResponse(chatId, null, response)
    }

    private fun buildMenuResponse(): BotResponse {
        val text = "Выбери действие:"

        val keyboard = listOf(
            listOf(TelegramButton("📚 Учить слова", LEARN_WORDS_CLICKED)),
            listOf(TelegramButton("📊 Статистика", STATISTICS_CLICKED)),
        )

        return BotResponse(text, keyboard)
    }

    private fun buildStatisticsResponse(chatId: Long): BotResponse {

        val trainer = getTrainer(chatId)
        val statistics = trainer.getStatistics()

        val text = "Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%"

        val keyboard = listOf(listOf(TelegramButton("♻\uFE0F Сбросить прогресс", RESET_CLICKED)), listOf(backButton()))

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

        val answerRows = question.variants.mapIndexed { i, word ->
            TelegramButton(word.translate, "$CALLBACK_DATA_ANSWER_PREFIX$i")
        }.chunked(2)

        return answerRows + listOf(listOf(backButton()))
    }

    private fun backButton(): TelegramButton {
        return TelegramButton("⬅\uFE0F Назад", CALLBACK_DATA_BACK)
    }

    fun sendResponse(
        chatId: Long,
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
        chatId: Long,
        text: String,
        keyboardRows: List<List<TelegramButton>>,
    ) {

        val requestBody = SendMessageRequest(
            chatId = chatId, text = text, replyMarkup = ReplyMarkup(inlineKeyboard = keyboardRows.map { row ->
                row.map { InlineKeyboardButton(it.text, it.callbackData) }
            })
        )

        val body = json.encodeToString(requestBody)

        sendJson("$TELEGRAM_BASE_URL$botToken/sendMessage", body)
    }

    private fun editMessageWithKeyboard(
        chatId: Long,
        messageId: Int,
        text: String,
        keyboardRows: List<List<TelegramButton>>,
    ) {

        val requestBody = EditMessageRequest(
            chatId = chatId,
            messageId = messageId,
            text = text,
            replyMarkup = ReplyMarkup(inlineKeyboard = keyboardRows.map { row ->
                row.map { InlineKeyboardButton(it.text, it.callbackData) }
            })
        )

        val body = json.encodeToString(requestBody)

        sendJson("$TELEGRAM_BASE_URL$botToken/editMessageText", body)
    }

    private fun sendJson(url: String, body: String) {
        val request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build()

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