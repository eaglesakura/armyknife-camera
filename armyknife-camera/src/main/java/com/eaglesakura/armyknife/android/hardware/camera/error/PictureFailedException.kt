package com.eaglesakura.armyknife.android.hardware.camera.error

class PictureFailedException : CameraException {
    constructor()

    constructor(detailMessage: String) : super(detailMessage)

    constructor(detailMessage: String, throwable: Throwable) : super(detailMessage, throwable)

    constructor(throwable: Throwable) : super(throwable)
}
