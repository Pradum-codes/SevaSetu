package com.example.sevasetu.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NearbyIssuesResponse(
    @SerializedName("issues") val issues: List<IssueDto>
)

data class IssueDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
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
