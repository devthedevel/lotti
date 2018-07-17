package net.devthedevel.lotti.commands.admin

import com.beust.klaxon.Klaxon
import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.commands.Command
import net.devthedevel.lotti.commands.CommandContext
import net.devthedevel.lotti.commands.InvalidCommand
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import net.devthedevel.lotti.json.UserConverter
import sx.blah.discord.util.MessageBuilder

class AdminRequestsCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {

    private val json: String? = null //context.json TODO Update parsing syntax
    private val adminOp: AdminOperation = if (parameters.isNotEmpty()) {
        val arg0 = parameters.removeAt(0)
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

                    //Return early if user given in JSON does not exist in guild
                    if (json != null && requests?.isEmpty() == true) {
                        sendMessage { +"User doesn't exist. Have you spelled the name correctly?" }
                        return
                    }

                    val (op, users) = LotteryDatabase.getTicketRequests(context.guild, context.channel, requests)

                    when (op) {
                        OperationStatus.COMPLETED -> {
                            sendMessage {
                                users.forEach {
                                    val username = it.user?.getNicknameForGuild(context.guild)
                                            ?: it.user?.getDisplayName(context.guild)
                                    +"- $username: ${it.tickets} requested tickets\n"
                                }
                            }
                        }
                        OperationStatus.NO_RESULT -> sendMessage { +"No requested tickets at this time" }
                        else -> { }
                    }
            }
            AdminOperation.SET -> {
                if (requests != null && requests.isNotEmpty()) {
                    print("")
                }
            }
            else -> return sendInvalidMessage()
        }
    }

    companion object {
        const val COMMAND_NAME: String = "requests"
    }
}