package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import sx.blah.discord.util.MessageBuilder

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