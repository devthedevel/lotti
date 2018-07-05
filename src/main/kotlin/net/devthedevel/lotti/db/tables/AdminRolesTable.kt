package net.devthedevel.lotti.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object AdminRolesTable: Table() {
    val guildId = integer("guild_index").references(GuildOptionsTable.id, ReferenceOption.CASCADE)
    val roleId = long("role_id").uniqueIndex()
}