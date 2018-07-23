package net.devthedevel.lotti.commands.debug

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.commands.Command
import net.devthedevel.lotti.commands.CommandContext
import sx.blah.discord.util.MessageBuilder

class IdDebugCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {

    private val id = if (parameters.isNotEmpty()) parameters.removeAt(0).toLong() else null

    override fun execute() {
        sendMessage(context.sender.orCreatePMChannel, context.sender) {
            if (id != null) {
                val user = context.guild.getUserByID(id)

                if (user == null) {
                    val guild = Lotti.CLIENT.getGuildByID(id)

                    if (guild != null) {
                        +"Guild: ${guild.name}"
                    }
                } else {
                    +"User: ${user.name} AKA ${user.getNicknameForGuild(context.guild)}"
                }
            } else +"Come on Dev, gimme a proper ID. God, stop being bad!"
        }
    }

    companion object {
        const val COMMAND_NAME: String = "debug"
    }
}