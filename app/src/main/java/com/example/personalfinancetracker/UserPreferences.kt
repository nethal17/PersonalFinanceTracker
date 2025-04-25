package com.example.personalfinancetracker

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UserPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "user_preferences", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    fun getSharedPreferences(): SharedPreferences {
        return sharedPreferences
    }

    fun saveUser(user: User) {
        // Get existing users
        val users = getUsers().toMutableList()

        // Check if username already exists
        val existingUserIndex = users.indexOfFirst { it.username == user.username }
        if (existingUserIndex != -1) {
            // Replace existing user
            users[existingUserIndex] = user
        } else {
            // Add new user
            users.add(user)
        }

        // Save updated list
        saveUsers(users)
    }

    fun saveUsers(users: List<User>) {
        val usersJson = gson.toJson(users)
        sharedPreferences.edit().putString(KEY_USERS, usersJson).apply()
    }

    fun getUsers(): List<User> {
        val usersJson = sharedPreferences.getString(KEY_USERS, null)
        return if (usersJson != null) {
            val type = object : TypeToken<List<User>>() {}.type
            gson.fromJson(usersJson, type)
        } else {
            emptyList()
        }
    }

    fun validateUser(username: String, password: String): Boolean {
        val users = getUsers()
        return users.any { it.username == username && it.password == password }
    }

    fun saveLoginState(isLoggedIn: Boolean, username: String = "") {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            .putString(KEY_CURRENT_USER, username)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getCurrentUsername(): String {
        return sharedPreferences.getString(KEY_CURRENT_USER, "") ?: ""
    }

    fun logout() {
        saveLoginState(false)
    }

    companion object {
        private const val KEY_USERS = "users"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CURRENT_USER = "current_user"
    }
}