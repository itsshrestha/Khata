package com.khata.app.utils

import com.khata.app.domain.model.KhataError

/** Maps domain errors to short, friendly messages. The UI never shows technical details. */
fun KhataError.toUserMessage(currency: CurrencyFormatter): String = when (this) {
    KhataError.EmptyCustomerName -> "Please enter the customer's name."
    KhataError.CustomerNameTooLong -> "The name is too long. Please shorten it."
    KhataError.InvalidPhone -> "Please enter a valid phone number."
    KhataError.CustomerNotFound -> "This customer could not be found."
    KhataError.InvalidAmount -> "Please enter an amount greater than zero."
    KhataError.AmountTooLarge -> "This amount is too large."
    KhataError.NoItems -> "Please add at least one item."
    KhataError.EmptyItemName -> "Every item needs a name."
    KhataError.InvalidItemQuantity -> "Item quantity must be greater than zero."
    KhataError.InvalidItemPrice -> "Item price must be greater than zero."
    KhataError.NoOutstandingBalance -> "This customer has no outstanding balance."
    is KhataError.PaymentExceedsOutstanding ->
        "Payment cannot exceed the outstanding balance of ${currency.format(outstanding)}."
    KhataError.TransactionNotFound -> "This transaction could not be found."
    KhataError.WouldCreateNegativeBalance ->
        "This change isn't allowed because the customer would end up having paid more than they owed. " +
            "Adjust the related payments first."
    KhataError.DetailedAmountLocked ->
        "The amount of a detailed credit follows its items and can't be edited here."
    KhataError.InvalidTransaction -> "This transaction is not valid. Please check the details."
    KhataError.DatabaseError -> "Something went wrong while saving. Please try again."
}
