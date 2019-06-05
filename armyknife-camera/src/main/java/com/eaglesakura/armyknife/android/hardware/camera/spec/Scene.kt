package com.eaglesakura.armyknife.android.hardware.camera.spec

import android.content.Context
import com.eaglesakura.armyknife.android.ApplicationResources

/**
 * シーン情報
 */
data class Scene internal constructor(
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
                String.format("Camera.Scene.%s", rawName.replace("-".toRegex(), "_"))
        )
        return result ?: rawName
    }

    companion object {

        private val gSceneSpecMap: MutableMap<String, Scene>

        /**
         * モードなし
         */
        @JvmField
        val SETTING_OFF: Scene

        /**
         * 自動設定
         */
        @JvmField
        val SETTING_AUTO: Scene

        /**
         * 人物撮影
         * ソフトスナップ by XperiaGX
         */
        @JvmField
        val SETTING_PORTRAIT: Scene

        /**
         * 風景
         */
        @JvmField
        val SETTING_LANDSCAPE: Scene

        /**
         * 夜景
         */
        @JvmField
        val SETTING_NIGHT: Scene

        /**
         * 夜景人物
         * 夜景＆人物 by XperiaGX
         */
        @JvmField
        val SETTING_NIGHT_PORTRAIT: Scene

        /**
         * ビーチ
         * ビーチ ＆ スノー by XperiaGX
         */
        @JvmField
        val SETTING_BEACH: Scene

        /**
         * 雪景色
         * ビーチ ＆ スノー by XperiaGX
         */
        @JvmField
        val SETTING_SNOW: Scene

        /**
         * スポーツ
         */
        @JvmField
        val SETTING_SPORTS: Scene

        /**
         * パーティ
         */
        @JvmField
        val SETTING_PARTY: Scene

        /**
         * 二値化/文字認識
         */
        @JvmField
        val SETTING_DOCUMENT: Scene

        @JvmField
        val SETTING_SUNSET: Scene

        @JvmField
        val SETTING_STEADYPHOTO: Scene

        @JvmField
        val SETTING_FIREWORKS: Scene

        @JvmField
        val SETTING_CANDLELIGHT: Scene

        @JvmField
        val SETTING_THEATRE: Scene

        @JvmField
        val SETTING_ACTION: Scene

        init {
            gSceneSpecMap = HashMap()
            SETTING_OFF = fromName("off")
            SETTING_AUTO = fromName("auto")
            SETTING_PORTRAIT = fromName("portrait")
            SETTING_LANDSCAPE = fromName("landscape")
            SETTING_NIGHT = fromName("night")
            SETTING_NIGHT_PORTRAIT = fromName("night-portrait")
            SETTING_BEACH = fromName("beach")
            SETTING_SNOW = fromName("snow")
            SETTING_SPORTS = fromName("sports")
            SETTING_PARTY = fromName("party")
            SETTING_DOCUMENT = fromName("document")
            SETTING_SUNSET = fromName("sunset")
            SETTING_STEADYPHOTO = fromName("steadyphoto")
            SETTING_FIREWORKS = fromName("fireworks")
            SETTING_CANDLELIGHT = fromName("candlelight")
            SETTING_THEATRE = fromName("theatre")
            SETTING_ACTION = fromName("action")
        }

        /**
         * シーンを取得する
         */
        fun fromName(mode: String): Scene {
            var result: Scene? = gSceneSpecMap[mode]
            if (result == null) {
                result = Scene(mode)
                gSceneSpecMap[mode] = result
            }
            return result
        }

        /**
         * デバイス設定から取得する
         *
         * @return シーン設定
         */
        fun list(deviceSettings: List<String>?): List<Scene> {
            val result = ArrayList<Scene>()
            deviceSettings?.forEach { mode ->
                result.add(fromName(mode))
            }
            return result
        }
    }
}
