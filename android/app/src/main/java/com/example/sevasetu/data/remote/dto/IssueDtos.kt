package com.example.sevasetu.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NearbyIssuesResponse(
    @SerializedName("issues") val issues: List<IssueDto>
)

data class ReportsIssuesResponse(
    @SerializedName("page") val page: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("issues") val issues: List<IssueDto>
)

data class CreateIssueRequest(
    @SerializedName("clientId") val clientId: String,
    @SerializedName("categoryId") val categoryId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("addressText") val addressText: String,
    @SerializedName("locality") val locality: String? = null,
    @SerializedName("landmark") val landmark: String? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null,
    @SerializedName("imageUrls") val imageUrls: List<String>,
    @SerializedName("priority") val priority: String = "medium",
    @SerializedName("districtId") val districtId: String,
    @SerializedName("cityId") val cityId: String? = null,
    @SerializedName("wardId") val wardId: String? = null,
    @SerializedName("blockId") val blockId: String? = null,
    @SerializedName("panchayatId") val panchayatId: String? = null
)

data class CreateIssueResponse(
    @SerializedName("message") val message: String,
    @SerializedName("idempotent") val idempotent: Boolean,
    @SerializedName("issue") val issue: IssueDto
)

data class IssueDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("status") val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("addressText") val addressText: String?,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("priority") val priority: String?,
    @SerializedName("images") val images: List<IssueImageDto> = emptyList(),
    @SerializedName("imageUrls") val imageUrls: List<String>? = null,
    @SerializedName("imageUrl") val imageUrl: String?
)

data class IssueImageDto(
    @SerializedName("imageUrl") val imageUrl: String?
)
