package additional

import kotlinx.serialization.json.Json

const val HELLO_COMMAND = "/start"

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        println("Передай bot token как аргумент программы")
        return
    }

    val botToken = args[0]
    val dictionary = loadDictionary("words.txt")

    val trainer = LearnWordsTrainer(dictionary)
    val botService = TelegramBotService(botToken, trainer)

    var updateId = 0
    val json = Json {
        ignoreUnknownKeys = true
    }

    while (true) {
        Thread.sleep(2000)

        val updatesRaw = botService.getUpdates(updateId)
        val updates = json.decodeFromString<UpdateResponse>(updatesRaw)

        updates.result.forEach { update ->

            updateId = (update.updateId + 1).toInt()

            val message = update.message
            val callback = update.callbackQuery

            if (callback != null) {
                val chatId = callback.message.chat.id
                val messageId = callback.message.messageId.toInt()

                botService.handleCallback(
                    chatId,
                    messageId,
                    callback.data
                )

            } else if (message?.text != null) {

                val chatId = message.chat.id
                val normalizedText = message.text
                    .trim()
                    .lowercase()
                    .replace(Regex("\\s+"), " ")

                when {
                    normalizedText.startsWith(HELLO_COMMAND) ->
                        botService.handleStart(chatId)

                    else ->
                        botService.sendResponse(
                            chatId,
                            null,
                            BotResponse(
                                text = "Такой команды не существует :(",
                                keyboard = listOf(
                                    listOf(TelegramButton("🔙 Назад", "back"))
                                )
                            )
                        )
                }
            }
        }
    }
}
