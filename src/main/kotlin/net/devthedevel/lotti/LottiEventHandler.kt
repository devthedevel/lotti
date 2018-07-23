package net.devthedevel.lotti

import mu.KotlinLogging
import net.devthedevel.lotti.commands.Command
import net.devthedevel.lotti.config.Config
import net.devthedevel.lotti.db.LotteryDatabase
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType

private val log = KotlinLogging.logger {}

class LottiEventHandler {

    @EventSubscriber
    fun onReady(event: ReadyEvent) {
        log.info { "Lotti ready!" }

        Lotti.CLIENT.shards.forEach {
            it.changePresence(StatusType.ONLINE, ActivityType.PLAYING, "/lotti help. Playing the slots!")
        }
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
        val command = Command.parseCommand(event)

        if (command?.validate() == true) command.execute() else command?.sendInvalidMessage()
    }

}