package net.devthedevel.lotti

import net.devthedevel.lotti.db.LotteryDatabase
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

fun main(args: Array<String>) {
    log.info { "Initializing Lotti..." }

    //Register Discord event listener
    Lotti.CLIENT.dispatcher.registerListener(LottiEventHandler())
    log.info { "Registered event handler..." }

    //Init
    LotteryDatabase.initTables()
    log.info { "Initializing database tables..." }

    //Log bot into Discord
    Lotti.CLIENT.login()
    log.info { "Logging into the Discord gateway..." }

    //Register shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        fun run() {
            print("Shutting down Lotti...")

            if (Lotti.CLIENT.isLoggedIn) {
                Lotti.CLIENT.logout()
                log.info { "Lotti logged out" }
            }

            log.info { "Shutdown complete. Goodbye!" }
        }
    })
}