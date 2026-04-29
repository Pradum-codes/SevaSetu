package com.example.sevasetu.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

data class ResolvedLocationAddress(
    val lat: Double,
    val lng: Double,
    val addressText: String?,
    val locality: String?,
    val district: String?,
    val state: String?,
    val pinCode: String?,
    val matchedDistrict: JurisdictionConstants.District?
)

class LocationAddressResolver(context: Context) {
    private val appContext = context.applicationContext

    suspend fun resolve(lat: Double, lng: Double): ResolvedLocationAddress {
        val address = reverseGeocode(lat, lng)
        val locality = address?.bestLocality()
        val district = address?.subAdminArea?.takeIf { it.isNotBlank() }
        return ResolvedLocationAddress(
            lat = lat,
            lng = lng,
            addressText = address?.formattedAddress(),
            locality = locality,
            district = district,
            state = address?.adminArea?.takeIf { it.isNotBlank() },
            pinCode = address?.postalCode?.takeIf { it.isNotBlank() },
            matchedDistrict = JurisdictionConstants.findDistrictForAddress(locality, district)
        )
    }

    private suspend fun reverseGeocode(lat: Double, lng: Double): Address? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null

        runCatching {
            @Suppress("DEPRECATION")
            Geocoder(appContext, Locale.getDefault())
                .getFromLocation(lat, lng, 1)
                ?.firstOrNull()
        }.recoverCatching { error ->
            if (error is IOException) null else throw error
        }.getOrNull()
    }

    private fun Address.formattedAddress(): String? {
        return when {
            maxAddressLineIndex >= 0 -> getAddressLine(0)
            else -> listOfNotNull(
                featureName,
                thoroughfare,
                subLocality,
                locality,
                subAdminArea,
                adminArea,
                postalCode
            )
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", ")
                .takeIf { it.isNotBlank() }
        }
    }

    private fun Address.bestLocality(): String? {
        return listOf(locality, subLocality, thoroughfare, featureName)
            .firstOrNull { !it.isNullOrBlank() }
    }
}
