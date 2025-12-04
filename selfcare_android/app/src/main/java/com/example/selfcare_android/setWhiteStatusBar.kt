package com.example.selfcare_android

import android.app.Activity
import android.util.TypedValue
import androidx.core.view.WindowCompat

fun Activity.setCustomStatusBar() {
    val window = this.window

    // attr/colorSurface の実際の色を取得
    val typedValue = TypedValue()
    theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
    val surfaceColor = typedValue.data

    // ステータスバーに色を適用
    window.statusBarColor = surfaceColor

    // 明るい色なら黒アイコンを使うようにする
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    val isLight = isColorLight(surfaceColor)
    controller.isAppearanceLightStatusBars = isLight
}

// 明るさ判定（明るい色 → 黒アイコン）
private fun isColorLight(color: Int): Boolean {
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    val brightness = (r * 299 + g * 587 + b * 114) / 1000
    return brightness >= 180
}


