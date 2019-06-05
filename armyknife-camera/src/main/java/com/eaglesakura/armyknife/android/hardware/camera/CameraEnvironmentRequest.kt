package com.eaglesakura.armyknife.android.hardware.camera

import com.eaglesakura.armyknife.android.hardware.camera.spec.FlashMode
import com.eaglesakura.armyknife.android.hardware.camera.spec.FocusMode
import com.eaglesakura.armyknife.android.hardware.camera.spec.Scene
import com.eaglesakura.armyknife.android.hardware.camera.spec.WhiteBalance

data class CameraEnvironmentRequest(
    val focusMode: FocusMode? = null,

    val scene: Scene? = null,

    val whiteBalance: WhiteBalance? = null,

    val flashMode: FlashMode? = null
)