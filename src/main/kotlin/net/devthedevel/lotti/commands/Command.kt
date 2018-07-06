package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.commands.admin.AdminCommand
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IUser

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
                val prefix = args.removeAt(0)
                commandName = args.removeAt(0)

                //Early return
                if (sender.isBot || !prefix.equals(Lotti.COMMAND_PREFIX, true)) return null
            }

            //Command values
            val context = CommandContext(guild, channel, sender, json, args)

            //Return corresponding command, or null if we couldn't determine command
            return when {
                CreateNewLottoCommand.COMMAND_NAME == commandName -> CreateNewLottoCommand(context)
                UserRequestTicketCommand.COMMAND_NAME == commandName -> UserRequestTicketCommand(context)
                DrawWinnerCommand.COMMAND_NAME == commandName -> DrawWinnerCommand(context)
                HelpCommand.COMMAND_NAME == commandName -> HelpCommand(context)
                StatusCommand.COMMAND_NAME == commandName -> StatusCommand(context)
                AdminCommand.COMMAND_NAME == commandName -> AdminCommand(context)
                else -> InvalidCommand(context)
            }
        }
    }

    /*
    Methods
    */
    abstract fun execute()
}
