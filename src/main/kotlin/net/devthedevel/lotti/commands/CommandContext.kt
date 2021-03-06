package net.devthedevel.lotti.commands

import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IUser

data class CommandContext(val guild: IGuild,
                          val channel: IChannel,
                          val sender: IUser,
                          val isAdmin: Boolean = false)