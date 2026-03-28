package com.example.myapplication.utils

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)

    companion object {
        const val KEY_TOKEN = "TOKEN"
        const val KEY_NAME = "NAME"
        const val KEY_EMAIL = "EMAIL"
    }

    fun saveToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveName(name: String) = prefs.edit().putString(KEY_NAME, name).apply()
    fun getName(): String? = prefs.getString(KEY_NAME, null)

    fun saveEmail(email: String) = prefs.edit().putString(KEY_EMAIL, email).apply()
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clearSession() = prefs.edit().clear().apply()
}