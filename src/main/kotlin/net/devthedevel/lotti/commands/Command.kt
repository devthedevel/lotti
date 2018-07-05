package net.devthedevel.lotti.commands

import com.beust.klaxon.Klaxon
import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import net.devthedevel.lotti.utils.random
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.MessageBuilder

sealed class Command constructor(val context: CommandContext) {

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
                UserBuyTicketCommand.COMMAND_NAME == commandName -> UserBuyTicketCommand(context)
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
    abstract fun validate(): Boolean
    abstract fun execute()

    /*
    Sealed classes
     */
    class CreateNewLottoCommand(context: CommandContext): Command(context) {
        companion object {
            const val COMMAND_NAME: String = "new"
        }

        override fun validate(): Boolean {
            return context.arguments[0] == COMMAND_NAME
        }

        override fun execute() {
            val op = LotteryDatabase.createNewLotto(context.guild, context.channel, context.sender)

            MessageBuilder(Lotti.CLIENT).apply {
                withChannel(context.channel)
                when (op) {
                    OperationStatus.COMPLETED -> withContent("Alrighty ${context.sender.mention(true)}, new lotto started!")
                    OperationStatus.EXISTS -> withContent("Hmmm ${context.sender.mention(true)}, there's currently a lotto started!")
                    else -> return InvalidCommand(context).execute()
                }
                send()
            }
        }
    }

    class UserBuyTicketCommand(context: CommandContext): Command(context) {
        var numTickets: Int = 0

        init {
            if (context.arguments.size >= 2) numTickets = context.arguments[1].toInt()
        }

        companion object {
            const val COMMAND_NAME: String = "buy"
        }

        override fun validate(): Boolean {
            if (context.arguments[0] == COMMAND_NAME) {
                val num: Int? = context.arguments[1].toIntOrNull()
                if (num != null && num > 0) {
                    return true
                }
            }
            return false
        }

        override fun execute() {
            val (op, result, _) = LotteryDatabase.userBuyTickets(context.guild, context.channel, context.sender, numTickets)

            val messageBuilder = MessageBuilder(Lotti.CLIENT)
            messageBuilder.withChannel(context.channel)
            when (op) {
                OperationStatus.COMPLETED -> {
                    messageBuilder.withContent("${context.sender.mention(true)} has bought $numTickets entries for a total of $result entries")
                }
                OperationStatus.DOES_NOT_EXIST -> {
                    messageBuilder.withContent("Hey ${context.sender.mention(true)}, I know you're eager to throw away money but there's no lottery started. Ask your leaders to start one.")
                }
                OperationStatus.FAILED -> {
                    messageBuilder.withContent("Oh uh, something went wrong! ${context.sender.mention(true)}, this is probably your fault, just saying.")
                }
            }
            messageBuilder.send()
        }
    }

    class DrawWinnerCommand(context: CommandContext): Command(context) {
        companion object {
            const val COMMAND_NAME: String = "draw"
        }

        override fun validate(): Boolean {
            return true
        }

        override fun execute() {
            val (op, result, _) = LotteryDatabase.getUsersInLotto(context.guild, context.channel)

            @Suppress("UNCHECKED_CAST")
            val lottoUsers: MutableList<Long> = result as MutableList<Long>

            val winnerId = lottoUsers.random()

            val winner = context.guild.getUserByID(winnerId)

            LotteryDatabase.deleteLotto(context.guild, context.channel)

            val messageBuilder = MessageBuilder(Lotti.CLIENT)
            messageBuilder.withChannel(context.channel)
            when (op) {
                OperationStatus.COMPLETED -> {
                    messageBuilder.withContent("Drawing winner...\n\n")
                    messageBuilder.appendContent("And our winner is: ${winner.mention(true)}! Congrats!")
                }
                else -> {
                    messageBuilder.withContent("Oh uh, something went wrong! ${context.sender.mention(true)}, this is probably your fault, just saying.")
                }
            }
            messageBuilder.send()
        }
    }

    class HelpCommand(context: CommandContext): Command(context) {
        companion object {
            const val COMMAND_NAME: String = "help"
        }

        override fun validate(): Boolean {
            return context.arguments[0] == COMMAND_NAME
        }

        override fun execute() {
            MessageBuilder(Lotti.CLIENT).apply {
               withChannel(context.channel)
               withContent("Heya ${context.sender.mention(true)}\n")
               appendContent("Here's a list of commands:\n\n")
               appendContent("/lotti new    *Creates a new lottery for the server*\n")
               appendContent("/lotti buy numTickets    *Buy numTickets for the current lottery. numTickets must be positive*\n")
               appendContent("/lotti draw    *Draws a winner for the current lottery. The lottery will then close itself*\n")
               appendContent("/lotti help    *Displays this helpful help menu*\n")
               send()
            }
        }
    }

    class StatusCommand(context: CommandContext): Command(context) {
        companion object {
            const val COMMAND_NAME: String = "status"
        }

        private val scope: String? = if (context.arguments.size == 0) null else context.arguments[0]

        override fun validate(): Boolean {
            return context.arguments[0] == COMMAND_NAME
        }

        override fun execute() {
            if (scope != null) {

            }

            val (creatorId, currencyName, ticketPrice, users, result) = LotteryDatabase.getChannelStatus(context.guild, context.channel)

            MessageBuilder(Lotti.CLIENT).apply {
                withChannel(context.channel)
                when (result) {
                    OperationStatus.COMPLETED -> {
                        val creatorUser = context.guild.getUserByID(creatorId ?: 0)
                        val creatorName = creatorUser.getNicknameForGuild(context.guild)?: creatorUser.getDisplayName(context.guild)
                        withContent("Here's this channel's lottery status:\n\n")
                        appendContent("Creator: $creatorName, Currency: $currencyName, Ticket Price: $ticketPrice\n")
                        appendContent("People entered:\n")
                        for (userTicket: Pair<Long, Int> in users) {
                            val user = context.guild.getUserByID(userTicket.first)
                            val userName = user.getNicknameForGuild(context.guild)?: user.getDisplayName(context.guild)
                            appendContent("$userName: ${userTicket.second} tickets")
                        }
                    }
                    OperationStatus.DOES_NOT_EXIST -> withContent("Hey ${context.sender.mention(true)}, I know you're eager to throw away money but there's no lottery started. Ask your leaders to start one.")
                    else -> return InvalidCommand(context).execute()
                }
                send()
            }

        }
    }

    class AdminCommand(context: CommandContext): Command(context) {
        companion object {
            const val COMMAND_NAME: String = "admin"
        }

        private val json: String? = context.json
        private val adminOp: AdminOperation = if (context.arguments.size == 1) AdminOperation.parseOperation(context.arguments[0]) else AdminOperation.GET

        override fun validate(): Boolean {
            return COMMAND_NAME == context.arguments.removeAt(0)
        }

        override fun execute() {
            var options: AdminOptions? = null

            //Catch json if its not a valid json string
            if (json != null ) {
                try {
                    options = Klaxon().parse<AdminOptions>(json).apply { this?.adminOperation = adminOp }
                } catch (e: Exception) {}
            }

            when (options?.adminOperation) {
                AdminOperation.SET, AdminOperation.ADD -> {

                    LotteryDatabase.setAdminOptions(context.guild, context.channel, options)

                    MessageBuilder(Lotti.CLIENT).apply {
                        withChannel(context.channel)
                        withContent("Admin set")
                        send()
                    }
                }
                else -> return InvalidCommand(context).execute()
            }
        }
    }

    class InvalidCommand(context: CommandContext): Command(context) {
        companion object {
            const val COMMAND_NAME: String = "invalid"
        }

        override fun validate(): Boolean {
            return true
        }

        override fun execute() {
            MessageBuilder(Lotti.CLIENT).apply {
                withChannel(context.channel)
                withContent("${context.sender.mention(true)}, seems like you made an invalid command. Is there any typos? If you're feeling lost, try the help command")
                send()
            }
        }
    }
}
