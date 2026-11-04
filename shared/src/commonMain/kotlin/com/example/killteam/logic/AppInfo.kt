package com.example.killteam.logic

/** v<feature>.<extension>.<bug>.<stage>, stage b = beta, a = alpha. */
object AppInfo {
    const val VERSION = "1.4.0.a"
}

object FeatureFlags {
    /** Camera measuring is paused but kept in the code; the CAMERA permission in the Android manifest is commented out as well. */
    const val MEASURE = false
}
