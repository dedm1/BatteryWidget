package com.dedm.batterywidget.timezone

import java.time.Instant
import java.time.ZoneId

data class TimeZoneOption(
    val offsetHours: Double,
    val zoneId: String,
    val cityName: String,
    val searchQuery: String
) {
    companion object {
        private data class ZoneTemplate(
            val zoneId: String,
            val cityName: String,
            val searchQuery: String? = null
        )

        private val templates = listOf(
            ZoneTemplate("Pacific/Niue", "Ниуэ"),
            ZoneTemplate("Pacific/Rarotonga", "Американское Самоа"),
            ZoneTemplate("Pacific/Gambier", "Французская Полинезия"),
            ZoneTemplate("Pacific/Pitcairn", "Питкэрн"),
            ZoneTemplate("America/Guatemala", "Гватемала"),
            ZoneTemplate("America/Bogota", "Колумбия"),
            ZoneTemplate("America/La_Paz", "Боливия"),
            ZoneTemplate("America/Buenos_Aires", "Аргентина"),
            ZoneTemplate("Atlantic/South_Georgia", "Южная Георгия"),
            ZoneTemplate("Atlantic/Cape_Verde", "Кабо-Верде"),
            ZoneTemplate("Africa/Abidjan", "Кот-д'Ивуар"),
            ZoneTemplate("Africa/Algiers", "Алжир"),
            ZoneTemplate("Africa/Johannesburg", "ЮАР"),
            ZoneTemplate("Africa/Nairobi", "Кения"),
            ZoneTemplate("Asia/Dubai", "ОАЭ"),
            ZoneTemplate("Asia/Tashkent", "Узбекистан"),
            ZoneTemplate("Asia/Tehran", "Иран"),
            ZoneTemplate("Asia/Kabul", "Афганистан"),
            ZoneTemplate("Asia/Kolkata", "Индия"),
            ZoneTemplate("Asia/Kathmandu", "Непал"),
            ZoneTemplate("Asia/Yangon", "Мьянма"),
            ZoneTemplate("Asia/Dhaka", "Бангладеш"),
            ZoneTemplate("Asia/Bangkok", "Таиланд"),
            ZoneTemplate("Asia/Singapore", "Сингапур"),
            ZoneTemplate("Asia/Tokyo", "Япония"),
            ZoneTemplate("Australia/Adelaide", "Аделаида", "Австралия"),
            ZoneTemplate("Australia/Darwin", "Дарвин", "Австралия"),
            ZoneTemplate("Australia/Lord_Howe", "Лорд-Хау", "Австралия"),
            ZoneTemplate("Pacific/Port_Moresby", "Новая Гвинея"),
            ZoneTemplate("Pacific/Guadalcanal", "Соломоновы"),
            ZoneTemplate("Pacific/Fiji", "Фиджи"),
            ZoneTemplate("Pacific/Chatham", "Чатем", "Новая Зеландия"),
            ZoneTemplate("Pacific/Apia", "Самоа"),
            ZoneTemplate("Pacific/Kiritimati", "Кирибати")
        )

        fun defaultList(instant: Instant = Instant.now()): List<TimeZoneOption> {
            return templates.mapNotNull { template ->
                val zone = runCatching { ZoneId.of(template.zoneId) }.getOrNull() ?: return@mapNotNull null
                val offsetSeconds = zone.rules.getOffset(instant).totalSeconds.toLong()
                val offsetHours = offsetSeconds / 3600.0
                TimeZoneOption(
                    offsetHours = offsetHours,
                    zoneId = template.zoneId,
                    cityName = template.cityName,
                    searchQuery = template.searchQuery ?: template.cityName
                )
            }.sortedBy { it.offsetHours }
        }
    }
}

