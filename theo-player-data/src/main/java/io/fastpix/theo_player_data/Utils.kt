package io.fastpix.theo_player_data

import kotlin.math.floor

object Utils {
    fun secondToMs(seconds: Double): Int = floor(seconds * 1000).toInt()
}