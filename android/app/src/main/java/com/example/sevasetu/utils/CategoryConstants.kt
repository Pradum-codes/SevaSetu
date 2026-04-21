package com.example.sevasetu.utils

/**
 * Hardcoded Category IDs for SevaSetu Issue Reporting
 * These IDs correspond to the seeded categories in the backend database
 */
object CategoryConstants {
    
    // ==================== CATEGORY IDs ====================
    const val ROAD_ID = 1
    const val ROAD_NAME = "Road"
    
    const val GARBAGE_ID = 2
    const val GARBAGE_NAME = "Garbage"
    
    const val WATER_ID = 3
    const val WATER_NAME = "Water"
    
    const val ELECTRICITY_ID = 4
    const val ELECTRICITY_NAME = "Electricity"
    
    const val STREETLIGHT_ID = 5
    const val STREETLIGHT_NAME = "Streetlight"

    const val ROADS_INFRASTRUCTURE_ID = 6
    const val ROADS_INFRASTRUCTURE_NAME = "Roads & Infrastructure"
    
    const val STREET_LIGHTS_ELECTRICITY_ID = 7
    const val STREET_LIGHTS_ELECTRICITY_NAME = "Street Lights & Electricity"
    
    const val GARBAGE_CLEANLINESS_ID = 8
    const val GARBAGE_CLEANLINESS_NAME = "Garbage & Cleanliness"
    
    const val WATER_SUPPLY_DRAINAGE_ID = 9
    const val WATER_SUPPLY_DRAINAGE_NAME = "Water Supply & Drainage"
    
    const val TRAFFIC_SIGNALS_ID = 10
    const val TRAFFIC_SIGNALS_NAME = "Traffic & Signals"
    
    const val PUBLIC_SPACES_ENVIRONMENT_ID = 11
    const val PUBLIC_SPACES_ENVIRONMENT_NAME = "Public Spaces & Environment"
    
    const val PUBLIC_SAFETY_HEALTH_ID = 12
    const val PUBLIC_SAFETY_HEALTH_NAME = "Public Safety & Health"
    
    const val PUBLIC_TOILETS_SANITATION_ID = 13
    const val PUBLIC_TOILETS_SANITATION_NAME = "Public Toilets & Sanitation"
    
    const val PUBLIC_TRANSPORT_ISSUES_ID = 14
    const val PUBLIC_TRANSPORT_ISSUES_NAME = "Public Transport Issues"
    
    const val CONSTRUCTION_ENCROACHMENT_ID = 15
    const val CONSTRUCTION_ENCROACHMENT_NAME = "Construction & Encroachment"
    
    const val OTHERS_ID = 16
    const val OTHERS_NAME = "Others"
    
    // ==================== DATA STRUCTURES ====================
    
    data class Category(
        val id: Int,
        val name: String
    )
    
    // ==================== CATEGORY LIST ====================
    
    val CATEGORIES = listOf(
        Category(ROAD_ID, ROAD_NAME),
        Category(GARBAGE_ID, GARBAGE_NAME),
        Category(WATER_ID, WATER_NAME),
        Category(ELECTRICITY_ID, ELECTRICITY_NAME),
        Category(STREETLIGHT_ID, STREETLIGHT_NAME),
        Category(ROADS_INFRASTRUCTURE_ID, ROADS_INFRASTRUCTURE_NAME),
        Category(STREET_LIGHTS_ELECTRICITY_ID, STREET_LIGHTS_ELECTRICITY_NAME),
        Category(GARBAGE_CLEANLINESS_ID, GARBAGE_CLEANLINESS_NAME),
        Category(WATER_SUPPLY_DRAINAGE_ID, WATER_SUPPLY_DRAINAGE_NAME),
        Category(TRAFFIC_SIGNALS_ID, TRAFFIC_SIGNALS_NAME),
        Category(PUBLIC_SPACES_ENVIRONMENT_ID, PUBLIC_SPACES_ENVIRONMENT_NAME),
        Category(PUBLIC_SAFETY_HEALTH_ID, PUBLIC_SAFETY_HEALTH_NAME),
        Category(PUBLIC_TOILETS_SANITATION_ID, PUBLIC_TOILETS_SANITATION_NAME),
        Category(PUBLIC_TRANSPORT_ISSUES_ID, PUBLIC_TRANSPORT_ISSUES_NAME),
        Category(CONSTRUCTION_ENCROACHMENT_ID, CONSTRUCTION_ENCROACHMENT_NAME),
        Category(OTHERS_ID, OTHERS_NAME)
    )
    
    // ==================== UTILITY FUNCTIONS ====================
    
    /**
     * Get category name by ID
     */
    fun getCategoryName(categoryId: Int): String? {
        return CATEGORIES.find { it.id == categoryId }?.name
    }
    
    /**
     * Get category ID by name
     */
    fun getCategoryId(categoryName: String): Int? {
        return CATEGORIES.find { it.name == categoryName }?.id
    }
    
    /**
     * Get category by ID
     */
    fun getCategory(categoryId: Int): Category? {
        return CATEGORIES.find { it.id == categoryId }
    }
}
