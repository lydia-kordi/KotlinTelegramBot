package additional

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


fun main(args: Array<String>) {

    val botToken = args[0]
    val urlGetMe = "https://api.telegram.org/bot$botToken/getMe"
    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates"

    val builder = HttpClient.newBuilder()
    val client = builder.build()

    val requestGetMe: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetMe)).build()
    val requestGeUpdates: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()

    val responseGetMe: HttpResponse<String> = client.send(requestGetMe, HttpResponse.BodyHandlers.ofString())
    val responseGetUpdates: HttpResponse<String> = client.send(requestGeUpdates, HttpResponse.BodyHandlers.ofString())

    println(responseGetMe.body())
    println(responseGetUpdates.body())


}