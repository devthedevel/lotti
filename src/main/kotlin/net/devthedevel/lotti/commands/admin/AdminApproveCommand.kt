package net.devthedevel.lotti.commands.admin

import com.beust.klaxon.Klaxon
import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.commands.Command
import net.devthedevel.lotti.commands.CommandContext
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import net.devthedevel.lotti.json.UserConverter
import sx.blah.discord.util.MessageBuilder

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