package com.example.sevasetu.data.repository

import com.example.sevasetu.data.remote.api.IssueApi
import com.example.sevasetu.data.remote.dto.CreateIssueRequest
import com.example.sevasetu.data.remote.dto.CreateIssueResponse
import com.example.sevasetu.data.remote.dto.IssueDto
import com.example.sevasetu.data.remote.dto.ReportsIssuesResponse
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

    suspend fun getMyReportedIssues(
        page: Int = 1,
        limit: Int = 50,
        status: String? = null
    ): Result<ReportsIssuesResponse> {
        return runCatching {
            val safePage = page.coerceAtLeast(1)
            val safeLimit = limit.coerceIn(1, 100)
            val response = issueApi.getMyReportedIssues(
                page = safePage,
                limit = safeLimit,
                status = status
            )

            if (!response.isSuccessful) {
                val message = extractErrorMessage(response.errorBody()?.string())
                throw IllegalStateException(message ?: "Failed to fetch reports (${response.code()})")
            }

            requireNotNull(response.body()) { "reports response body is null" }
        }
    }

    suspend fun createIssue(request: CreateIssueRequest): Result<CreateIssueResponse> {
        return runCatching {
            val response = issueApi.createIssue(request)
            if (!response.isSuccessful) {
                val message = extractErrorMessage(response.errorBody()?.string())
                throw IllegalStateException(message ?: "Failed to create issue (${response.code()})")
            }

            requireNotNull(response.body()) { "create issue response body is null" }
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
