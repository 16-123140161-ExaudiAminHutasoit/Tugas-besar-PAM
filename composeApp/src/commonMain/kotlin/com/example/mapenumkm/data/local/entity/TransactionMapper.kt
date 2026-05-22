package com.example.mapenumkm.data.local.entity

import com.example.mapenumkm.data.local.TransactionEntity
import com.example.mapenumkm.data.local.TransactionItemEntity
import com.example.mapenumkm.domain.model.Transaction
import com.example.mapenumkm.domain.model.TransactionItem
import kotlinx.datetime.Instant

fun TransactionEntity.toDomain(items: List<TransactionItemEntity>): Transaction {
    return Transaction(
        id = id,
        subtotal = subtotal,
        discount = discount,
        total = total,
        paymentAmount = payment_amount,
        changeAmount = change_amount,
        createdAt = Instant.fromEpochMilliseconds(created_at),
        items = items.map { it.toDomain() }
    )
}

fun TransactionItemEntity.toDomain(): TransactionItem {
    return TransactionItem(
        productId = product_id,
        productName = product_name,
        productPrice = product_price,
        quantity = quantity.toInt()
    )
}
