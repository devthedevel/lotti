package net.devthedevel.lotti.db.tables

import org.jetbrains.exposed.sql.Table

object GuildOptionsTable: Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val guildId = long("guild_id").uniqueIndex()
    val currencyIndex = integer("currency_index").references(CurrencyNamesTable.id)
    val ticketPrice = integer("ticket_price")

    const val DEFAULT_TICKET_PRICE: Int = 10
}