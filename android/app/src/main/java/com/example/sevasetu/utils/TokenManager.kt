package com.example.sevasetu.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        AUTH_PREFS,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        prefs.edit { putString(KEY_JWT, token) }
    }

    fun getToken(): String? = prefs.getString(KEY_JWT, null)

    fun saveUserDistrict(districtId: String) {
        prefs.edit { putString(KEY_USER_DISTRICT, districtId) }
    }

    fun getUserDistrict(): String? = prefs.getString(KEY_USER_DISTRICT, null)

    fun addVotedIssue(issueId: String) {
        val voted = getVotedIssues().toMutableSet()
        voted.add(issueId)
        prefs.edit { putStringSet(KEY_VOTED_ISSUES, voted) }
    }

    fun removeVotedIssue(issueId: String) {
        val voted = getVotedIssues().toMutableSet()
        voted.remove(issueId)
        prefs.edit { putStringSet(KEY_VOTED_ISSUES, voted) }
    }

    fun getVotedIssues(): Set<String> = prefs.getStringSet(KEY_VOTED_ISSUES, emptySet()) ?: emptySet()

    fun clear() {
        prefs.edit { clear() }
    }

    companion object {
        private const val AUTH_PREFS = "auth_prefs"
        private const val KEY_JWT = "jwt"
        private const val KEY_USER_DISTRICT = "user_district"
        private const val KEY_VOTED_ISSUES = "voted_issues"
    }
}
