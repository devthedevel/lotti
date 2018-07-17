package net.devthedevel.lotti.commands

import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus

class FeedbackCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {

    var feedback: String = String()

    init {
       if (parameters.isNotEmpty()) {
           parameters.joinToString(" ")
           feedback.trim()
       }
    }

    override fun execute() {
        sendMessage(context.sender.orCreatePMChannel, context.sender) {
            if (feedback.isNotBlank()) {
                val op = LotteryDatabase.sendFeedback(context.sender, feedback)
                when (op) {
                    OperationStatus.COMPLETED -> +"Feedback recieved! Thanks for criticizing me!"
                    else -> sendInvalidMessage()
                }
            } else {
                +"I would love to hear your thoughts on the wonderful, amazing me, buuuuuuuut you need to actually write something about me first!"
            }
        }
    }

    companion object {
        const val COMMAND_NAME: String = "feedback"
    }
}