package com.example.sevasetu.data.remote.api

import com.example.sevasetu.data.remote.dto.NearbyIssuesResponse
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
}
