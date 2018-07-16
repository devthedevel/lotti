package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import sx.blah.discord.util.MessageBuilder

class FeedbackCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {

    var feedback: String = String()

    init {
       if (parameters.isNotEmpty()) {
           parameters.joinToString(" ")
           feedback.trim()
       }
    }

    override fun sendInvalidMessage() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute() {
        sendMessage(context.sender.orCreatePMChannel, context.sender) {
            if (feedback.isNotBlank()) {
                val op = LotteryDatabase.sendFeedback(context.sender, feedback)
                when (op) {
                    OperationStatus.COMPLETED -> appendContent("Feedback recieved! Thanks for criticizing me!")
                    else -> sendInvalidCommandMessage()
                }
            } else {
                appendContent("I would love to hear your thoughts on the wonderful, amazing me, buuuuuuuut you need to actually write something about me first!")
            }
        }
    }

    companion object {
        const val COMMAND_NAME: String = "feedback"
    }
}