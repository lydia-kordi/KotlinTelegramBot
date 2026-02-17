package additional

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val HELLO_COMMAND = "/start"

class TelegramBotService(private val botToken: String) {

    private val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateID: Int): String {
        val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateID"
        val requestGetUpdates: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val responseGetUpdates: HttpResponse<String> =
            client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

        return responseGetUpdates.body()
    }

    fun sendMessage(chatId: String, text: String) {
        val urlSendMessage =
            "$TELEGRAM_BASE_URL$botToken/sendMessage?chat_id=$chatId&text=${URLEncoder.encode(text, "UTF-8")}"

        val requestSendMessage: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()

        val responseSendMessage: HttpResponse<String> =
            client.send(requestSendMessage, HttpResponse.BodyHandlers.ofString())

        responseSendMessage.body()
    }

    fun sendMenu(chatId: String): String {
        val sendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage"
        val sendMenuBody = """
            {
              "chat_id": $chatId,
              "text": "Основное меню",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    {
                      "text": "Изучать слова",
                      "callback_data": "learn_words_clicked"
                    },
                    {
                      "text": "Статистика",
                      "callback_data": "show_statistics_clicked"
                    }
                  ],
                  [
                    {
                      "text": "Выход",
                      "callback_data": "exit_menu_clicked"
                    }
                  ]
                ]
              }
            }
        """.trimIndent()

        val requestSendMessage: HttpRequest =
            HttpRequest.newBuilder().uri(URI.create(sendMessage)).header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(sendMenuBody)).build()

        val responseSendMessage: HttpResponse<String> =
            client.send(requestSendMessage, HttpResponse.BodyHandlers.ofString())

        return responseSendMessage.body()
    }
}

fun main(args: Array<String>) {

    val botToken = args[0]
    val botService = TelegramBotService(botToken)

    val updateIdRegex = "\"update_id\"\\s*:\\s*(\\d+)".toRegex()
    val chatIdRegex = "\"chat\"\\s*:\\s*\\{\\s*\"id\"\\s*:\\s*(\\d+)".toRegex()
    val messageTextRegex = "\"text\"\\s*:\\s*\"(.+?)\"".toRegex()
    val callbackRegex = "\"callback_data\"\\s*:\\s*\"(.+?)\"".toRegex()

    var updateId = 0

    while (true) {
        Thread.sleep(2000)

        val updates = botService.getUpdates(updateId)

        updateIdRegex.find(updates)?.let {
            updateId = it.groupValues[1].toInt() + 1
        }

        val chatId = chatIdRegex.find(updates)?.groupValues?.get(1)
        val text = messageTextRegex.find(updates)?.groups?.get(1)?.value
        val callback = callbackRegex.find(updates)?.groups?.get(1)?.value
        val normalizedText = text?.trim()?.lowercase()?.replace(Regex("\\s+"), " ")

        if (chatId != null) {
            if (callback != null) {
                println("update_id=$updateId chat_id=$chatId callback=$callback")
            } else if (normalizedText != null) {
                println("update_id=$updateId chat_id=$chatId message=\"$text\"")

                if (normalizedText.contains(HELLO_COMMAND)) {
                    botService.sendMenu(chatId)
                } else {
                    botService.sendMessage(chatId, "Такой команды не существует")
                }
            }
        }
    }
}