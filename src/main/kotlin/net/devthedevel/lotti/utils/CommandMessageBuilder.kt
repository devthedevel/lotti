package net.devthedevel.lotti.utils

import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.util.MessageBuilder

class CommandMessageBuilder(client: IDiscordClient): MessageBuilder(client) {

    operator fun String.unaryPlus() {
        appendContent(this)
    }
}