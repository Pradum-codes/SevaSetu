package com.example.sevasetu.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NearbyIssuesResponse(
    @SerializedName("searchMode") val searchMode: String, // "location" or "district"
    @SerializedName("location") val location: LocationSearchInfo? = null,
    @SerializedName("district") val district: DistrictSearchInfo? = null,
    @SerializedName("page") val page: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("issues") val issues: List<IssueDto>
)

data class LocationSearchInfo(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("radiusKm") val radiusKm: Double
)

data class DistrictSearchInfo(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String
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
    @SerializedName("description") val description: String? = null,
    @SerializedName("categoryId") val categoryId: Int? = null,
    @SerializedName("category") val category: IssueCategoryDto? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("addressText") val addressText: String?,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("priority") val priority: String?,
    @SerializedName("images") val images: List<IssueImageDto> = emptyList(),
    @SerializedName("imageUrls") val imageUrls: List<String>? = null,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("voteCount") val voteCount: Int = 0,
    @SerializedName("isVotedByMe") val isVotedByMe: Boolean? = null
)

data class VoteResponse(
    @SerializedName("voted") val voted: Boolean,
    @SerializedName("totalVotes") val totalVotes: Int
)

data class IssueCategoryDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class IssueImageDto(
    @SerializedName("imageUrl") val imageUrl: String?
)

data class DashboardResponse(
    @SerializedName("searchContext") val searchContext: SearchContext,
    @SerializedName("myReportsSnapshot") val myReportsSnapshot: MyReportsSnapshot,
    @SerializedName("myPendingAction") val myPendingAction: MyPendingAction,
    @SerializedName("nearbyRiskSummary") val nearbyRiskSummary: NearbyRiskSummary,
    @SerializedName("nearbyInsights") val nearbyInsights: NearbyInsights,
    @SerializedName("generatedAt") val generatedAt: String? = null
)

data class SearchContext(
    @SerializedName("mode") val mode: String, // "location" or "district"
    @SerializedName("location") val location: LocationSearchInfo? = null,
    @SerializedName("district") val district: DistrictSearchInfo? = null
)

data class MyReportsSnapshot(
    @SerializedName("open") val open: Int,
    @SerializedName("inProgress") val inProgress: Int,
    @SerializedName("resolved") val resolved: Int,
    @SerializedName("rejected") val rejected: Int,
    @SerializedName("total") val total: Int
)

data class MyPendingAction(
    @SerializedName("unresolved") val unresolved: Int,
    @SerializedName("cta") val cta: CtaAction,
    @SerializedName("label") val label: String? = null
)

data class CtaAction(
    @SerializedName("type") val type: String,
    @SerializedName("filters") val filters: List<String>,
    @SerializedName("label") val label: String
)

data class NearbyRiskSummary(
    @SerializedName("highPriority") val highPriority: Int,
    @SerializedName("open") val open: Int,
    @SerializedName("totalNearby") val totalNearby: Int,
    @SerializedName("coverageText") val coverageText: String
)

data class NearbyInsights(
    @SerializedName("open") val open: Int,
    @SerializedName("inProgress") val inProgress: Int,
    @SerializedName("closed") val closed: Int
)
