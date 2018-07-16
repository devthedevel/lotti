package net.devthedevel.lotti.commands.admin

import com.beust.klaxon.Klaxon
import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.commands.Command
import net.devthedevel.lotti.commands.CommandContext
import net.devthedevel.lotti.db.LotteryDatabase
import net.devthedevel.lotti.db.OperationStatus
import net.devthedevel.lotti.json.RoleConverter
import sx.blah.discord.util.MessageBuilder

class AdminConfigCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {

    private val json: String? = null //context.json TODO Update parsing syntax
    private val adminOp: AdminOperation = if (parameters.isNotEmpty()) AdminOperation.parseOperation(parameters[0]) else AdminOperation.GET

    override fun sendInvalidMessage() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute() {
        var options: AdminOptions? = null

        //Catch json if its not a valid json string
        if (json != null ) {
            try {
                val userConverter = RoleConverter(context.guild)
                options = Klaxon().converter(userConverter.converter).parse<AdminOptions>(json)
            } catch (e: Exception) {}
        }

        MessageBuilder(Lotti.CLIENT).apply {
            withChannel(context.channel)

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
                //Delete an admin role
                AdminOperation.DELETE -> {
                    if (options != null) {
                        val op = LotteryDatabase.deleteAdminRoles(context.guild, options.roles.mapNotNull { it.longID })

                        when (op) {
                            OperationStatus.COMPLETED -> {
                                appendContent("Roles deleted")
                            }
                            else -> sendInvalidCommandMessage(true, "Stay calm. I don't think that worked. Relax its not your fault. Just letting you know. Carry on.")
                        }
                    } else {
                        sendInvalidCommandMessage(true, "Excuse me. Hey. What are you removing? Nothing, that's what. Try again with some roles to remove please.")
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
                else -> return sendInvalidCommandMessage()
            }
        }
    }

    companion object {
        const val COMMAND_NAME: String = "config"
    }
}