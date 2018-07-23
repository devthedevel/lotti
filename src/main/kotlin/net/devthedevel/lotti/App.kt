package net.devthedevel.lotti

import com.natpryce.konfig.Misconfiguration
import net.devthedevel.lotti.db.LotteryDatabase
import mu.KotlinLogging
import java.sql.SQLException

private val log = KotlinLogging.logger {}

fun main(args: Array<String>) {
    try {
        log.info { "Initializing Lotti..." }

        //Init database
        log.info { "Initializing database tables..." }
        LotteryDatabase.initTables()
        log.info { "Database tables initialized!" }

        //Register Discord event listener
        log.info { "Registering event handler..." }
        Lotti.CLIENT.dispatcher.registerListener(LottiEventHandler())
        log.info { "Registered event handler!" }

        //Log bot into Discord
        log.info { "Logging into the Discord gateway..." }
        Lotti.CLIENT.login()
        log.info { "Logged into the Discord gateway!" }

        //All good
        log.info { "Lotti initialized!" }
    } catch (e: Misconfiguration) {
        log.error(e) { "Misconfiguration. Check your config" }
    } catch (e: ExceptionInInitializerError) {
        log.error(e) { "Exception In Initializer" }
    } catch (e: SQLException) {
        log.error(e) { "SQL Exception" }
    } catch (e: Throwable) {
        log.error(e) { "Something bad happened..." }
    }

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