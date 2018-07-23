package net.devthedevel.lotti.commands

class HelpCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {

    override fun sendInvalidMessage() {
        sendMessage { +"I don't know how or why you did it, but you managed to get *help* wrong..." }
    }

    override fun execute() {
        sendMessage(context.sender.orCreatePMChannel, context.sender) {
            +"Here's a list of commands:\n\n"
            +"**/lotti new**    *Creates a new lottery for the server*\n"
            +"**/lotti buy [user] [numTickets]**    *Buy numTickets for the specified user. Leave user blank to purchase tickets for yourself. numTickets must be positive*\n"
            +"**/lotti draw**    *Draws a winner for the current lottery. The lottery will then close itself*\n"
            +"**/lotti help**    *Displays this helpful help menu*\n"
            +"**/lotti status**  *Displays the current lottery's status*\n"
            +"**/lotti feedback**  *Send feedback*\n"

            if (context.isAdmin) {
                +"\n"
                +"Admin commands:\n"
                +"**/lotti admin config [get | set | add | del] {\"currency\": Text, \"price\": Integer, \"roles\": [Text]}**      *View config. If parameter is set/add, will update the config based on JSON*\n"
                +"**/lotti admin requests [{\"name\": Text}]**     *View ticket requests. If the JSON parameter is left out, will return all requested tickets for the lottery*\n"
                +"**/lotti admin approve [all]**       *Approve ticket requests. Currently can only approve all user's request tickets at once*\n"
            }
        }
    }

    companion object {
        const val COMMAND_NAME: String = "help"
    }
}