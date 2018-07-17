package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.commands.admin.AdminCommand
import net.devthedevel.lotti.commands.debug.IdDebugCommand
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.utils.CommandMessageBuilder
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.MessageBuilder

abstract class Command constructor(val context: CommandContext, val parameters: MutableList<String> = mutableListOf()) {

    companion object {
        fun parseCommand(event: MessageReceivedEvent): Command? {
            val sender: IUser = event.author
            val channel: IChannel = event.channel
            val guild: IGuild = event.guild
            val commandName: String?
            val parameters = event.message.content.split(" ").toMutableList()

            //Early return
            if (sender.isBot || Lotti.COMMAND_PREFIX != parameters.removeAt(0)) return null

            commandName = parameters.removeAt(0)

            //Check if admin
            val isAdmin = let {
                val (_, _, adminRoles) = LotteryDatabase.getAdminOptions(guild)
                guild.owner == sender || adminRoles.intersect(sender.getRolesForGuild(guild)).isNotEmpty()
            }

            //Command values
            val context = CommandContext(guild, channel, sender, isAdmin)

            //Return corresponding command, or null if we couldn't determine command
            return when (commandName) {
                CreateNewLottoCommand.COMMAND_NAME -> CreateNewLottoCommand(context, parameters)
                RequestTicketCommand.COMMAND_NAME -> RequestTicketCommand(context, parameters)
                DrawWinnerCommand.COMMAND_NAME -> DrawWinnerCommand(context, parameters)
                HelpCommand.COMMAND_NAME -> HelpCommand(context, parameters)
                StatusCommand.COMMAND_NAME -> StatusCommand(context, parameters)
                AdminCommand.COMMAND_NAME -> AdminCommand(context, parameters)
                FeedbackCommand.COMMAND_NAME -> FeedbackCommand(context, parameters)
                IdDebugCommand.COMMAND_NAME -> IdDebugCommand(context, parameters)
                else -> InvalidCommand(context, parameters)
            }
        }
    }

    /*
    Methods
    */
    open fun validate(): Boolean = true

    abstract fun execute()

    open fun sendInvalidMessage() {
        sendMessage(context.channel, context.sender, { +"Hmmm something went wrong. Maybe try it again?"} )
    }

    fun sendMessage(channel: IChannel = context.channel, user: IUser? = null, message: CommandMessageBuilder.() -> Unit) {
        CommandMessageBuilder(Lotti.CLIENT).apply {
            withChannel(channel)
            if (user != null) {
                withContent(user.mention(true) + "\n")
            } else withContent("")
            message()
            send()
        }
    }
}
