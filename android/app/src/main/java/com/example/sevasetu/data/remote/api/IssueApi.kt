package com.example.sevasetu.data.remote.api

import com.example.sevasetu.data.remote.dto.NearbyIssuesResponse
import com.example.sevasetu.data.remote.dto.ReportsIssuesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface IssueApi {

    @GET("/issues/nearby")
    suspend fun getNearbyIssues(
        @Query("districtId") districtId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): Response<NearbyIssuesResponse>

    @GET("/issues/reports")
    suspend fun getMyReportedIssues(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("status") status: String? = null
    ): Response<ReportsIssuesResponse>
}
