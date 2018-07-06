package net.devthedevel.lotti.commands

import com.beust.klaxon.Klaxon
import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.commands.admin.AdminOperation
import net.devthedevel.lotti.commands.admin.AdminOptions
import net.devthedevel.lotti.commands.admin.AdminRequests
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import net.devthedevel.lotti.json.RoleConverter
import net.devthedevel.lotti.json.UserConverter
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

    /*
    Sealed classes
     */
    class CreateNewLottoCommand(context: CommandContext): Command(context) {
        companion object {
            const val COMMAND_NAME: String = "new"
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

    class UserRequestTicketCommand(context: CommandContext): Command(context) {
        var numTickets: Int = 0

        init {
            if (context.arguments.isNotEmpty()) numTickets = context.arguments[0].toInt()
        }

        companion object {
            const val COMMAND_NAME: String = "buy"
        }

        override fun execute() {
            val (_, _, adminRoles) = LotteryDatabase.getAdminOptions(context.guild)

            val isAdmin = adminRoles.intersect(context.sender.getRolesForGuild(context.guild)).isNotEmpty()

            val (op, approved, requested) = LotteryDatabase.userBuyTickets(context.guild, context.channel, context.sender, numTickets, isAdmin)

            MessageBuilder(Lotti.CLIENT).apply {
                withChannel(context.channel)
                when (op) {
                    OperationStatus.COMPLETED -> {
                        withContent("${context.sender.mention(true)} has bought $numTickets tickets. You have $approved approved tickets, and $requested requested tickets.")
                    }
                    OperationStatus.DOES_NOT_EXIST -> {
                        withContent("Hey ${context.sender.mention(true)}, I know you're eager to throw away money but there's no lottery started. Ask your leaders to start one.")
                    }
                    else -> return InvalidCommand(context).execute()
                }
                send()
            }
        }
    }

    class DrawWinnerCommand(context: CommandContext): Command(context) {
        companion object {
            const val COMMAND_NAME: String = "draw"
        }

        override fun execute() {
            val (op, result) = LotteryDatabase.getUsersInLotto(context.guild, context.channel)

            MessageBuilder(Lotti.CLIENT).apply {
                withChannel(context.channel)
                when (op) {
                    OperationStatus.COMPLETED -> {
                        if (result.isNotEmpty()) {
                            val winner = context.guild.getUserByID(result.random())
                            LotteryDatabase.deleteLotto(context.guild, context.channel)
                            withContent("Drawing winner...\n\n")
                            appendContent("And our winner is: ${winner.mention(true)}! Congrats!")
                        } else {
                            withContent("Huh, looks like no one has any approved tickets...")
                        }
                    }
                    else -> return InvalidCommand(context).execute()
                }
                send()
            }
        }
    }

    class HelpCommand(context: CommandContext): Command(context) {
        companion object {
            const val COMMAND_NAME: String = "help"
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
                            appendContent("$userName: ${userTicket.second} requested")
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

        private val subCommand: String? = if (context.arguments.isNotEmpty()) context.arguments.removeAt(0) else null

        override fun execute() {
            return when (subCommand) {
                AdminConfigCommand.COMMAND_NAME -> AdminConfigCommand(context).execute()
                AdminRequestsCommand.COMMAND_NAME -> AdminRequestsCommand(context).execute()
                AdminApproveCommand.COMMAND_NAME -> AdminApproveCommand(context).execute()
                else -> InvalidCommand(context).execute()
            }
        }
    }

    class AdminConfigCommand(context: CommandContext): Command(context) {
        companion object {
            const val COMMAND_NAME: String = "config"
        }

        private val json: String? = context.json
        private val adminOp: AdminOperation = if (context.arguments.isNotEmpty()) AdminOperation.parseOperation(context.arguments[0]) else AdminOperation.GET

        override fun execute() {
            var options: AdminOptions? = null

            //Catch json if its not a valid json string
            if (json != null ) {
                try {
                    val userConverter = RoleConverter(context.guild)
                    options = Klaxon().converter(userConverter.converter).parse<AdminOptions>(json)
                } catch (e: Exception) {}
            }

            when (adminOp) {
            //Get the current guild's config
                AdminOperation.GET -> {
                    val (op, _options, roles) = LotteryDatabase.getAdminOptions(context.guild)

                    MessageBuilder(Lotti.CLIENT).apply {
                        withChannel(context.channel)
                        when (op) {
                            OperationStatus.COMPLETED -> {
                                withContent(context.sender.mention(true) + "\n")
                                appendContent("Currency: ${_options.currency}, Price: ${_options.price}\n")
                                appendContent("Admin Roles: \n")
                                roles.forEach{appendContent("- $it \n")}
                            }
                            else -> {}
                        }
                        send()
                    }
                }
            //Update current guild's config
                AdminOperation.SET, AdminOperation.ADD -> {
                    if (options != null) {
                        LotteryDatabase.setAdminOptions(context.guild, context.channel, options, adminOp)

                        MessageBuilder(Lotti.CLIENT).apply {
                            withChannel(context.channel)
                            withContent("Admin set")
                            send()
                        }
                    }
                }
                else -> return InvalidCommand(context).execute()
            }
        }
    }

    class AdminRequestsCommand(context: CommandContext): Command(context) {
        companion object {
            const val COMMAND_NAME: String = "requests"
        }

        private val json: String? = context.json
        private val adminOp: AdminOperation = if (context.arguments.isNotEmpty()) {
            val arg0 = context.arguments.removeAt(0)
            if (arg0 != "[]") {
                AdminOperation.parseOperation(arg0)
            }
            AdminOperation.GET
        } else AdminOperation.GET   //TODO Find a better way to do this

        override fun execute() {
            var requests: List<AdminRequests>? = listOf()

            if (json != null) {
                try {
                    val converter = UserConverter(context.guild)
                    requests = Klaxon().converter(converter.converter).parseArray("[$json]")
                } catch (e: Exception) {}
            }

            when (adminOp) {
                AdminOperation.GET -> {
                    MessageBuilder(Lotti.CLIENT).apply {
                        withChannel(context.channel)

                        //Return early if user given in JSON does not exist in guild
                        if (json != null && requests?.isEmpty() == true) {
                            withContent(context.sender.mention(true) + "\n")
                            appendContent("User doesn't exist. Have you spelled the name correctly?")
                            send()
                            return
                        }

                        val (op, users) = LotteryDatabase.getTicketRequests(context.guild, context.channel, requests)

                        when (op) {
                            OperationStatus.COMPLETED -> {
                                withContent(context.sender.mention(true) + "\n")
                                users.forEach {
                                    val username = it.user?.getNicknameForGuild(context.guild)
                                            ?: it.user?.getDisplayName(context.guild)
                                    appendContent("- $username: ${it.tickets} requested tickets\n")
                                }
                            }
                            OperationStatus.NO_RESULT -> {
                                withContent(context.sender.mention(true) + "\n")
                                appendContent("No requested tickets at this time")
                            }
                            else -> {
                            }
                        }
                        send()
                    }
                }
                AdminOperation.SET -> {
                    if (requests != null && requests.isNotEmpty()) {
                        print("")
                    }
                }
                else -> return InvalidCommand(context).execute()
            }
        }
    }

    class AdminApproveCommand(context: CommandContext): Command(context) {
        companion object {
            const val COMMAND_NAME: String = "approve"
        }

        private val json: String? = context.json
        private val approveAll = if (context.arguments.isNotEmpty()) {
            "all" == context.arguments.removeAt(0)
        } else false

        override fun execute() {
            if (approveAll) {
                val op = LotteryDatabase.approveTickets(context.guild, context.channel, approveAll = approveAll)

                when (op) {
                    OperationStatus.COMPLETED -> {
                        MessageBuilder(Lotti.CLIENT).apply {
                            withChannel(context.channel)
                            withContent("All tickets approved")
                            send()
                        }
                    }
                    else -> {
                    }
                }
            } else {
                var requests: List<AdminRequests>? = listOf()

                if (json != null) {
                    try {
                        val converter = UserConverter(context.guild)
                        requests = Klaxon().converter(converter.converter).parseArray("[$json]")
                    } catch (e: Exception) {}
                }
            }
        }
    }

    class InvalidCommand(context: CommandContext): Command(context) {
        override fun execute() {
            MessageBuilder(Lotti.CLIENT).apply {
                withChannel(context.channel)
                withContent("${context.sender.mention(true)}, seems like you made an invalid command. Is there any typos? If you're feeling lost, try the help command")
                send()
            }
        }
    }
}
