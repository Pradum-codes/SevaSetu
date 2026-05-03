package com.example.sevasetu.data.remote.api

import com.example.sevasetu.data.remote.dto.CreateIssueRequest
import com.example.sevasetu.data.remote.dto.CreateIssueResponse
import com.example.sevasetu.data.remote.dto.DashboardResponse
import com.example.sevasetu.data.remote.dto.NearbyIssuesResponse
import com.example.sevasetu.data.remote.dto.ReportsIssuesResponse
import com.example.sevasetu.data.remote.dto.VoteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface IssueApi {

    @GET("/issues/nearby")
    suspend fun getNearbyIssues(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radiusKm") radiusKm: Double = 5.0,
        @Query("districtId") districtId: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<NearbyIssuesResponse>

    @GET("/issues/reports")
    suspend fun getMyReportedIssues(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("status") status: String? = null
    ): Response<ReportsIssuesResponse>

    @POST("/issues")
    suspend fun createIssue(
        @Body request: CreateIssueRequest
    ): Response<CreateIssueResponse>

    @POST("/issues/{issueId}/vote")
    suspend fun voteIssue(
        @Path("issueId") issueId: String
    ): Response<VoteResponse>

    @GET("/dashboard")
    suspend fun getDashboard(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radiusKm") radiusKm: Double = 5.0,
        @Query("districtId") districtId: String? = null,
        @Query("insightWindowDays") insightWindowDays: Int = 30
    ): Response<DashboardResponse>
}
