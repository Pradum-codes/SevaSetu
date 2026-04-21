package com.example.sevasetu.data.repository

import com.example.sevasetu.data.remote.api.IssueApi
import com.example.sevasetu.data.remote.dto.IssueDto
import org.json.JSONObject

class IssueRepository(
    private val issueApi: IssueApi
) {
    suspend fun getNearbyIssues(districtId: String): Result<List<IssueDto>> {
        return runCatching {
            val response = issueApi.getNearbyIssues(districtId = districtId)
            if (!response.isSuccessful) {
                val message = extractErrorMessage(response.errorBody()?.string())
                throw IllegalStateException(message ?: "Failed to fetch nearby issues (${response.code()})")
            }

            val issues = response.body()?.issues.orEmpty()
            issues.filter { issue ->
                issue.lat != null && issue.lng != null
            }
        }
    }

    private fun extractErrorMessage(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            when {
                json.has("message") -> json.optString("message")
                json.has("error") -> json.optString("error")
                else -> null
            }
        }.getOrNull()
    }
}
