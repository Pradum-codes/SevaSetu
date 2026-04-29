package com.example.sevasetu.utils

/**
 * Hardcoded Jurisdiction IDs for SevaSetu Registration Flow
 * These IDs correspond to the seeded data in the backend database (Punjab state with 5 districts)
 */
object JurisdictionConstants {
    
    // ==================== STATE ====================
    const val PUNJAB_STATE_ID = "10000000-0000-0000-0000-000000000000"
    const val PUNJAB_STATE_NAME = "Punjab"
    
    // ==================== URBAN DISTRICTS ====================
    
    // Amritsar (Urban)
    const val AMRITSAR_DISTRICT_ID = "20000001-0000-0000-0000-000000000000"
    const val AMRITSAR_DISTRICT_NAME = "Amritsar"
    const val AMRITSAR_CITY_ID = "20000001-1000-0000-0000-000000000000"
    const val AMRITSAR_CITY_NAME = "Amritsar City"
    const val AMRITSAR_ZONE_ID = "20000001-1100-0000-0000-000000000000"
    const val AMRITSAR_ZONE_NAME = "Zone 1"
    const val AMRITSAR_WARD_ID = "20000001-1110-0000-0000-000000000000"
    const val AMRITSAR_WARD_NAME = "Ward 5"
    
    // Ludhiana (Urban)
    const val LUDHIANA_DISTRICT_ID = "20000002-0000-0000-0000-000000000000"
    const val LUDHIANA_DISTRICT_NAME = "Ludhiana"
    const val LUDHIANA_CITY_ID = "20000002-1000-0000-0000-000000000000"
    const val LUDHIANA_CITY_NAME = "Ludhiana City"
    const val LUDHIANA_ZONE_ID = "20000002-1100-0000-0000-000000000000"
    const val LUDHIANA_ZONE_NAME = "Zone 2"
    const val LUDHIANA_WARD_ID = "20000002-1110-0000-0000-000000000000"
    const val LUDHIANA_WARD_NAME = "Ward 8"
    
    // Jalandhar (Urban)
    const val JALANDHAR_DISTRICT_ID = "20000003-0000-0000-0000-000000000000"
    const val JALANDHAR_DISTRICT_NAME = "Jalandhar"
    const val JALANDHAR_CITY_ID = "20000003-1000-0000-0000-000000000000"
    const val JALANDHAR_CITY_NAME = "Jalandhar City"
    const val JALANDHAR_ZONE_ID = "20000003-1100-0000-0000-000000000000"
    const val JALANDHAR_ZONE_NAME = "Zone 1"
    const val JALANDHAR_WARD_ID = "20000003-1110-0000-0000-000000000000"
    const val JALANDHAR_WARD_NAME = "Ward 12"
    
    // ==================== RURAL DISTRICTS ====================
    
    // Hoshiarpur (Rural)
    const val HOSHIARPUR_DISTRICT_ID = "20000004-0000-0000-0000-000000000000"
    const val HOSHIARPUR_DISTRICT_NAME = "Hoshiarpur"
    const val HOSHIARPUR_BLOCK_ID = "20000004-2000-0000-0000-000000000000"
    const val HOSHIARPUR_BLOCK_NAME = "Dasuya Block"
    const val HOSHIARPUR_PANCHAYAT_ID = "20000004-2100-0000-0000-000000000000"
    const val HOSHIARPUR_PANCHAYAT_NAME = "Dasuya Panchayat"
    
    // Sangrur (Rural)
    const val SANGRUR_DISTRICT_ID = "20000005-0000-0000-0000-000000000000"
    const val SANGRUR_DISTRICT_NAME = "Sangrur"
    const val SANGRUR_BLOCK_ID = "20000005-2000-0000-0000-000000000000"
    const val SANGRUR_BLOCK_NAME = "Sangrur Block"
    const val SANGRUR_PANCHAYAT_ID = "20000005-2100-0000-0000-000000000000"
    const val SANGRUR_PANCHAYAT_NAME = "Sangrur Panchayat"

    // Kapurthala (Urban)
    const val KAPURTHALA_DISTRICT_ID = "20000006-0000-0000-0000-000000000000"
    const val KAPURTHALA_DISTRICT_NAME = "Kapurthala"
    const val KAPURTHALA_CITY_ID = "20000006-1000-0000-0000-000000000000"
    const val KAPURTHALA_CITY_NAME = "Kapurthala City"
    const val KAPURTHALA_WARD_ID = "20000006-1110-0000-0000-000000000000"
    const val KAPURTHALA_WARD_NAME = "Kapurthala Ward"
    
    // ==================== DATA STRUCTURES ====================
    
    data class District(
        val id: String,
        val name: String,
        val category: String, // "URBAN" or "RURAL"
        val lat: Double,
        val lng: Double
    )
    
    data class UrbanLocation(
        val cityId: String,
        val cityName: String,
        val wardId: String,
        val wardName: String
    )
    
    data class RuralLocation(
        val blockId: String,
        val blockName: String,
        val panchayatId: String,
        val panchayatName: String
    )
    
    // ==================== DISTRICT LIST ====================
    
    val DISTRICTS = listOf(
        District(AMRITSAR_DISTRICT_ID, AMRITSAR_DISTRICT_NAME, "URBAN", 31.6340, 74.8723),
        District(LUDHIANA_DISTRICT_ID, LUDHIANA_DISTRICT_NAME, "URBAN", 30.9010, 75.8573),
        District(JALANDHAR_DISTRICT_ID, JALANDHAR_DISTRICT_NAME, "URBAN", 31.3260, 75.5762),
        District(HOSHIARPUR_DISTRICT_ID, HOSHIARPUR_DISTRICT_NAME, "RURAL", 31.5143, 75.9115),
        District(SANGRUR_DISTRICT_ID, SANGRUR_DISTRICT_NAME, "RURAL", 30.2290, 75.8412),
        District(KAPURTHALA_DISTRICT_ID, KAPURTHALA_DISTRICT_NAME, "URBAN", 31.3715, 75.3937)
    )
    
    // ==================== URBAN MAPPINGS ====================
    
    private val URBAN_LOCATIONS = mapOf(
        AMRITSAR_DISTRICT_ID to UrbanLocation(
            cityId = AMRITSAR_CITY_ID,
            cityName = AMRITSAR_CITY_NAME,
            wardId = AMRITSAR_WARD_ID,
            wardName = AMRITSAR_WARD_NAME
        ),
        LUDHIANA_DISTRICT_ID to UrbanLocation(
            cityId = LUDHIANA_CITY_ID,
            cityName = LUDHIANA_CITY_NAME,
            wardId = LUDHIANA_WARD_ID,
            wardName = LUDHIANA_WARD_NAME
        ),
        JALANDHAR_DISTRICT_ID to UrbanLocation(
            cityId = JALANDHAR_CITY_ID,
            cityName = JALANDHAR_CITY_NAME,
            wardId = JALANDHAR_WARD_ID,
            wardName = JALANDHAR_WARD_NAME
        ),
        KAPURTHALA_DISTRICT_ID to UrbanLocation(
            cityId = KAPURTHALA_CITY_ID,
            cityName = KAPURTHALA_CITY_NAME,
            wardId = KAPURTHALA_WARD_ID,
            wardName = KAPURTHALA_WARD_NAME
        )
    )
    
    // ==================== RURAL MAPPINGS ====================
    
    private val RURAL_LOCATIONS = mapOf(
        HOSHIARPUR_DISTRICT_ID to RuralLocation(
            blockId = HOSHIARPUR_BLOCK_ID,
            blockName = HOSHIARPUR_BLOCK_NAME,
            panchayatId = HOSHIARPUR_PANCHAYAT_ID,
            panchayatName = HOSHIARPUR_PANCHAYAT_NAME
        ),
        SANGRUR_DISTRICT_ID to RuralLocation(
            blockId = SANGRUR_BLOCK_ID,
            blockName = SANGRUR_BLOCK_NAME,
            panchayatId = SANGRUR_PANCHAYAT_ID,
            panchayatName = SANGRUR_PANCHAYAT_NAME
        )
    )
    
    // ==================== UTILITY FUNCTIONS ====================
    
    /**
     * Get the urban location details for a given district ID
     */
    fun getUrbanLocation(districtId: String): UrbanLocation? {
        return URBAN_LOCATIONS[districtId]
    }
    
    /**
     * Get the rural location details for a given district ID
     */
    fun getRuralLocation(districtId: String): RuralLocation? {
        return RURAL_LOCATIONS[districtId]
    }
    
    /**
     * Check if a district is urban
     */
    fun isUrban(districtId: String): Boolean {
        return URBAN_LOCATIONS.containsKey(districtId)
    }
    
    /**
     * Check if a district is rural
     */
    fun isRural(districtId: String): Boolean {
        return RURAL_LOCATIONS.containsKey(districtId)
    }
    
    /**
     * Get the category (URBAN or RURAL) for a district
     */
    fun getCategory(districtId: String): String {
        return if (isUrban(districtId)) "URBAN" else "RURAL"
    }
    
    /**
     * Get the final jurisdiction ID to be used for registration
     * For urban: returns ward ID
     * For rural: returns panchayat ID
     */
    fun getFinalJurisdictionId(districtId: String): String? {
        return if (isUrban(districtId)) {
            getUrbanLocation(districtId)?.wardId
        } else {
            getRuralLocation(districtId)?.panchayatId
        }
    }

    fun findDistrictByName(name: String?): District? {
        val normalizedName = normalizeDistrictName(name)
        if (normalizedName.isBlank()) return null

        return DISTRICTS.firstOrNull { district ->
            normalizeDistrictName(district.name) == normalizedName
        }
    }

    fun findDistrictForAddress(locality: String?, district: String?): District? {
        return findDistrictByName(district) ?: findDistrictByName(locality)
    }

    fun normalizeDistrictName(name: String?): String {
        return name
            ?.lowercase()
            ?.replace(Regex("[^a-z0-9\\s]"), " ")
            ?.replace(Regex("\\bdistrict\\b"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()
    }
}
