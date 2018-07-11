package net.devthedevel.lotti.commands

import com.sun.org.apache.xpath.internal.operations.Bool
import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.commands.admin.AdminCommand
import net.devthedevel.lotti.db.LotteryDatabase
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.MessageBuilder

abstract class Command constructor(val context: CommandContext) {

    companion object {
        fun parseCommand(event: MessageReceivedEvent): Command? {
            val sender: IUser = event.author
            val channel: IChannel = event.channel
            val guild: IGuild = event.guild
            var json: String? = null
            var args: MutableList<String> = mutableListOf()
            var commandName: String? = null

            //Handle event's message
            let {
                var messageContents = event.message.content

                //Early return
                if (sender.isBot || !messageContents.startsWith(Lotti.COMMAND_PREFIX)) return null

                //Parse JSON arguments
                let {
                    val jsonStartIdx = messageContents.indexOf("{")
                    val jsonEndIdx = messageContents.lastIndexOf("}")
                    json = if (jsonStartIdx != -1 && jsonEndIdx != -1) messageContents.substring(jsonStartIdx, jsonEndIdx + 1) else null
                    if (json != null) {
                        messageContents = messageContents.replace(json as String, "").trim()
                    }
                }

                //Split non-JSON arguments and remove prefix/command name
                args = messageContents.split(" ").toMutableList()
                args.removeAt(0)
                commandName = args.removeAt(0)
            }

            //Check if admin
            val isAdmin = let {
                val (_, _, adminRoles) = LotteryDatabase.getAdminOptions(guild)
                guild.owner == sender || adminRoles.intersect(sender.getRolesForGuild(guild)).isNotEmpty()
            }

            //Command values
            val context = CommandContext(guild, channel, sender, json, isAdmin, args)

            //Return corresponding command, or null if we couldn't determine command
            return when (commandName) {
                CreateNewLottoCommand.COMMAND_NAME -> CreateNewLottoCommand(context)
                RequestTicketCommand.COMMAND_NAME -> RequestTicketCommand(context)
                DrawWinnerCommand.COMMAND_NAME -> DrawWinnerCommand(context)
                HelpCommand.COMMAND_NAME -> HelpCommand(context)
                StatusCommand.COMMAND_NAME -> StatusCommand(context)
                AdminCommand.COMMAND_NAME -> AdminCommand(context)
                else -> InvalidCommand(context)
            }
        }
    }

    /*
    Methods
    */
    abstract fun execute()

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
