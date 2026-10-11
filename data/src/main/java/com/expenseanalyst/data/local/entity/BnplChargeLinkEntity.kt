package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * A card charge linked by hand to a Tabby/Tamara plan it pays (DB v35). See
 * [com.expenseanalyst.domain.model.BnplChargeLink]. No foreign keys: expenses are soft-deleted,
 * and BnplPlans skips links whose charge or purchase is gone.
 */
@Entity(
    tableName = "bnpl_charge_links",
    primaryKeys = ["charge_id", "purchase_id"],
    indices = [Index(value = ["purchase_id"])]
)
data class BnplChargeLinkEntity(
    @ColumnInfo(name = "charge_id") val chargeId: Long,
    @ColumnInfo(name = "purchase_id") val purchaseId: Long
)
