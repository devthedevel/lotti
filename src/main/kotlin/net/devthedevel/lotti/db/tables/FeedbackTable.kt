package net.devthedevel.lotti.db.tables

import org.jetbrains.exposed.sql.Table

object FeedbackTable : Table() {
    val date = date("date")
    val version = varchar("version", 20)
    val userId = long("user_id")
    val feedback = text("feedback")
}