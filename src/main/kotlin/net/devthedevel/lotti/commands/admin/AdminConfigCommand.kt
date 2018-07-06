package net.devthedevel.lotti.commands.admin

import com.beust.klaxon.Klaxon
import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.commands.Command
import net.devthedevel.lotti.commands.CommandContext
import net.devthedevel.lotti.commands.InvalidCommand
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import net.devthedevel.lotti.json.RoleConverter
import sx.blah.discord.util.MessageBuilder

class AdminConfigCommand(context: CommandContext): Command(context) {
    companion object {
        const val COMMAND_NAME: String = "config"
    }

    private val json: String? = context.json
    private val adminOp: AdminOperation = if (context.arguments.isNotEmpty()) AdminOperation.parseOperation(context.arguments[0]) else AdminOperation.GET

    override fun execute() {
        var options: AdminOptions? = null

        //Catch json if its not a valid json string
        if (json != null ) {
            try {
                val userConverter = RoleConverter(context.guild)
                options = Klaxon().converter(userConverter.converter).parse<AdminOptions>(json)
            } catch (e: Exception) {}
        }

        when (adminOp) {
        //Get the current guild's config
            AdminOperation.GET -> {
                val (op, _options, roles) = LotteryDatabase.getAdminOptions(context.guild)

                MessageBuilder(Lotti.CLIENT).apply {
                    withChannel(context.channel)
                    when (op) {
                        OperationStatus.COMPLETED -> {
                            withContent(context.sender.mention(true) + "\n")
                            appendContent("Currency: ${_options.currency}, Price: ${_options.price}\n")
                            appendContent("Admin Roles: \n")
                            roles.forEach{appendContent("- $it \n")}
                        }
                        else -> {}
                    }
                    send()
                }
            }
        //Update current guild's config
            AdminOperation.SET, AdminOperation.ADD -> {
                if (options != null) {
                    LotteryDatabase.setAdminOptions(context.guild, context.channel, options, adminOp)

                    MessageBuilder(Lotti.CLIENT).apply {
                        withChannel(context.channel)
                        withContent("Admin set")
                        send()
                    }
                }
            }
            else -> return InvalidCommand(context).execute()
        }
    }
}