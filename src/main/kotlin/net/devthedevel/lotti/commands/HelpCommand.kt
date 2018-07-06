package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import sx.blah.discord.util.MessageBuilder

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