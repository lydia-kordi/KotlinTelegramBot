package additional

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTICS_CLICKED = "show_statistics_clicked"
const val BACK_TO_MENU = "back_to_menu"

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

    fun sendMessage(chatId: String, text: String) {
        val url = "$TELEGRAM_BASE_URL$botToken/sendMessage?chat_id=$chatId&text=${URLEncoder.encode(text, "UTF-8")}"

        val request = HttpRequest.newBuilder().uri(URI.create(url)).build()

        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    fun sendMenu(chatId: String) {

        val body = """
            {
              "chat_id": $chatId,
              "text": "Основное меню",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    {
                      "text": "Изучать слова",
                      "callback_data": "$LEARN_WORDS_CLICKED"
                    },
                    {
                      "text": "Статистика",
                      "callback_data": "$STATISTICS_CLICKED"
                    }
                  ]
                ]
              }
            }
        """.trimIndent()

        sendJson(body)
    }

    fun handleCallback(chatId: String, callbackData: String) {

        when (callbackData) {

            LEARN_WORDS_CLICKED -> {
                sendMessage(chatId, "Пока не сделали :)")
            }

            STATISTICS_CLICKED -> {
                val statistics = trainer.getStatistics()
                val percent = if (statistics.total > 0) (statistics.learned * 100) / statistics.total
                else 0

                val body = """
                    {
                      "chat_id": $chatId,
                      "text": "Выучено ${statistics.learned} из ${statistics.total} слов | $percent%",
                      "reply_markup": {
                        "inline_keyboard": [
                          [
                            {
                              "text": "⬅️ В меню",
                              "callback_data": "$BACK_TO_MENU"
                            }
                          ]
                        ]
                      }
                    }
                """.trimIndent()

                sendJson(body)
            }

            BACK_TO_MENU -> {
                sendMenu(chatId)
            }
        }
    }

    private fun sendJson(body: String) {
        val url = "$TELEGRAM_BASE_URL$botToken/sendMessage"

        val request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build()

        client.send(request, HttpResponse.BodyHandlers.ofString())
    }
}