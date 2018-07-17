package net.devthedevel.lotti.commands

class InvalidCommand(context: CommandContext, parameters: MutableList<String>): Command(context, parameters) {

    override fun execute() {
        sendInvalidMessage()
    }
}