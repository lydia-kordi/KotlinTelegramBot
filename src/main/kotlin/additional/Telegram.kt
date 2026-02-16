package additional

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0

    while (true) {
        Thread.sleep(2000)
        val updates: String = getUpdates(botToken, updateId)
        println(updates)

        val startUpdateId = updates.lastIndexOf("update_id")
        val endUpdateId = updates.lastIndexOf(",\n\"message\"")
        if (startUpdateId == -1 || endUpdateId == -1) continue
        val updateIdString = updates.substring(startUpdateId + 11, endUpdateId)
        updateId = updateIdString.toInt() + 1
    }
}

fun getUpdates(botToken: String, updateID: Int): String {
    val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateID"
    val builder = HttpClient.newBuilder()
    val client = builder.build()
    val requestGeUpdates: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val responseGetUpdates: HttpResponse<String> = client.send(requestGeUpdates, HttpResponse.BodyHandlers.ofString())

    return responseGetUpdates.body()
}