package net.devthedevel.lotti.commands

import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus

class CreateNewLottoCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {

    override fun execute() {
        val op = LotteryDatabase.createNewLotto(context.guild, context.channel, context.sender)

        sendMessage(context.channel, context.sender) {
            when (op) {
                OperationStatus.COMPLETED -> {
                    if (context.isAdmin) {
                        +"Alrighty, new lotto started!"
                    } else {
                        +"Yo only admins can do this. You're not cool enough to be an admin. #nonadminproblems"
                    }
                }
                OperationStatus.EXISTS -> +"Hmmm there's currently a lotto started!"
                else -> sendInvalidMessage()
            }
        }
    }

    companion object {
        const val COMMAND_NAME: String = "new"
    }

}