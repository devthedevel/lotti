package net.devthedevel.lotti.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object LotteryTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val guildIndex = integer("guild_index").references(GuildOptionsTable.id, ReferenceOption.CASCADE)
    val channelId = long("channel_id").uniqueIndex()
    val creator = long("creator_id")
}