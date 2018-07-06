package net.devthedevel.lotti.db

import net.devthedevel.lotti.Lotti
import net.devthedevel.lotti.commands.admin.AdminOperation
import net.devthedevel.lotti.commands.admin.AdminOptions
import net.devthedevel.lotti.commands.admin.AdminRequests
import net.devthedevel.lotti.db.dto.ChannelStatus
import net.devthedevel.lotti.db.dto.DatabaseResult
import net.devthedevel.lotti.db.dto.Lottery
import net.devthedevel.lotti.db.dto.UserTickets
import net.devthedevel.lotti.db.tables.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.util.PSQLException
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IRole
import sx.blah.discord.handle.obj.IUser
import java.sql.DriverManager

object LotteryDatabase {

    private var db: Database = Database.connect({DriverManager.getConnection(Lotti.DATABASE_URL)})

    fun initTables() {
        transaction(db) {
            create (GuildOptionsTable, AdminRolesTable, LotteryTable, TicketTable)
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

    fun userBuyTickets(guild: IGuild, channel: IChannel, user: IUser, numTickets: Int, isAdmin: Boolean): Triple<OperationStatus, Int, Int> {
        var dbResult = Triple(OperationStatus.FAILED, 0, numTickets)
        transaction(db) {
            try {
                //TODO Figure out how to do the following statement in Exposed
                val ticketTable = TicketTable.tableName
                val lottoIndex = TicketTable.lottoIndex.name
                val userId = TicketTable.userId.name
                val approved = TicketTable.approved.name
                val requested = TicketTable.requested.name

                val lotteryTable = LotteryTable.tableName
                val guildTable = GuildOptionsTable.tableName

                val sb = StringBuilder().apply {
                    append("INSERT INTO $ticketTable ($lottoIndex, $userId, $approved, $requested) VALUES ")
                    append("((SELECT lot.id FROM $lotteryTable AS lot WHERE lot.channel_id = ${channel.longID} AND lot.guild_index = (SELECT go.id FROM $guildTable AS go WHERE go.guild_id = ${guild.longID})),")
                    append("${user.longID}, ")
                    append("0, ")
                    append("$numTickets) ON CONFLICT ON CONSTRAINT pk_$ticketTable DO UPDATE SET ")

                    //Directly approve admin tickets
                    if (isAdmin) {
                        append("approved = $ticketTable.approved + EXCLUDED.requested")
                    } else {
                        append("requested = $ticketTable.requested + EXCLUDED.requested")
                    }
                }

                TransactionManager.current().exec(sb.toString()) {rs ->
                    if (rs.next()) { }
                }

                TransactionManager.current().exec(
                        "SELECT ut.approved, ut.requested FROM $ticketTable AS ut WHERE ut.user_id = ${user.longID} " +
                                "AND ut.lotto_index = (SELECT lot.id FROM $lotteryTable AS lot WHERE lot.channel_id = ${channel.longID} " +
                                "AND lot.guild_index = (SELECT go.id FROM $guildTable AS go WHERE go.guild_id = ${guild.longID}))") {rs ->
                    if (rs.next()) {
                        dbResult = Triple(OperationStatus.COMPLETED, rs.getInt(TicketTable.approved.name), rs.getInt(TicketTable.requested.name))
                    }
                }

            } catch (e: Exception) {
                val cause: String? = e.cause?.message
                dbResult = if (cause?.contains("is not present in table") ?: false) {
                    Triple(OperationStatus.DOES_NOT_EXIST, 0, 0)
                } else {
                    dbResult
                }
            }
        }
        return dbResult
    }

    fun getApprovedTickets(guild: IGuild, channel: IChannel): Pair<OperationStatus, Lottery> {
        var dbResult = Pair(OperationStatus.FAILED, Lottery())
        transaction(db) {
            try {
                val lottery = Lottery()

                val ticketTable = TicketTable.tableName
                val guildTable = GuildOptionsTable.tableName
                val lotteryTable = LotteryTable.tableName
                TransactionManager.current().exec("SELECT ut.user_id, ut.approved FROM $ticketTable AS ut " +
                        "WHERE ut.approved > 0 AND ut.lotto_index = (SELECT lot.id FROM $lotteryTable AS lot WHERE lot.channel_id = ${channel.longID} AND lot.guild_index = " +
                        "(SELECT go.id FROM $guildTable AS go WHERE go.guild_id = ${guild.longID}))") {rs ->
                    while (rs.next()) {
                        lottery.addUserTicket(rs.getLong(TicketTable.userId.name), rs.getInt(TicketTable.approved.name))
                    }
                }

                dbResult = Pair(OperationStatus.COMPLETED, lottery)
            } catch (e: ExposedSQLException) { }
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
                TransactionManager.current().exec("SELECT go.ticket_price, go.currency, lot.creator_id, ut.user_id, ut.requested FROM guildoptions AS go" +
                        "  LEFT JOIN currencynames AS cn on go.currency_index = cn.id" +
                        "  LEFT JOIN usertickets AS ut on lot.id = ut.lotto_index" +
                        "  WHERE go.guild_id = ${guild.longID} AND lot.channel_id = ${channel.longID}") {rs ->
                    var lottoExists = false
                    if (rs.next()) {
                        val creatorId = rs.getLong(LotteryTable.creator.name)
                        val currencyName = rs.getString(GuildOptionsTable.currency.name)
                        val ticketPrice = rs.getInt(GuildOptionsTable.ticketPrice.name)
                        val userTickets = Pair(rs.getLong(TicketTable.userId.name), rs.getInt(TicketTable.requested.name))

                        channelStatus.creatorId = creatorId
                        channelStatus.currencyName = currencyName
                        channelStatus.ticketPrice = ticketPrice

                        if (userTickets.first != 0L && userTickets.second != 0) {
                            channelStatus.userTickets.add(userTickets)
                        }
                        lottoExists = true
                    }
                    while (rs.next()) {
                        channelStatus.userTickets.add(Pair(rs.getLong(TicketTable.userId.name), rs.getInt(TicketTable.requested.name)))
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

    fun setAdminOptions(guild: IGuild, channel: IChannel, options: AdminOptions, op: AdminOperation) {
        transaction(db) {

            val guildIndex = GuildOptionsTable.slice(GuildOptionsTable.id).select({ GuildOptionsTable.guildId eq guild.longID}).single()[GuildOptionsTable.id]

            when (op) {
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

    fun getAdminOptions(guild: IGuild): Triple<OperationStatus, AdminOptions, MutableList<IRole>> {
        var dbResult = Triple(OperationStatus.FAILED, AdminOptions(), mutableListOf<IRole>())
        transaction(db) {
            val options: AdminOptions
            val resultRow = GuildOptionsTable.slice(GuildOptionsTable.id,
                    GuildOptionsTable.currency,
                    GuildOptionsTable.ticketPrice).select({ GuildOptionsTable.guildId eq guild.longID}).single()

            val guildIndex = resultRow[GuildOptionsTable.id]
            options = AdminOptions(resultRow[GuildOptionsTable.currency], resultRow[GuildOptionsTable.ticketPrice])

            val roles = mutableListOf<IRole>()
            AdminRolesTable.slice(AdminRolesTable.roleId).select({ AdminRolesTable.guildId eq guildIndex}).mapTo(roles) {guild.getRoleByID(it[AdminRolesTable.roleId])}

            dbResult = Triple(OperationStatus.COMPLETED, options, roles)
            println("")
        }

        return dbResult
    }

    fun deleteAdminRoles(guild: IGuild, roles: List<Long>): OperationStatus {
        var dbResult = OperationStatus.FAILED
        transaction(this.db) {
            if (roles.isNotEmpty()) {
                AdminRolesTable.deleteWhere {
                    (AdminRolesTable.guildId eq (GuildOptionsTable.slice(GuildOptionsTable.id).select({GuildOptionsTable.guildId eq guild.longID})).single()[GuildOptionsTable.id]) and (AdminRolesTable.roleId inList roles)
                }
                dbResult = OperationStatus.COMPLETED
            }
        }
        return dbResult
    }

    fun getTicketRequests(guild: IGuild, channel: IChannel, requests: List<AdminRequests>?): Pair<OperationStatus, List<AdminRequests>> {
        var dbResult = Pair(OperationStatus.FAILED, listOf<AdminRequests>())
        transaction(this.db) {

            val guildId = GuildOptionsTable.slice(GuildOptionsTable.id).select({GuildOptionsTable.guildId eq guild.longID}).single()[GuildOptionsTable.id]
            val lottoId = LotteryTable.slice(LotteryTable.id).select{(LotteryTable.guildIndex eq guildId) and (LotteryTable.channelId eq channel.longID)}.single()[LotteryTable.id]

            val users = mutableListOf<AdminRequests>()
            val query: Query

            //Get only users listed in requests
            //If null or empty, get all users in table
            query = if (requests?.isNotEmpty() == true) {
                val userIds = mutableListOf<Long>()
                requests.mapNotNullTo(userIds) {it.user?.longID}
                TicketTable.slice(TicketTable.userId, TicketTable.requested).select{
                    (TicketTable.lottoIndex eq lottoId) and (TicketTable.userId inList userIds)}
            } else {
                TicketTable.slice(TicketTable.userId, TicketTable.requested).select({TicketTable.lottoIndex eq lottoId})
            }

            query.forEach {
                val requested = it[TicketTable.requested]
                //Ignore users that have no requested tickets
                if (requested != 0) { users.add(AdminRequests(guild.getUserByID(it[TicketTable.userId]), requested)) }
            }

            dbResult = if (users.isEmpty()) Pair(OperationStatus.NO_RESULT, users) else Pair(OperationStatus.COMPLETED, users)
        }

        return dbResult
    }

    fun approveTickets(guild: IGuild, channel: IChannel, users: List<AdminRequests> = listOf(), approveAll: Boolean = false): OperationStatus {
        var dbResult = OperationStatus.FAILED
        transaction(this.db) {
            val lottoIndex = LotteryTable.slice(LotteryTable.id).select {
                (LotteryTable.channelId eq channel.longID) and
                    (LotteryTable.guildIndex eq
                            (GuildOptionsTable.slice(GuildOptionsTable.id).select({GuildOptionsTable.guildId eq guild.longID}).single()[GuildOptionsTable.id]))
            }.single()[LotteryTable.id]

            if (approveAll) {
                TicketTable.update({TicketTable.lottoIndex eq lottoIndex}) {
                    with(SqlExpressionBuilder) {
                        it.update(TicketTable.approved, TicketTable.approved + TicketTable.requested)
                        it.update(TicketTable.requested, QueryParameter(0, IntegerColumnType()))
                    }
                }
                dbResult = OperationStatus.COMPLETED
            } else {

            }
        }

        return dbResult
    }
}