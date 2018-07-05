package net.devthedevel.lotti.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object LotteryTicketsTable : Table() {
    val lottoIndex = integer("lotto_index").references(LotteryTable.id, ReferenceOption.CASCADE).primaryKey()
    val userId = long("user_id").primaryKey()
    val approved = integer("approved")
    val requested = integer("requested")
}