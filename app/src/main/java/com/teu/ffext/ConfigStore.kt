package com.teu.ffext

import android.content.Context
import com.teu.ffext.core.Config

object ConfigStore {
    private const val PREFS = "delta_cfg"

    fun save(context: Context, cfg: Config) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        prefs.putBoolean("spin", cfg.spinEnabled)
        prefs.putBoolean("esp", cfg.espEnabled)
        prefs.apply()
    }

    fun load(context: Context): Config {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Config(
            spinEnabled = prefs.getBoolean("spin", false),
            espEnabled = prefs.getBoolean("esp", true)
        )
    }
}
