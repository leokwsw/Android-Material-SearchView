package dev.leonardpark.lib.material_search_view

import android.content.Context
import android.content.res.Configuration

class Utils {
  companion object {
    fun isUsingNightModeResources(context: Context): Boolean {
      return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO -> false
        Configuration.UI_MODE_NIGHT_UNDEFINED -> false
        else -> false
      }
    }
  }
}