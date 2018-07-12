package net.devthedevel.lotti

import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient

class Lotti {

    companion object {
        //Consts
        const val VERSION = "alpha_1.0"
        const val COMMAND_PREFIX = "/lotti"

        //Env vars
        val TOKEN = System.getenv("DISCORD_BOT_TOKEN")
        val DATABASE_URL = System.getenv("DATABASE_URL_SSL")

        //Discord gateway client
        val CLIENT: IDiscordClient = ClientBuilder().withToken(TOKEN).build()
    }
}