package net.devthedevel.lotti.commands.admin

import sx.blah.discord.handle.obj.IUser

data class AdminRequests(
        val user: IUser? = null,
        val tickets: Int? = null)