package net.devthedevel.lotti.db.tables

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object UserTicketsTable : Table() {
    val lottoIndex = integer("lotto_index").references(LotteryTable.id, ReferenceOption.CASCADE).primaryKey()
    val userId: Column<Long> = long("user_id").primaryKey()
    val tickets: Column<Int> = integer("tickets")
}