package net.devthedevel.lotti

import net.devthedevel.lotti.config.Config
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient

class Lotti {

    companion object {
        //Constants
        const val VERSION = "alpha_1.0"
        const val COMMAND_PREFIX = "/lotti"

        //Discord gateway client
        val CLIENT: IDiscordClient = ClientBuilder().withToken(Config.Discord.token).build()
    }
}