package com.example.tournote

data class RoutePointDataClass(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isStartPoint: Boolean = false,
    val isEndPoint: Boolean = false
)

