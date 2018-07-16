package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import sx.blah.discord.util.MessageBuilder

class CreateNewLottoCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {

    override fun sendInvalidMessage() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute() {
        val op = LotteryDatabase.createNewLotto(context.guild, context.channel, context.sender)

        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)
            when (op) {
                OperationStatus.COMPLETED -> {
                    if (context.isAdmin) {
                        withContent("Alrighty ${context.sender.mention(true)}, new lotto started!")
                    } else {
                        withContent("Yo ${context.sender.mention(true)}. Only admins can do this. You're not cool enough to be an admin. #nonadminproblems")
                    }
                }
                OperationStatus.EXISTS -> withContent("Hmmm ${context.sender.mention(true)}, there's currently a lotto started!")
                else -> return sendInvalidCommandMessage()
            }
            send()
        }
    }

    companion object {
        const val COMMAND_NAME: String = "new"
    }

}