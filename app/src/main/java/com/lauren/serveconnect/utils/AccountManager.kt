package com.lauren.serveconnect.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class RememberedAccount(
    val email: String,
    val fullName: String,
    val role: String,
    val uid: String,
    val pass: String, // Note: In a production app, use biometric auth or encrypted tokens instead of storing passwords
    val lastLogin: Long = System.currentTimeMillis()
)

class AccountManager(context: Context) {
    private val prefs = context.getSharedPreferences("serveconnect_accounts", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveAccount(account: RememberedAccount) {
        val accounts = getAccounts().toMutableList()
        // Remove existing entry for same email to update it
        accounts.removeAll { it.email == account.email }
        accounts.add(account)
        
        val json = gson.toJson(accounts)
        prefs.edit().putString("saved_accounts", json).apply()
    }

    fun getAccounts(): List<RememberedAccount> {
        val json = prefs.getString("saved_accounts", null) ?: return emptyList()
        val type = object : TypeToken<List<RememberedAccount>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun removeAccount(email: String) {
        val accounts = getAccounts().toMutableList()
        accounts.removeAll { it.email == email }
        val json = gson.toJson(accounts)
        prefs.edit().putString("saved_accounts", json).apply()
    }
}
