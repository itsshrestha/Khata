package com.khata.app.data.security

import android.content.Context
import android.content.SharedPreferences

class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSecurityCode(): String {
        return prefs.getString(KEY_SECURITY_CODE, DEFAULT_CODE) ?: DEFAULT_CODE
    }

    fun setSecurityCode(newCode: String): Boolean {
        if (!newCode.matches(Regex("^[0-9]{4}$"))) {
            return false
        }
        prefs.edit().putString(KEY_SECURITY_CODE, newCode).apply()
        return true
    }

    fun verifyCode(input: String): Boolean {
        return input == getSecurityCode()
    }

    fun isAppLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "khata_security_prefs"
        private const val KEY_SECURITY_CODE = "security_code"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val DEFAULT_CODE = "0000"
    }
}
