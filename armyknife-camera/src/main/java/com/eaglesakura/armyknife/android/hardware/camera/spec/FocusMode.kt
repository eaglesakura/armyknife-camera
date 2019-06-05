package com.eaglesakura.armyknife.android.hardware.camera.spec

import android.content.Context
import com.eaglesakura.armyknife.android.ApplicationResources

/**
 * フォーカス状態の設定を行う
 */
data class FocusMode internal constructor(
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
                String.format("Camera.FocusMode.%s", rawName.replace("-".toRegex(), "_"))
        )
        return result ?: rawName
    }

    companion object {

        private val gFocusModeSpecMap: MutableMap<String, FocusMode>

        /**
         * 自動設定
         */
        @JvmField
        val SETTING_AUTO: FocusMode
        /**
         * 無限遠
         */
        @JvmField
        val SETTING_INFINITY: FocusMode
        /**
         * マクロ
         */
        @JvmField
        val SETTING_MACRO: FocusMode
        /**
         * 写真自動
         */
        @JvmField
        val SETTING_CONTINUOUS_PICTURE: FocusMode
        /**
         * ビデオ自動
         */
        @JvmField
        val SETTING_CONTINUOUS_VIDEO: FocusMode

        init {
            gFocusModeSpecMap = HashMap()

            SETTING_AUTO = fromName("auto")
            SETTING_INFINITY = fromName("infinity")
            SETTING_MACRO = fromName("macro")
            SETTING_CONTINUOUS_PICTURE = fromName("continuous-picture")
            SETTING_CONTINUOUS_VIDEO = fromName("continuous-video")
        }

        /**
         * フォーカス設定モードを取得する
         */
        fun fromName(mode: String): FocusMode {
            var result: FocusMode? = gFocusModeSpecMap[mode]
            if (result == null) {
                result = FocusMode(mode)
                gFocusModeSpecMap[mode] = result
            }
            return result
        }
    }
}
