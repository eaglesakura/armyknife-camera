package com.eaglesakura.armyknife.android.hardware.camera.spec

enum class CameraType {
    Front,
    Back,

    /**
     * This option is only Android 5.0 or later.
     * example) Android Things with Web Camera.
     */
    External,

    /**
     * Choose auto camera from any options.
     * Priority as follows.
     * 1. Back Camera.
     * 2. Front Camera.
     * 3. External Camera.
     */
    Auto
}
