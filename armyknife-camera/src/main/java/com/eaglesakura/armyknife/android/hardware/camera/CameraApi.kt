package com.eaglesakura.armyknife.android.hardware.camera

import android.os.Build
import java.util.Arrays

enum class CameraApi {
    /**
     * Android 4.4以下の古いAPI
     */
    Legacy,

    /**
     * Camera2 API
     */
    Camera2,

    /**
     * 自動で取得する
     */
    Default;

    companion object {
        fun listSupoorted(): List<CameraApi> {
            return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                Arrays.asList(Legacy, Camera2)
            } else {
                Arrays.asList(Legacy)
            }
        }
    }
}
