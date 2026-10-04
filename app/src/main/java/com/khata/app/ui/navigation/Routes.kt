package com.khata.app.ui.navigation

/** Route strings and builders for every non-bottom-bar screen. */
object Routes {
    const val ARG_CUSTOMER_ID = "customerId"

    /** SavedStateHandle key used to pass a one-time success message back to the previous screen. */
    const val RESULT_MESSAGE = "result_message"

    const val ADD_CUSTOMER = "add_customer"
    const val EDIT_CUSTOMER = "edit_customer/{$ARG_CUSTOMER_ID}"
    const val CUSTOMER_DETAILS = "customer/{$ARG_CUSTOMER_ID}"
    const val ADD_CREDIT = "add_credit/{$ARG_CUSTOMER_ID}"
    const val RECORD_PAYMENT = "record_payment/{$ARG_CUSTOMER_ID}"

    const val CUSTOMER_STATEMENT = "customer_statement/{$ARG_CUSTOMER_ID}"

    fun editCustomer(customerId: String) = "edit_customer/$customerId"
    fun customerDetails(customerId: String) = "customer/$customerId"
    fun addCredit(customerId: String) = "add_credit/$customerId"
    fun recordPayment(customerId: String) = "record_payment/$customerId"
    fun customerStatement(customerId: String) = "customer_statement/$customerId"
}
