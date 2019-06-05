package com.eaglesakura.armyknife.android.hardware.camera

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.location.Location
import android.media.ImageReader
import android.os.Build
import android.view.Surface
import com.eaglesakura.armyknife.android.ApplicationRuntime
import com.eaglesakura.armyknife.android.extensions.AsyncHandler
import com.eaglesakura.armyknife.android.extensions.UIHandler
import com.eaglesakura.armyknife.android.hardware.camera.error.CameraAccessFailedException
import com.eaglesakura.armyknife.android.hardware.camera.error.CameraException
import com.eaglesakura.armyknife.android.hardware.camera.error.CameraSecurityException
import com.eaglesakura.armyknife.android.hardware.camera.error.PictureFailedException
import com.eaglesakura.armyknife.android.hardware.camera.preview.CameraSurface
import com.eaglesakura.armyknife.android.hardware.camera.spec.CameraType
import com.eaglesakura.armyknife.android.hardware.camera.spec.CaptureFormat
import com.eaglesakura.armyknife.android.hardware.camera.spec.FocusMode
import com.eaglesakura.armyknife.runtime.extensions.cancelByError
import com.eaglesakura.armyknife.runtime.extensions.receiveOrError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

@SuppressLint("MissingPermission")
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
internal class Camera2ControlManager(
    private val context: Context,
    private val connectRequest: CameraConnectRequest
) : CameraControlManager() {

    private var camera: CameraDevice? = null

    private val spec: Camera2SpecImpl = Camera2SpecImpl(context)

    private val characteristics: CameraCharacteristics =
        spec.getCameraSpec(connectRequest.cameraType)

    /**
     * 撮影用セッション
     *
     * MEMO: 基本的に使いまわさなければ撮影とプレビューを両立できない
     */
    private var captureSession: CameraCaptureSession? = null

    /**
     * プレビュー用バッファ
     */
    private var previewSurface: CameraSurface? = null

    /**
     * 撮影用バッファ
     */
    private var imageReader: ImageReader? = null

    /**
     * 接続時に確定されたプレビューリクエスト
     */
    private var previewRequest: CameraPreviewRequest? = null

    /**
     * 接続時に確定された撮影リクエスト
     */
    private var pictureShotRequest: CameraPictureShotRequest? = null

    private var mFlags: Int = 0

    private var processingHandler: AsyncHandler? = null

    override val supportApi: CameraApi
        get() = CameraApi.Camera2

    override val previewNow: Boolean
        get() = mFlags.and(FLAG_NOW_PREVIEW) == FLAG_NOW_PREVIEW

    override val connected: Boolean
        get() = camera != null

    override suspend fun connect(
        previewSurface: CameraSurface?,
        previewRequest: CameraPreviewRequest?,
        shotRequest: CameraPictureShotRequest?
    ) {
        processingHandler = AsyncHandler.newInstance("camera-processing-${hashCode()}")

        withContext(coroutineContext + Dispatchers.Main) {
            val channel = Channel<CameraDevice>()

            try {
                this@Camera2ControlManager.pictureShotRequest = shotRequest
                this@Camera2ControlManager.previewRequest = previewRequest
                this@Camera2ControlManager.previewSurface = previewSurface

                spec.cameraManager.openCamera(
                    spec.cameraId!!,
                    object : CameraDevice.StateCallback() {
                        override fun onOpened(cameraDevice: CameraDevice) {
                            launch(Dispatchers.Main) {
                                channel.send(cameraDevice)
                            }
                        }

                        override fun onDisconnected(cameraDevice: CameraDevice) {
                            launch(Dispatchers.Main) {
                                disconnect()
                            }
                        }

                        override fun onError(cameraDevice: CameraDevice, error: Int) {
                            launch(Dispatchers.Main) {
                                if (error == ERROR_CAMERA_IN_USE) {
                                    // すでに使われている
                                    channel.send(cameraDevice)
                                } else {
                                    channel.cancelByError(CameraSecurityException("Error[$error]"))
                                }
                            }
                        }
                    },
                    UIHandler
                )

                camera = channel.receiveOrError()
            } catch (err: SecurityException) {
                // haven't camera-permission
                throw CameraAccessFailedException(err)
            } catch (err: CameraAccessException) {
                throw CameraAccessFailedException(err)
            }
        }
    }

    override suspend fun disconnect() {
        withContext(NonCancellable) {
            stopPreview()

            imageReader?.close()
            captureSession?.close()
            camera?.close()

            imageReader = null
            captureSession = null
            camera = null

            pictureShotRequest = null
            previewRequest = null
            previewSurface = null

            processingHandler?.dispose()
            processingHandler = null
        }
    }

    @Synchronized
    @Throws(CameraException::class)
    private suspend fun getSession(): CameraCaptureSession {
        if (camera == null) {
            throw CameraAccessFailedException("connect() not called")
        }

        if (captureSession != null) {
            return captureSession!!
        }

        val surfaces = ArrayList<Surface>()

        previewRequest?.also { cameraPreviewRequest ->
            surfaces.add(previewSurface!!.getSurface(cameraPreviewRequest.previewSize))
        }

        pictureShotRequest?.also { cameraPictureShotRequest ->
            imageReader = ImageReader.newInstance(
                cameraPictureShotRequest.captureSize.width,
                cameraPictureShotRequest.captureSize.height,
                if (cameraPictureShotRequest.format === CaptureFormat.Raw) ImageFormat.RAW_SENSOR else ImageFormat.JPEG,
                2
            )
            surfaces.add(imageReader!!.surface)
        }

        val channel = Channel<CameraCaptureSession>()
        try {
            camera!!.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    GlobalScope.launch(Dispatchers.Main) {
                        channel.send(session)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    channel.close(CameraException("Session create failed"))
                }
            }, UIHandler)
            return channel.receive()
        } catch (e: CameraAccessException) {
            throw CameraAccessFailedException(e)
        }
    }

    @Throws(CameraAccessException::class)
    private fun newCaptureRequest(
        env: CameraEnvironmentRequest?,
        template: Int
    ): CaptureRequest.Builder {
        val camera = camera
            ?: throw throw CameraAccessException(
                CameraAccessException.CAMERA_ERROR,
                "connect() not called"
            )
        val request = camera.createCaptureRequest(template)

        env?.apply {
            flashMode?.also { flashMode ->
                request.set(CaptureRequest.FLASH_MODE, Camera2SpecImpl.toFlashModeInt(flashMode))
            }

            focusMode?.also { focusMode ->
                // AFモード設定
                request.set(CaptureRequest.CONTROL_AF_MODE, Camera2SpecImpl.toAfModeInt(focusMode))

                if (focusMode == FocusMode.SETTING_INFINITY) {
                    request.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.0f)
                }
            }

            scene?.also { scene ->
                request.set(CaptureRequest.CONTROL_SCENE_MODE, Camera2SpecImpl.toSceneInt(scene))
            }

            whiteBalance?.also { whiteBalance ->
                request.set(CaptureRequest.CONTROL_AWB_MODE, Camera2SpecImpl.toAwbInt(whiteBalance))
            }
        }

        return request
    }

    @Throws(CameraException::class)
    override suspend fun startPreview(env: CameraEnvironmentRequest?) {
        try {
            // セッションを生成する
            val previewSession = getSession()

            // プレビューを開始する
            val previewCaptureRequest =
                newCaptureRequest(env, CameraDevice.TEMPLATE_PREVIEW).also { builder ->
                    builder.set(
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
                    )
                    builder.addTarget(previewSurface!!.getSurface(previewRequest!!.previewSize))
                }
            previewSession.stopRepeating()
            previewSession.setRepeatingRequest(
                previewCaptureRequest.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    private var mOldAfState: Int? = null

                    private var mOldAeState: Int? = null

                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        val afState = result.get(CaptureResult.CONTROL_AF_STATE)

                        if (aeState != null && aeState != mOldAeState) {
                            mOldAeState = aeState
                        }
                        if (afState != null && afState != mOldAfState) {
                            mOldAfState = afState
                        }
                    }
                },
                UIHandler
            )

            mFlags = mFlags or FLAG_NOW_PREVIEW
        } catch (e: CameraAccessException) {
            throw CameraAccessFailedException(e)
        }
    }

    override suspend fun stopPreview() {
        try {
            captureSession?.also { cameraCaptureSession ->
                cameraCaptureSession.stopRepeating()
            }
        } catch (e: Exception) {
            // drop error.
        } finally {
            mFlags = mFlags and FLAG_NOW_PREVIEW.inv()
        }
    }

    private fun getJpegOrientation(): Int {
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        val deviceRotateDegree = ApplicationRuntime.getDeviceRotateDegree(context)
        val jpegOrientation: Int

        // (360 * 2)を加算しているのは、最大で-270-270の角度を正の値に補正するためである
        jpegOrientation =
            if (connectRequest.cameraType == CameraType.Back) {
                (180 - (sensorOrientation + deviceRotateDegree) + 360 * 2) % 360
            } else {
                (sensorOrientation + deviceRotateDegree + 360 * 2) % 360
            }
        return jpegOrientation
    }

    @Throws(CameraException::class)
    override suspend fun takePicture(env: CameraEnvironmentRequest?): PictureData {
        if (previewRequest != null && !previewNow) {
            throw IllegalStateException("Preview not started")
        }

        val session = getSession()
        val imageReader = this.imageReader!!
        val pictureShotRequest = this.pictureShotRequest!!
        try {
            val captureCompleted = Channel<Boolean>()
            val picture = Channel<PictureData>()

            // 撮影コールバック
            val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    GlobalScope.launch(Dispatchers.Main) {
                        captureCompleted.send(true)
                    }
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    GlobalScope.launch(Dispatchers.Main) {
                        captureCompleted.cancelByError(PictureFailedException("Fail reason[${failure.reason}]"))
                    }
                }
            }

            // 画像圧縮完了コールバック
            imageReader.setOnImageAvailableListener({
                val image = imageReader.acquireLatestImage()
                val buffer = image.planes[0].buffer
                val onMemoryFile = ByteArray(buffer.capacity())
                buffer.get(onMemoryFile)
                GlobalScope.launch(Dispatchers.Main) {
                    picture.send(PictureData(image.width, image.height, onMemoryFile))
                    image.close()
                }
            }, processingHandler)

            val builder = newCaptureRequest(env, CameraDevice.TEMPLATE_STILL_CAPTURE)
            builder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation())

            // Lat/Lng
            if (pictureShotRequest.hasLocation) {
                val loc = Location("camera")
                loc.latitude = pictureShotRequest.latitude!!
                loc.longitude = pictureShotRequest.longitude!!
                builder.set(CaptureRequest.JPEG_GPS_LOCATION, loc)
            }

            if (FocusMode.SETTING_AUTO == env?.focusMode) {
                builder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }

            builder.addTarget(imageReader.surface)
            session.stopRepeating()
            session.capture(builder.build(), captureCallback, UIHandler)

            // receive all messages.
            captureCompleted.receiveOrError()
            return picture.receiveOrError()
        } catch (e: CameraAccessException) {
            throw CameraAccessFailedException(e)
        } finally {
            imageReader.setOnImageAvailableListener(null, null)
            if (mFlags and FLAG_NOW_PREVIEW != 0) {
                startPreview(env)
            }
        }
    }

    companion object {

        /**
         * プレビュー中である場合true
         */
        private const val FLAG_NOW_PREVIEW = 0x01 shl 0

        private val TAG = Camera2ControlManager::class.java.simpleName
    }
}