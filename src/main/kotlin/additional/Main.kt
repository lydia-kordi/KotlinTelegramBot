package additional

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

    val updateIdRegex = "\"update_id\"\\s*:\\s*(\\d+)".toRegex()
    val chatIdRegex = "\"chat\"\\s*:\\s*\\{\\s*\"id\"\\s*:\\s*(\\d+)".toRegex()
    val messageIdRegex = "\"message_id\"\\s*:\\s*(\\d+)".toRegex()
    val messageTextRegex = "\"text\"\\s*:\\s*\"(.+?)\"".toRegex()
    val callbackRegex = "\"data\"\\s*:\\s*\"(.+?)\"".toRegex()

    var updateId = 0

    while (true) {
        Thread.sleep(2000)

        val updates = botService.getUpdates(updateId)

        updateIdRegex.find(updates)?.let {
            updateId = it.groupValues[1].toInt() + 1
        }

        val chatId = chatIdRegex.find(updates)?.groupValues?.get(1)
        val messageId = messageIdRegex.find(updates)?.groupValues?.get(1)?.toInt()
        val text = messageTextRegex.find(updates)?.groups?.get(1)?.value
        val callback = callbackRegex.find(updates)?.groups?.get(1)?.value
        val normalizedText = text?.trim()?.lowercase()?.replace(Regex("\\s+"), " ")

        if (chatId != null) {

            if (callback != null && messageId != null) {
                println("chatId = $chatId, messageId = $messageId, callback=$callback")
                botService.handleCallback(chatId, messageId, callback)

            } else if (normalizedText != null) {
                println("message=$text")

                when {
                    normalizedText.startsWith(HELLO_COMMAND) ->
                        botService.handleStart(chatId)

                    else ->
                        botService.sendResponse(
                            chatId,
                            null,
                            BotResponse(
                                text = "Такой команды не существует :(",
                                keyboard = listOf(listOf(TelegramButton("🔙 Назад", "back")))
                            )
                        )
                }
            }
        }
    }
}
