package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.commands.admin.AdminCommand
import net.devthedevel.lotti.commands.debug.IdDebugCommand
import net.devthedevel.lotti.db.LotteryDatabase
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
            var commandName: String? = null
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

    abstract fun sendInvalidMessage()
    abstract fun execute()

    fun sendMessage(channel: IChannel, user: IUser, message: MessageBuilder.() -> Unit) {
        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(channel)
            withContent(user.mention(true) + "\n")
            message()
            send()
        }
    }

    fun sendInvalidCommandMessage(mention: Boolean = false, msg: String? = null) {
        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)

            if (mention) {
                withContent(context.sender.mention(true) + "\n")
                appendContent("Seems like you made an invalid command.\n")
            } else {
                withContent("Seems like you made an invalid command.\n")
            }

            if (msg.isNullOrEmpty()) {
                appendContent("Have you made a typo? If you're feeling lost, try the help command")
            } else {
                appendContent(msg)
            }
            send()
        }
    }

    fun parseToInt(str: String, lower: Int = Int.MIN_VALUE, higher: Int = Int.MAX_VALUE, sendInvalidMessage: Boolean = false, invalidMessage: String? = null): Int? {
        var ret: Int? = null
        if (str.isNotEmpty()) {
            try {
                val num = str.toDouble().toInt()
                ret = if (num < lower) lower else num
                ret = if (num > higher) higher else num
            } catch (e: NumberFormatException) {
                if (sendInvalidMessage) {
                    sendInvalidCommandMessage(false, invalidMessage)
                }
            }
        }
        return ret
    }
}
