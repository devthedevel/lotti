package net.devthedevel.lotti.config

import com.natpryce.konfig.*

object Config {

    //Loads properties from different locations
    private val config = ConfigurationProperties.systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromResource("lotti.config")

    //Property keys
    private val discordDev = Key("discord.dev", booleanType)
    private val discordToken = Key("discord.token", stringType)
    private val databaseType = Key("database.type", stringType)
    private val databaseHost = Key("database.host", stringType)
    private val databasePort = Key("database.port", intType)
    private val databaseName = Key("database.name", stringType)
    private val databaseUser = Key("database.user", stringType)
    private val databasePassword = Key("database.password", stringType)

    /*
    Config objects
     */
    object Discord {
        val dev: Boolean = config[discordDev]
        val token: String = config[discordToken]
    }

    object Database {
        val type: String = config[databaseType]
        val host: String = config[databaseHost]
        val port: Int = config[databasePort]
        val name: String = config[databaseName]
        val user: String = config[databaseUser]
        val password: String = config[databasePassword]
    }
}