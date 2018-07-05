package net.devthedevel.lotti.commands.admin

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