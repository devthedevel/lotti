package net.devthedevel.lotti.commands.admin

import sx.blah.discord.handle.obj.IRole

data class AdminOptions(
        val currency: String? = null,
        val price: Int? = null,
        var roles: MutableList<IRole> = mutableListOf()
)