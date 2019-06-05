package com.eaglesakura.armyknife.android.hardware.camera.error

open class CameraException : Exception {
    constructor()

    constructor(detailMessage: String) : super(detailMessage)

    constructor(detailMessage: String, throwable: Throwable) : super(detailMessage, throwable)

    constructor(throwable: Throwable) : super(throwable)
}
