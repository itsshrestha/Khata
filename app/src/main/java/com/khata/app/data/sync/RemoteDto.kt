package com.khata.app.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteCustomer(
    val id: String,
    @SerialName("shop_id") val shopId: String,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null,
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class RemoteTransaction(
    val id: String,
    @SerialName("shop_id") val shopId: String,
    @SerialName("customer_id") val customerId: String,
    val type: String,
    val amount: Long,
    val description: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("transaction_date") val transactionDate: Long,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null,
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class RemoteTransactionItem(
    val id: String,
    @SerialName("shop_id") val shopId: String,
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("item_name") val itemName: String,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Long,
    @SerialName("total_price") val totalPrice: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null,
)

@Serializable
data class RemoteCustomerLegacy(
    val id: String,
    @SerialName("shop_id") val shopId: String,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null,
)

@Serializable
data class RemoteTransactionLegacy(
    val id: String,
    @SerialName("shop_id") val shopId: String,
    @SerialName("customer_id") val customerId: String,
    val type: String,
    val amount: Long,
    val description: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("transaction_date") val transactionDate: Long,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null,
)

