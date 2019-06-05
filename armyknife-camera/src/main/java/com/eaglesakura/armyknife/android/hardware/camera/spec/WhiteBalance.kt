package com.eaglesakura.armyknife.android.hardware.camera.spec

import android.content.Context
import com.eaglesakura.armyknife.android.ApplicationResources

/**
 * ホワイトバランス設定
 */
data class WhiteBalance internal constructor(
    /**
     * API設定名
     */
    val rawName: String
) {

    /**
     * 設定名を取得する
     *
     * @return 日本語での設定名
     */
    fun name(context: Context): String {
        val result = ApplicationResources.getStringFromIdName(
                context,
                String.format("Camera.WhiteBalance.%s", rawName.replace("-".toRegex(), "_"))
        )
        return result ?: rawName
    }

    companion object {
        private val gWhiteBalanceSpecMap: MutableMap<String, WhiteBalance>

        /**
         * 自動設定
         */
        @JvmField
        val SETTING_AUTO: WhiteBalance
        /**
         * 白熱灯
         */
        @JvmField
        val SETTING_INCANDESCENT: WhiteBalance
        /**
         * 蛍光灯
         */
        @JvmField
        val SETTING_FLUORESCENT: WhiteBalance
        /**
         * 晴天
         */
        @JvmField
        val SETTING_DAYLIGHT: WhiteBalance
        /**
         * 曇り
         */
        @JvmField
        val SETTING_CLOUDY_DAYLIGHT: WhiteBalance

        init {
            gWhiteBalanceSpecMap = HashMap()

            SETTING_AUTO = fromName("auto")
            SETTING_INCANDESCENT = fromName("incandescent")
            SETTING_FLUORESCENT = fromName("fluorescent")
            SETTING_DAYLIGHT = fromName("daylight")
            SETTING_CLOUDY_DAYLIGHT = fromName("cloudy-daylight")
        }

        /**
         * ホワイトバランス設定モードを取得する
         */
        fun fromName(mode: String): WhiteBalance {
            var result: WhiteBalance? = gWhiteBalanceSpecMap[mode]
            if (result == null) {
                result = WhiteBalance(mode)
                gWhiteBalanceSpecMap[mode] = result
            }
            return result
        }

        /**
         * デバイス設定から取得する
         *
         * @return ホワイトバランス設定
         */
        fun list(deviceSettings: List<String>?): List<WhiteBalance> {
            val result = ArrayList<WhiteBalance>()
            if (deviceSettings == null) {
                return result
            }

            for (mode in deviceSettings) {
                result.add(fromName(mode))
            }

            return result
        }
    }
}
