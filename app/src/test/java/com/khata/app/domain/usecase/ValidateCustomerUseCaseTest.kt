package com.khata.app.domain.usecase

import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateCustomerUseCaseTest {

    private val validate = ValidateCustomerUseCase()

    @Test
    fun `empty name is rejected`() {
        assertEquals(
            Outcome.Failure(KhataError.EmptyCustomerName),
            validate("", null, null, null),
        )
    }

    @Test
    fun `blank name is rejected`() {
        assertEquals(
            Outcome.Failure(KhataError.EmptyCustomerName),
            validate("   ", "9812345678", null, null),
        )
    }

    @Test
    fun `name only is accepted and optional fields become null`() {
        val result = validate("  Ram Bahadur ", "", "  ", null) as Outcome.Success
        assertEquals(CustomerInput("Ram Bahadur", null, null, null), result.value)
    }

    @Test
    fun `valid phone numbers are accepted`() {
        for (phone in listOf("9812345678", "+977 981-234-5678", "014123456")) {
            val outcome = validate("Ram", phone, null, null)
            assertEquals("phone $phone", true, outcome is Outcome.Success)
        }
    }

    @Test
    fun `invalid phone is rejected`() {
        for (phone in listOf("abc", "123", "98123456789012345", "98xxxxxxxx")) {
            assertEquals(
                "phone $phone",
                Outcome.Failure(KhataError.InvalidPhone),
                validate("Ram", phone, null, null),
            )
        }
    }

    @Test
    fun `overly long name is rejected`() {
        assertEquals(
            Outcome.Failure(KhataError.CustomerNameTooLong),
            validate("a".repeat(101), null, null, null),
        )
    }
}
