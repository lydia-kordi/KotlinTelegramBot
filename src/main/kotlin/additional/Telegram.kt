package additional

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val HELLO_COMMAND = "hello"

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
}

fun main(args: Array<String>) {

    val botToken = args[0]
    val botService = TelegramBotService(botToken)

    var updateId = 0

    while (true) {
        Thread.sleep(2000)

        val updates: String = botService.getUpdates(updateId)

        val updateIdRegex = "\"update_id\"\\s*:\\s*(\\d+)".toRegex()
        val updateMatch = updateIdRegex.find(updates)
        if (updateMatch != null) {
            updateId = updateMatch.groupValues[1].toInt() + 1
        }

        val chatIdRegex = "\"chat\"\\s*:\\s*\\{\\s*\"id\"\\s*:\\s*(\\d+)".toRegex()
        val chatMatch = chatIdRegex.find(updates)
        val chatId = chatMatch?.groupValues?.get(1)

        val messageTextRegex: Regex = "\"text\"\\s*:\\s*\"(.+?)\"".toRegex()
        val matchResult: MatchResult? = messageTextRegex.find(updates)
        val groups = matchResult?.groups
        val text = groups?.get(1)?.value

        val normalizedText = text?.trim()?.lowercase()?.replace(Regex("\\s+"), " ")

        if (chatId != null && normalizedText != null) {
            println(
                "update_id=$updateId chat_id=$chatId: $text"
            )

            if (normalizedText.contains(HELLO_COMMAND)) {
                botService.sendMessage(chatId, HELLO_COMMAND)
            }
        }
    }
}