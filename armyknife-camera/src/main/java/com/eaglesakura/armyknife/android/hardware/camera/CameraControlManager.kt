package com.eaglesakura.armyknife.android.hardware.camera

import android.content.Context
import android.os.Build

import com.eaglesakura.armyknife.android.hardware.camera.error.CameraException
import com.eaglesakura.armyknife.android.hardware.camera.preview.CameraSurface

/**
 * This class utilities for camera on android device.
 *
 * When this class use in Android 5.0 or later,It use the camera2 api.
 * Or else,It use the legacy camera api.
 *
 * Methods in this class are non-callback programming model.
 * Should use "connect()", "startPreview()" and more in "kotlin-coroutines" function.
 *
 * Want to take a picture, Should call methods as follows.
 * - 1. connect
 * - 2(Can skip on Android 5.0 or later). startPreview
 * - 3(optional). takePicture
 * - 4. stopPreview
 * - 5. disconnect
 */
abstract class CameraControlManager {

    abstract val supportApi: CameraApi

    abstract val previewNow: Boolean

    abstract val connected: Boolean

    /**
     * 撮影用の設定を指定して接続する
     *
     * @param previewSurface Preview target surface
     * @param previewRequest Preview settings.
     * @param shotRequest Picture settings.
     */
    @Throws(CameraException::class)
    abstract suspend fun connect(
        previewSurface: CameraSurface?,
        previewRequest: CameraPreviewRequest?,
        shotRequest: CameraPictureShotRequest?
    )

    abstract suspend fun disconnect()

    /**
     * Start preview from initialized a camera to the render surface.
     *
     * If you should be use a camera in the background service,
     * Should use the offscreen-surface.
     */
    @Throws(CameraException::class)
    abstract suspend fun startPreview(env: CameraEnvironmentRequest?)

    /**
     * カメラプレビューを停止する
     *
     *
     * MEMO: プレビューの停止はサーフェイスと同期して削除しなければならないため、実装的にはUIスレッド・バックグラウンドスレッドどちらでも動作できる。
     */
    @Throws(CameraException::class)
    abstract suspend fun stopPreview()

    /**
     * Take a picture with optional environments.
     */
    @Throws(CameraException::class)
    abstract suspend fun takePicture(env: CameraEnvironmentRequest?): PictureData

    companion object {
        @Throws(CameraException::class)
        fun newInstance(context: Context, api: CameraApi, request: CameraConnectRequest): CameraControlManager {
            var api = api
            if (api == CameraApi.Default) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    api = CameraApi.Camera2
                } else {
                    api = CameraApi.Legacy
                }
            }

            if (api == CameraApi.Camera2) {
                return Camera2ControlManager(context, request)
            }
            TODO("should implements the legacy camera api in this version(or less) android.")
        }

        @Throws(CameraException::class)
        fun newInstance(context: Context, request: CameraConnectRequest): CameraControlManager {
            return newInstance(context, CameraApi.Default, request)
        }
    }
}
