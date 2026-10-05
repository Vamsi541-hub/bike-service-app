package com.example.data.auth

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("bikecare_session", Context.MODE_PRIVATE)

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun clear() {
        prefs.edit().remove(KEY_USER_ID).apply()
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
    }
}
