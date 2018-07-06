package net.devthedevel.lotti.commands.admin

import net.devthedevel.lotti.commands.Command
import net.devthedevel.lotti.commands.CommandContext
import net.devthedevel.lotti.commands.InvalidCommand

class AdminCommand(context: CommandContext): Command(context) {
    companion object {
        const val COMMAND_NAME: String = "admin"
    }

    private val subCommand: String? = if (context.arguments.isNotEmpty()) context.arguments.removeAt(0) else null

    override fun execute() {
        return when (subCommand) {
            AdminConfigCommand.COMMAND_NAME -> AdminConfigCommand(context).execute()
            AdminRequestsCommand.COMMAND_NAME -> AdminRequestsCommand(context).execute()
            AdminApproveCommand.COMMAND_NAME -> AdminApproveCommand(context).execute()
            else -> InvalidCommand(context).execute()
        }
    }
}