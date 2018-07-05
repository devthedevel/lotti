package net.devthedevel.lotti.db

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.commands.AdminOperation
import net.devthedevel.lotti.commands.AdminOptions
import net.devthedevel.lotti.db.dto.ChannelStatus
import net.devthedevel.lotti.db.dto.DatabaseResult
import net.devthedevel.lotti.db.tables.*
import org.apache.commons.lang3.mutable.Mutable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.util.PSQLException
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IUser
import java.sql.DriverManager

object LotteryDatabase {

    private var db: Database = Database.connect({DriverManager.getConnection(Lotti.DATABASE_URL)})

    fun initTables() {
        transaction(db) {
            create (GuildOptionsTable, AdminRolesTable, LotteryTable, UserTicketsTable)
        }
    }

    fun createNewLotto(guild: IGuild, channel:IChannel, user: IUser): OperationStatus {
        var dbResult = OperationStatus.FAILED
        transaction(db) {
            try {
                LotteryTable.insert {
                    it[guildIndex] = GuildOptionsTable.slice(GuildOptionsTable.id).select({ GuildOptionsTable.guildId eq guild.longID}).single()[GuildOptionsTable.id]
                    it[channelId] = channel.longID
                    it[creator] = user.longID
                }

                dbResult = OperationStatus.COMPLETED
            } catch (e: ExposedSQLException) {
                val cause = e.cause
                if (cause is PSQLException) {
                    if (cause.sqlState == "23505") {
                        dbResult = OperationStatus.EXISTS
                    }
                }
            }
        }

        return dbResult
    }

    fun setDefaultGuildOptions(guild: IGuild): DatabaseResult {
        var dbResult = DatabaseResult()
        transaction(db) {
            try {
                val gid = GuildOptionsTable.insertIgnore {
                    it[guildId] = guild.longID
                    it[currency] = GuildOptionsTable.DEFAULT_CURRENCY_NAME
                    it[ticketPrice] = GuildOptionsTable.DEFAULT_TICKET_PRICE
                } get GuildOptionsTable.id

                dbResult = DatabaseResult(operationStatus = OperationStatus.COMPLETED, result = gid)
            } catch (e: Exception) {
                val cause: String? = e.cause?.message
                println(cause)
            }
        }

        return dbResult
    }

    fun deleteGuild(guild: IGuild): DatabaseResult {
        var dbResult = DatabaseResult()
        transaction(db) {
            try {
                GuildOptionsTable.deleteWhere { GuildOptionsTable.guildId eq guild.longID}

                dbResult = DatabaseResult(operationStatus = OperationStatus.COMPLETED)
            } catch (e: Exception) {
                val cause: String? = e.cause?.message
                println(cause)
            }
        }

        return dbResult
    }

    fun userBuyTickets(guild: IGuild, channel: IChannel, user: IUser, numTickets: Int): DatabaseResult {
        var dbResult = DatabaseResult()
        var userTickets = numTickets
        transaction(db) {
            try {
                TransactionManager.current().exec("INSERT INTO usertickets (lotto_index, user_id, tickets) VALUES (" +
                        "(SELECT lot.id FROM lottery AS lot WHERE lot.channel_id = ${channel.longID} AND lot.guild_index =" +
                        "(SELECT go.id FROM guildoptions AS go WHERE go.guild_id = ${guild.longID})), ${user.longID}, $numTickets) ON CONFLICT ON CONSTRAINT pk_usertickets DO UPDATE SET tickets = usertickets.tickets + EXCLUDED.tickets") {rs ->
                    if (rs.next()) { }
                }

                TransactionManager.current().exec("SELECT ut.tickets FROM usertickets AS ut WHERE ut.user_id = ${user.longID} AND ut.lotto_index = (SELECT lot.id FROM lottery AS lot WHERE lot.channel_id = ${channel.longID} AND lot.guild_index = (SELECT go.id FROM guildoptions AS go WHERE go.guild_id = ${guild.longID}))") {rs ->
                    if (rs.next()) {
                        userTickets = rs.getInt(UserTicketsTable.tickets.name)
                    }
                }

                dbResult = DatabaseResult(operationStatus = OperationStatus.COMPLETED, result = userTickets)
            } catch (e: Exception) {
                val cause: String? = e.cause?.message
                println(cause)
                dbResult = when {
                    cause?.contains("is not present in table") ?: false -> DatabaseResult(operationStatus = OperationStatus.DOES_NOT_EXIST, message = cause)
                    else -> DatabaseResult(message = cause)
                }
            }
        }
        return dbResult
    }

    fun getUsersInLotto(guild: IGuild, channel: IChannel): DatabaseResult {
        var dbResult = DatabaseResult()
        transaction(db) {
            try {
                val userTicketList: MutableList<Long> = mutableListOf()

                TransactionManager.current().exec("SELECT ut.user_id, ut.tickets FROM usertickets AS ut WHERE ut.lotto_index = (SELECT lot.id FROM lottery AS lot WHERE lot.channel_id = ${channel.longID} AND lot.guild_index = (SELECT go.id FROM guildoptions AS go WHERE go.guild_id = ${guild.longID}))") {rs ->
                    while (rs.next()) {
                        for (i: Int in 0 until rs.getInt(UserTicketsTable.tickets.name)) {
                            userTicketList.add(rs.getLong(UserTicketsTable.userId.name))
                        }
                    }
                }

                dbResult = DatabaseResult(operationStatus = OperationStatus.COMPLETED, result = userTicketList)
            } catch (e: Exception) {
                val cause: String? = e.cause?.message
                println(cause)
            }
        }

        return dbResult
    }

    fun deleteLotto(guild: IGuild, channel: IChannel): DatabaseResult {
        var dbResult = DatabaseResult()
        transaction(db) {
            try {
//                LotteryTable.deleteWhere { (LotteryTable.guildId eq guild.longID) and (LotteryTable.channelId eq channel.longID) }

                TransactionManager.current().exec("DELETE FROM lottery AS lot WHERE lot.channel_id = ${channel.longID} AND lot.guild_index = (SELECT go.id FROM guildoptions AS go WHERE go.guild_id = ${guild.longID})")

                dbResult = DatabaseResult(operationStatus = OperationStatus.COMPLETED)
            } catch (e: Exception) {
                val cause: String? = e.cause?.message
                println(cause)
            }
        }

        return dbResult
    }

    fun getChannelStatus(guild: IGuild, channel: IChannel): ChannelStatus {
        val channelStatus = ChannelStatus()
        transaction(db) {
            try {
                TransactionManager.current().exec("SELECT go.ticket_price, go.currency, lot.creator_id, ut.user_id, ut.tickets FROM guildoptions AS go" +
                        "  LEFT JOIN currencynames AS cn on go.currency_index = cn.id" +
                        "  LEFT JOIN usertickets AS ut on lot.id = ut.lotto_index" +
                        "  WHERE go.guild_id = ${guild.longID} AND lot.channel_id = ${channel.longID}") {rs ->
                    var lottoExists = false
                    if (rs.next()) {
                        val creatorId = rs.getLong(LotteryTable.creator.name)
                        val currencyName = rs.getString(GuildOptionsTable.currency.name)
                        val ticketPrice = rs.getInt(GuildOptionsTable.ticketPrice.name)
                        val userTickets = Pair(rs.getLong(UserTicketsTable.userId.name), rs.getInt(UserTicketsTable.tickets.name))

                        channelStatus.creatorId = creatorId
                        channelStatus.currencyName = currencyName
                        channelStatus.ticketPrice = ticketPrice

                        if (userTickets.first != 0L && userTickets.second != 0) {
                            channelStatus.userTickets.add(userTickets)
                        }
                        lottoExists = true
                    }
                    while (rs.next()) {
                        channelStatus.userTickets.add(Pair(rs.getLong(UserTicketsTable.userId.name), rs.getInt(UserTicketsTable.tickets.name)))
                    }
                    channelStatus.operationStatus = if (lottoExists) OperationStatus.COMPLETED else OperationStatus.DOES_NOT_EXIST
                }
            } catch (e: Exception) {
                val cause: String? = e.cause?.message
                println(cause)
            }
        }

        return channelStatus
    }

    fun setAdminOptions(guild: IGuild, channel: IChannel, options: AdminOptions) {
        transaction(db) {

            val guildIndex = GuildOptionsTable.slice(GuildOptionsTable.id).select({ GuildOptionsTable.guildId eq guild.longID}).single()[GuildOptionsTable.id]

            when (options.adminOperation) {
                AdminOperation.SET -> {
                    try {

                        //SET roles
                        if (options.roles.isNotEmpty()) {
                            AdminRolesTable.deleteWhere { AdminRolesTable.guildId eq guildIndex}

                            AdminRolesTable.batchInsert(options.roles) { role ->
                                this[AdminRolesTable.guildId] = guildIndex
                                this[AdminRolesTable.roleId] = role.longID
                            }
                        } else {} //TODO look into this

                        //TODO look into combining price and currency into one query

                        //SET price
                        if (options.price != null) {
                            GuildOptionsTable.update({GuildOptionsTable.guildId eq guild.longID}) {
                                it[ticketPrice] = options.price
                            }
                        } else {}

                        //SET currency
                        if (options.currency != null) {
                            GuildOptionsTable.update({GuildOptionsTable.guildId eq guild.longID}) {
                                it[currency] = options.currency
                            }
                        } else {}

                    } catch (e: ExposedSQLException) { }
                }
                AdminOperation.ADD -> {
                    try {
                        AdminRolesTable.batchInsert(options.roles, true) { role ->
                            this[AdminRolesTable.guildId] = guildIndex
                            this[AdminRolesTable.roleId] = role.longID
                        }
                    } catch (e: ExposedSQLException) { }
                }
                else -> {}
            }
        }
    }

    fun getAdminOptions(guild: IGuild): Triple<OperationStatus, AdminOptions, MutableList<Long>> {
        var dbResult = Triple(OperationStatus.FAILED, AdminOptions(), mutableListOf<Long>())
        transaction(db) {
            val options: AdminOptions
            val resultRow = GuildOptionsTable.slice(GuildOptionsTable.id,
                    GuildOptionsTable.currency,
                    GuildOptionsTable.ticketPrice).select({ GuildOptionsTable.guildId eq guild.longID}).single()

            val guildIndex = resultRow[GuildOptionsTable.id]
            options = AdminOptions(AdminOperation.GET, resultRow[GuildOptionsTable.currency], resultRow[GuildOptionsTable.ticketPrice])

            val roles = mutableListOf<Long>()
            AdminRolesTable.slice(AdminRolesTable.roleId).select({ AdminRolesTable.guildId eq guildIndex}).mapTo(roles) {it[AdminRolesTable.roleId]}

            dbResult = Triple(OperationStatus.COMPLETED, options, roles)
            println("")
        }

        return dbResult
    }
}