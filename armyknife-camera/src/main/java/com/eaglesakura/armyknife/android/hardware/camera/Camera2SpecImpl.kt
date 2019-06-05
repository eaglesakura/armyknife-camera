package com.eaglesakura.armyknife.android.hardware.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import com.eaglesakura.armyknife.android.hardware.camera.error.CameraAccessFailedException
import com.eaglesakura.armyknife.android.hardware.camera.error.CameraException
import com.eaglesakura.armyknife.android.hardware.camera.error.CameraNotFoundException
import com.eaglesakura.armyknife.android.hardware.camera.spec.CameraType
import com.eaglesakura.armyknife.android.hardware.camera.spec.CaptureFormat
import com.eaglesakura.armyknife.android.hardware.camera.spec.CaptureSize
import com.eaglesakura.armyknife.android.hardware.camera.spec.FlashMode
import com.eaglesakura.armyknife.android.hardware.camera.spec.FocusMode
import com.eaglesakura.armyknife.android.hardware.camera.spec.Scene
import com.eaglesakura.armyknife.android.hardware.camera.spec.WhiteBalance
import com.eaglesakura.armyknife.runtime.extensions.findKey

@SuppressLint("NewApi")
internal class Camera2SpecImpl internal constructor(context: Context) {

    private val mContext: Context

    val cameraManager: CameraManager

    var cameraId: String? = null
        private set

    init {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw IllegalStateException()
        }

        mContext = context.applicationContext
        cameraManager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    @Throws(CameraException::class)
    internal fun getCameraSpec(type: CameraType): CameraCharacteristics {
        try {
            var autoCameraId: String? = null
            var autoCharacteristics: CameraCharacteristics? = null
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)!!
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    if (type === CameraType.Front) {
                        cameraId = id
                        return characteristics
                    } else if (autoCameraId == null) {
                        autoCameraId = id
                        autoCharacteristics = characteristics
                    }
                } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    if (type === CameraType.Back) {
                        cameraId = id
                        return characteristics
                    } else {
                        autoCameraId = id
                        autoCharacteristics = characteristics
                    }
                } else if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                    if (type === CameraType.External) {
                        cameraId = id
                        return characteristics
                    } else if (autoCameraId == null) {
                        autoCameraId = id
                        autoCharacteristics = characteristics
                    }
                }
            }

            // 自動選択が有効であればそちらを利用する
            if (autoCameraId != null) {
                cameraId = autoCameraId
                return autoCharacteristics!!
            }

            throw CameraNotFoundException("type :: $type")
        } catch (e: CameraAccessException) {
            throw CameraAccessFailedException(e)
        }
    }

    @Throws(CameraException::class)
    internal fun getFlashModes(characteristics: CameraCharacteristics): List<FlashMode> {
        if (!characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)!!) {
            return emptyList()
        }

        val result = ArrayList<FlashMode>()
        for (mode in characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)!!) {
            sFlashModeMap.findKey { it == mode }?.also { flashMode -> result.add(flashMode) }
        }

        return result
    }

    @Throws(CameraException::class)
    internal fun getFocusModes(characteristics: CameraCharacteristics): List<FocusMode> {
        val result = ArrayList<FocusMode>()

        for (mode in characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)!!) {
            sFocusModeMap.findKey { it == mode }?.also { focusMode -> result.add(focusMode) }
        }

        return result
    }

    @Throws(CameraException::class)
    internal fun getPictureSizes(characteristics: CameraCharacteristics, format: CaptureFormat): List<CaptureSize> {
        val sizes =
                characteristics.get<StreamConfigurationMap>(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.getOutputSizes(
                        toImageFormatInt(format)
                )
                        ?: return listOf()

        val result = ArrayList<CaptureSize>()
        for (size in sizes) {
            result.add(CaptureSize(size.width, size.height))
        }
        return result
    }

    /**
     * サポートしているプレビューサイズ一覧を取得する
     */
    @Throws(CameraException::class)
    internal fun getPreviewSizes(characteristics: CameraCharacteristics): List<CaptureSize> {
        val sizes =
                characteristics.get<StreamConfigurationMap>(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.getOutputSizes(
                        SurfaceTexture::class.java
                )
                        ?: return listOf()

        val result = ArrayList<CaptureSize>()
        for (size in sizes) {
            result.add(CaptureSize(size.width, size.height))
        }
        return result
    }

    /**
     * サポートしている撮影シーン一覧を取得する
     */
    @Throws(CameraException::class)
    internal fun getScenes(characteristics: CameraCharacteristics): List<Scene> {
        val result = ArrayList<Scene>()
        for (mode in characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)!!) {
            sSceneModeMap.findKey { it == mode }?.also { scene -> result.add(scene) }
        }

        return result
    }

    @Throws(CameraException::class)
    internal fun getWhiteBalances(characteristics: CameraCharacteristics): List<WhiteBalance> {
        val result = ArrayList<WhiteBalance>()

        for (mode in characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)!!) {
            sWhiteBalanceMap.findKey { it == mode }?.also { whiteBalance -> result.add(whiteBalance) }
        }

        return result
    }

    companion object {

        private val sCaptureFormatMap = HashMap<CaptureFormat, Int>()

        private val sFlashModeMap = HashMap<FlashMode, Int>()

        private val sFocusModeMap = HashMap<FocusMode, Int>()

        private val sSceneModeMap = HashMap<Scene, Int>()

        private val sWhiteBalanceMap = HashMap<WhiteBalance, Int>()

        init {
            sCaptureFormatMap[CaptureFormat.Jpeg] = ImageFormat.JPEG
            sCaptureFormatMap[CaptureFormat.Raw] = ImageFormat.RAW_SENSOR

            sFlashModeMap[FlashMode.SETTING_OFF] = CameraCharacteristics.CONTROL_AE_MODE_OFF
            sFlashModeMap[FlashMode.SETTING_ON] = CameraCharacteristics.CONTROL_AE_MODE_ON
            sFlashModeMap[FlashMode.SETTING_TORCH] = CameraCharacteristics.CONTROL_AE_MODE_ON_ALWAYS_FLASH
            sFlashModeMap[FlashMode.SETTING_RED_EYE] = CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE
            sFlashModeMap[FlashMode.SETTING_AUTO] = CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH

            sFocusModeMap[FocusMode.SETTING_AUTO] = CameraCharacteristics.CONTROL_AF_MODE_AUTO
            sFocusModeMap[FocusMode.SETTING_CONTINUOUS_PICTURE] =
                    CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            sFocusModeMap[FocusMode.SETTING_CONTINUOUS_VIDEO] = CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            sFocusModeMap[FocusMode.SETTING_MACRO] = CameraCharacteristics.CONTROL_AF_MODE_MACRO
            sFocusModeMap[FocusMode.SETTING_INFINITY] = CameraCharacteristics.CONTROL_AF_MODE_OFF

            sSceneModeMap[Scene.SETTING_OFF] = CameraCharacteristics.CONTROL_MODE_OFF
            sSceneModeMap[Scene.SETTING_AUTO] = CameraCharacteristics.CONTROL_MODE_AUTO
            sSceneModeMap[Scene.SETTING_PORTRAIT] = CameraCharacteristics.CONTROL_SCENE_MODE_PORTRAIT
            sSceneModeMap[Scene.SETTING_LANDSCAPE] = CameraCharacteristics.CONTROL_SCENE_MODE_LANDSCAPE
            sSceneModeMap[Scene.SETTING_NIGHT] = CameraCharacteristics.CONTROL_SCENE_MODE_NIGHT
            sSceneModeMap[Scene.SETTING_NIGHT_PORTRAIT] = CameraCharacteristics.CONTROL_SCENE_MODE_NIGHT_PORTRAIT
            sSceneModeMap[Scene.SETTING_BEACH] = CameraCharacteristics.CONTROL_SCENE_MODE_BEACH
            sSceneModeMap[Scene.SETTING_SNOW] = CameraCharacteristics.CONTROL_SCENE_MODE_SNOW
            sSceneModeMap[Scene.SETTING_SPORTS] = CameraCharacteristics.CONTROL_SCENE_MODE_SPORTS
            sSceneModeMap[Scene.SETTING_PARTY] = CameraCharacteristics.CONTROL_SCENE_MODE_PARTY
            sSceneModeMap[Scene.SETTING_DOCUMENT] = CameraCharacteristics.CONTROL_SCENE_MODE_BARCODE

            sSceneModeMap[Scene.SETTING_SUNSET] = CameraCharacteristics.CONTROL_SCENE_MODE_SUNSET
            sSceneModeMap[Scene.SETTING_STEADYPHOTO] = CameraCharacteristics.CONTROL_SCENE_MODE_STEADYPHOTO
            sSceneModeMap[Scene.SETTING_FIREWORKS] = CameraCharacteristics.CONTROL_SCENE_MODE_FIREWORKS
            sSceneModeMap[Scene.SETTING_CANDLELIGHT] = CameraCharacteristics.CONTROL_SCENE_MODE_CANDLELIGHT
            sSceneModeMap[Scene.SETTING_THEATRE] = CameraCharacteristics.CONTROL_SCENE_MODE_THEATRE
            sSceneModeMap[Scene.SETTING_ACTION] = CameraCharacteristics.CONTROL_SCENE_MODE_ACTION

            sWhiteBalanceMap[WhiteBalance.SETTING_AUTO] = CameraCharacteristics.CONTROL_AWB_MODE_AUTO
            sWhiteBalanceMap[WhiteBalance.SETTING_INCANDESCENT] = CameraCharacteristics.CONTROL_AWB_MODE_INCANDESCENT
            sWhiteBalanceMap[WhiteBalance.SETTING_FLUORESCENT] = CameraCharacteristics.CONTROL_AWB_MODE_FLUORESCENT
            sWhiteBalanceMap[WhiteBalance.SETTING_DAYLIGHT] = CameraCharacteristics.CONTROL_AWB_MODE_DAYLIGHT
            sWhiteBalanceMap[WhiteBalance.SETTING_CLOUDY_DAYLIGHT] =
                    CameraCharacteristics.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
        }

        @Throws(CameraException::class)
        internal fun getSpecs(context: Context, type: CameraType): CameraSpec {
            val impl = Camera2SpecImpl(context)
            val spec = impl.getCameraSpec(type)

            return CameraSpec(
                    type = type,
                    flashModeSpecs = impl.getFlashModes(spec),
                    focusModeSpecs = impl.getFocusModes(spec),
                    jpegPictureSizes = impl.getPictureSizes(spec, CaptureFormat.Jpeg),
                    rawPictureSizes = impl.getPictureSizes(spec, CaptureFormat.Raw),
                    previewSizes = impl.getPreviewSizes(spec),
                    sceneSpecs = impl.getScenes(spec),
                    whiteBalanceSpecs = impl.getWhiteBalances(spec)
            ).also {
                it.init()
            }
        }

        internal fun toSceneInt(scene: Scene): Int {
            return sSceneModeMap[scene]!!
        }

        internal fun toAfModeInt(mode: FocusMode): Int {
            return sFocusModeMap[mode]!!
        }

        internal fun toAwbInt(mode: WhiteBalance): Int {
            return sWhiteBalanceMap[mode]!!
        }

        internal fun toFlashModeInt(mode: FlashMode): Int {
            return sFlashModeMap[mode]!!
        }

        internal fun toImageFormatInt(format: CaptureFormat): Int {
            return sCaptureFormatMap[format]!!
        }
    }
}
