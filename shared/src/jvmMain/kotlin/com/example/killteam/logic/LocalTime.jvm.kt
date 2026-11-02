package com.example.killteam.logic

import java.util.TimeZone

actual fun localOffsetMillis(epochMillis: Long): Long = TimeZone.getDefault().getOffset(epochMillis).toLong()
