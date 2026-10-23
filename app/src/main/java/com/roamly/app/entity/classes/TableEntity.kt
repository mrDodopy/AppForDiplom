package com.roamly.app.entity.classes

data class TableEntity(
    val id: Long,
    val establishmentId: Long,
    val name: String,
    val description: String,
    val maxCapacity: Int
)