package net.devthedevel.lotti.db.dto

import net.devthedevel.lotti.db.OperationStatus

data class DatabaseResult(val result: Any? = null, val message: String? = null, override var operationStatus: OperationStatus = OperationStatus.FAILED) : IOperationStatus