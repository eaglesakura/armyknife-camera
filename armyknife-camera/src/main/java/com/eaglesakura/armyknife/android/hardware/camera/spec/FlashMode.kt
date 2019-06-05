package com.eaglesakura.armyknife.android.hardware.camera.spec

import android.content.Context
import com.eaglesakura.armyknife.android.ApplicationResources

/**
 * ホワイトバランス設定
 */
data class FlashMode internal constructor(
    /**
     * API設定名
     */
    val rawName: String
) {

    val permitFlash: Boolean = (rawName.toLowerCase() != "off")

    /**
     * 設定名を取得する
     *
     * @return 日本語での設定名
     */
    fun name(context: Context): String {
        val result = ApplicationResources.getStringFromIdName(
            context,
            String.format("Camera.FlashMode.%s", rawName.replace("-".toRegex(), "_"))
        )
        return result ?: rawName
    }

    companion object {
        private val gFlashSpecMap: MutableMap<String, FlashMode>

        /**
         * 自動設定
         */
        @JvmField
        val SETTING_AUTO: FlashMode

        /**
         * オフ
         */
        @JvmField
        val SETTING_OFF: FlashMode

        /**
         * オン
         */
        @JvmField
        val SETTING_ON: FlashMode

        /**
         * 赤目補正
         */
        @JvmField
        val SETTING_RED_EYE: FlashMode

        /**
         * 常時
         */
        @JvmField
        val SETTING_TORCH: FlashMode

        init {
            gFlashSpecMap = HashMap()

            SETTING_AUTO = fromName("auto")
            SETTING_OFF = fromName("off")
            SETTING_ON = fromName("on")
            SETTING_RED_EYE = fromName("red-eye")
            SETTING_TORCH = fromName("torch")
        }

        /**
         * フラッシュ設定モードを取得する
         */
        fun fromName(mode: String): FlashMode {
            var result: FlashMode? = gFlashSpecMap[mode]
            if (result == null) {
                result = FlashMode(mode)
                gFlashSpecMap[mode] = result
            }
            return result
        }
    }
}
