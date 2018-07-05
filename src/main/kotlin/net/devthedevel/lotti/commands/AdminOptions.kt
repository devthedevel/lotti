package net.devthedevel.lotti.commands

import com.beust.klaxon.Json

data class AdminOptions(
        @Json(ignored = true)
        var adminOperation: AdminOperation = AdminOperation.GET,
        val currency: String? = null,
        val price: Int? = null,
        var roles: MutableList<String> = mutableListOf()
)

enum class AdminOperation(val opString: String) {
    SET("set"),
    ADD("add"),
    REMOVE("remove"),
    GET("get"),
    INVALID("");

    companion object {
        fun parseOperation(string: String): AdminOperation {
            for (op in enumValues<AdminOperation>()) {
                if (op.opString.equals(string, true)) {
                    return op
                }
            }
            return INVALID
        }
    }
}