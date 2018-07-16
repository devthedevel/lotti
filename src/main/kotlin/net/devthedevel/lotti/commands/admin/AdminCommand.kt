package net.devthedevel.lotti.commands.admin

import net.devthedevel.lotti.commands.Command
import net.devthedevel.lotti.commands.CommandContext
import net.devthedevel.lotti.commands.InvalidCommand

class AdminCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {

    private val subCommand: String? = if (parameters.isNotEmpty()) parameters.removeAt(0) else null

    override fun sendInvalidMessage() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute() {
        if (context.isAdmin) {
            return when (subCommand) {
                AdminConfigCommand.COMMAND_NAME -> AdminConfigCommand(context, parameters).execute()
                AdminRequestsCommand.COMMAND_NAME -> AdminRequestsCommand(context, parameters).execute()
                AdminApproveCommand.COMMAND_NAME -> AdminApproveCommand(context, parameters).execute()
                else -> sendInvalidCommandMessage()
            }
        } else {
            sendInvalidCommandMessage(true, "Wait a second....you're not an admin! I will not execute this command now. Sorry (not sorry).")
        }
    }

    companion object {
        const val COMMAND_NAME: String = "admin"
    }
}