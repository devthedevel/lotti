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

class AdminApproveCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {

    private val json: String? = null //context.json TODO Update parsing syntax
    private val approveAll = if (parameters.isNotEmpty()) {
        "all" == parameters.removeAt(0)
    } else false

    override fun sendInvalidMessage() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

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

            if (requests != null) {
                val op = LotteryDatabase.approveTickets(context.guild, context.channel, requests)

                when (op) {
                    OperationStatus.COMPLETED -> {}
                    else -> return sendInvalidCommandMessage(false, null)
                }
            }
        }
    }

    companion object {
        const val COMMAND_NAME: String = "approve"
    }
}