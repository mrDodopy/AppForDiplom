package com.roamly.app.entity.DTO.establishment

import com.roamly.app.entity.classes.TypeOfEstablishment

data class EstablishmentUpdateRequest(
    val name: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val type: TypeOfEstablishment,
    val photoBase64s: List<String>, // Список Base64 строк
    val operatingHoursString: String? // Строка расписания
)