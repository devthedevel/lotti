package net.devthedevel.lotti.commands

import net.devthedevel.lotti.Lotti
import sx.blah.discord.util.MessageBuilder

class HelpCommand(context: CommandContext): Command(context) {
    companion object {
        const val COMMAND_NAME: String = "help"
    }

    override fun execute() {
        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.sender.orCreatePMChannel)
            withContent("Heya ${context.sender.mention(true)}\n")
            appendContent("Here's a list of commands:\n\n")
            appendContent("**/lotti new**    *Creates a new lottery for the server*\n")
            appendContent("**/lotti buy numTickets**    *Buy numTickets for the current lottery. numTickets must be positive*\n")
            appendContent("**/lotti draw**    *Draws a winner for the current lottery. The lottery will then close itself*\n")
            appendContent("**/lotti help**    *Displays this helpful help menu*\n")
            appendContent("**/lotti status**  *Displays the current lottery's status*\n")

            if (context.isAdmin) {
                appendContent("\n")
                appendContent("Admin commands:\n")
                appendContent("**/lotti admin config [get | set | add | del] {\"currency\": Text, \"price\": Integer, \"roles\": [Text]}**      *View config. If parameter is set/add, will update the config based on JSON*\n")
                appendContent("**/lotti admin requests [{\"name\": Text}]**     *View ticket requests. If the JSON parameter is left out, will return all requested tickets for the lottery*\n")
                appendContent("**/lotti admin approve [all]**       *Approve ticket requests. Currently can only approve all user's request tickets at once*\n")
            }
            send()
        }
    }
}