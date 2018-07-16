package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import sx.blah.discord.util.MessageBuilder

class InvalidCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {

    override fun sendInvalidMessage() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute() {
        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)
            withContent("${context.sender.mention(true)}, seems like you made an invalid command. Is there any typos? If you're feeling lost, try the help command")
            send()
        }
    }
}