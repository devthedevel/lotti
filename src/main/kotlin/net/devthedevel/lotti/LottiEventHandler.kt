package net.devthedevel.lotti

import mu.KotlinLogging
import net.devthedevel.lotti.commands.Command
import net.devthedevel.lotti.db.LotteryDatabase
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

class LottiEventHandler {

    companion object {
        private val log = KotlinLogging.logger {}
    }
    @EventSubscriber
    fun onReady(event: ReadyEvent) {
        log.info { "Lotti ready!" }
    }

    @EventSubscriber
    fun onGuildCreate(event: GuildCreateEvent) {
        LotteryDatabase.setDefaultGuildOptions(event.guild)
    }

    @EventSubscriber
    fun onGuildLeave(event: GuildLeaveEvent) {
        LotteryDatabase.deleteGuild(event.guild)
    }

    @EventSubscriber
    fun onMessage(event: MessageReceivedEvent) {
        Command.parseCommand(event).apply { this?.execute() }
    }

}