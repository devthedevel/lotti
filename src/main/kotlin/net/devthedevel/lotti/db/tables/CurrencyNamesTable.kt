package net.devthedevel.lotti.db.tables

import org.jetbrains.exposed.sql.Table

object CurrencyNamesTable: Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val name = varchar("currency_name", 50).uniqueIndex()

    const val DEFAULT_CURRENCY_INDEX: Int = 1
    val CURRENCY_VALUES: List<String> = listOf("Gold", "Crowns", "Gems", "Silver", "Coins", "Dollars", "Bucks", "Bills", "Skulls", "Rupees", "Yen", "Rubles",
            "Pounds", "Pesos", "Bones", "Notes", "Clams", "Dimes", "Doubloons", "Fivers", "Hundies", "Nuggets", "Stacks", "Tenners")
}