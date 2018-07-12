package net.devthedevel.lotti.commands.debug

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.commands.Command
import net.devthedevel.lotti.commands.CommandContext
import sx.blah.discord.util.MessageBuilder

class IdDebugCommand(context: CommandContext) : Command(context) {
    companion object {
        const val COMMAND_NAME: String = "debug"
    }

    private val id = if (context.arguments.isNotEmpty()) context.arguments.removeAt(0).toLong() else null

    override fun execute() {
        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)
            withContent(context.sender.mention(true) + "\n")

            if (id != null) {
                val user = context.guild.getUserByID(id)

                if (user == null) {
                    val guild = Lotti.CLIENT.getGuildByID(id)

                    if (guild != null) {
                        appendContent("Guild: ${guild.name}")
                    }
                } else {
                    appendContent("User: ${user.name} AKA ${user.getNicknameForGuild(context.guild)}")
                }
            } else appendContent("Come on Dev, gimme a proper ID. God, stop being bad!")
            send()
        }
    }
}