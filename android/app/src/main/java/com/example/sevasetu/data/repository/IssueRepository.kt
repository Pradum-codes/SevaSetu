package com.example.sevasetu.data.repository

import com.example.sevasetu.data.remote.api.IssueApi
import com.example.sevasetu.data.remote.dto.CreateIssueRequest
import com.example.sevasetu.data.remote.dto.CreateIssueResponse
import com.example.sevasetu.data.remote.dto.DashboardResponse
import com.example.sevasetu.data.remote.dto.NearbyIssuesResponse
import com.example.sevasetu.data.remote.dto.ReportsIssuesResponse
import com.example.sevasetu.data.remote.dto.IssueTimelineResponse
import com.example.sevasetu.data.remote.dto.VoteResponse
import org.json.JSONObject

class IssueRepository(
    private val issueApi: IssueApi
) {
    suspend fun voteIssue(issueId: String): Result<VoteResponse> {
        return runCatching {
            val response = issueApi.voteIssue(issueId)
            if (!response.isSuccessful) {
                val message = extractErrorMessage(response.errorBody()?.string())
                throw IllegalStateException(message ?: "Failed to vote for issue (${response.code()})")
            }
            requireNotNull(response.body()) { "vote response body is null" }
        }
    }

    suspend fun getIssueTimeline(issueId: String): Result<IssueTimelineResponse> {
        return runCatching {
            val response = issueApi.getIssueTimeline(issueId)
            if (!response.isSuccessful) {
                val message = extractErrorMessage(response.errorBody()?.string())
                throw IllegalStateException(message ?: "Failed to fetch timeline (${response.code()})")
            }
            requireNotNull(response.body()) { "timeline response body is null" }
        }
    }

    suspend fun getNearbyIssues(
        lat: Double? = null,
        lng: Double? = null,
        radiusKm: Double = 5.0,
        districtId: String? = null,
        page: Int = 1,
        limit: Int = 50
    ): Result<NearbyIssuesResponse> {
        return runCatching {
            val safePage = page.coerceAtLeast(1)
            val safeLimit = limit.coerceIn(1, 100)

            val response = issueApi.getNearbyIssues(
                lat = lat,
                lng = lng,
                radiusKm = radiusKm,
                districtId = districtId,
                page = safePage,
                limit = safeLimit
            )

            if (!response.isSuccessful) {
                val message = extractErrorMessage(response.errorBody()?.string())
                throw IllegalStateException(message ?: "Failed to fetch nearby issues (${response.code()})")
            }

            requireNotNull(response.body()) { "nearby issues response body is null" }
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

    suspend fun getDashboard(
        lat: Double? = null,
        lng: Double? = null,
        radiusKm: Double = 5.0,
        districtId: String? = null,
        insightWindowDays: Int = 30
    ): Result<DashboardResponse> {
        return runCatching {
            val response = issueApi.getDashboard(
                lat = lat,
                lng = lng,
                radiusKm = radiusKm,
                districtId = districtId,
                insightWindowDays = insightWindowDays
            )

            if (!response.isSuccessful) {
                val message = extractErrorMessage(response.errorBody()?.string())
                throw IllegalStateException(message ?: "Failed to fetch dashboard (${response.code()})")
            }

            requireNotNull(response.body()) { "dashboard response body is null" }
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
