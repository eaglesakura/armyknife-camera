package com.eaglesakura.armyknife.android.hardware.camera

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import com.eaglesakura.armyknife.android.hardware.camera.error.CameraException
import com.eaglesakura.armyknife.android.hardware.camera.error.CameraSpecNotFoundException
import com.eaglesakura.armyknife.android.hardware.camera.spec.CameraType
import com.eaglesakura.armyknife.android.hardware.camera.spec.CaptureSize
import com.eaglesakura.armyknife.android.hardware.camera.spec.FlashMode
import com.eaglesakura.armyknife.android.hardware.camera.spec.FocusMode
import com.eaglesakura.armyknife.android.hardware.camera.spec.Scene
import com.eaglesakura.armyknife.android.hardware.camera.spec.WhiteBalance
import java.util.Collections

@Suppress("unused")
/**
 * リアカメラ、フロントカメラごとのスペックを示したクラス
 */
class CameraSpec internal constructor(
    val type: CameraType,

        /**
         * カメラのプレビューサイズ
         */
    /**
     * プレビューサイズ一覧を取得
     */
    private val previewSizes: List<CaptureSize>,

    /**
     * カメラの撮影サイズ
     */
    private val jpegPictureSizes: List<CaptureSize>,

    /**
     * カメラの撮影サイズ
     */
    private val rawPictureSizes: List<CaptureSize>,

    /**
     * サポートしているシーン
     */
    private val sceneSpecs: List<Scene>,

    /**
     * ホワイトバランス設定一覧
     */
    private val whiteBalanceSpecs: List<WhiteBalance>,

    /**
     * フォーカスモード一覧
     */
    private val focusModeSpecs: List<FocusMode>,

    /**
     * フラッシュモード一覧
     */
    private val flashModeSpecs: List<FlashMode>
) {

    /**
     * サポートしているプレビューサイズのうち、最も小さいサイズを返却する
     */
    val minimumPreviewSize: CaptureSize
        get() = previewSizes[previewSizes.size - 1]

    val maximumPreviewSize: CaptureSize
        get() = previewSizes[0]

    val fullJpegPictureSize: CaptureSize
        get() = jpegPictureSizes[0]

    /**
     * サイズの大きいものが若いインデックスになるように調整する
     */
    @Suppress("JavaCollectionsStaticMethodOnImmutableList")
    internal fun init() {
        Collections.sort(jpegPictureSizes) { a, b -> -java.lang.Double.compare(a.megaPixel, b.megaPixel) }
        Collections.sort(rawPictureSizes) { a, b -> -java.lang.Double.compare(a.megaPixel, b.megaPixel) }
        Collections.sort(previewSizes) { a, b -> -java.lang.Double.compare(a.megaPixel, b.megaPixel) }
    }

    /**
     * CaptureSizeの縦横比を満たし、かつminWidth/minHeight以上の大きさを返却する
     *
     * @param minWidth 最小限の幅
     * @param minHeight 最小限の高さ
     * @param size 計算するアスペクト比
     * @return 新しい縦横サイズ
     */
    fun getViewSize(minWidth: Int, minHeight: Int, size: CaptureSize): IntArray {
        val result = intArrayOf(minWidth, minHeight)
        result[0] = (minHeight * size.aspect).toInt()
        if (result[0] < minWidth) {
            result[0] = minWidth
            result[1] = (minWidth / size.aspect).toInt()
        }
        return result
    }

    /**
     * 最低限のスペックを満たすプレビューサイズを取得する
     */
    fun getPreviewSize(requireWidth: Int, requireHeight: Int): CaptureSize {
        return chooseShotSize(previewSizes, requireWidth, requireHeight, requireWidth, requireHeight)
    }

    /**
     * 指定したアスペクト比で最も大きなサイズを取得する
     */
    fun getJpegPictureSize(aspect: CaptureSize.Aspect): CaptureSize {
        for (size in jpegPictureSizes) {
            if (size.aspectType === aspect) {
                return size
            }
        }

        return jpegPictureSizes[0]
    }

    /**
     * 最低限のスペックを満たす撮影サイズを取得する
     */
    fun getJpegPictureSize(requireWidth: Int, requireHeight: Int): CaptureSize {
        return chooseShotSize(jpegPictureSizes, requireWidth, requireHeight, requireWidth, requireHeight)
    }

    /**
     * シーンをサポートしていたらtrue
     */
    fun isSupportedScene(scene: Scene): Boolean {
        return sceneSpecs.contains(scene)
    }

    fun isSupported(scene: Scene): Boolean {
        return sceneSpecs.contains(scene)
    }

    fun isSupported(whiteBalance: WhiteBalance): Boolean {
        return whiteBalanceSpecs.contains(whiteBalance)
    }

    fun isSupported(flashMode: FlashMode): Boolean {
        return flashModeSpecs.contains(flashMode)
    }

    fun isSupported(focusMode: FocusMode): Boolean {
        return focusModeSpecs.contains(focusMode)
    }

    /**
     * フラッシュモードを持っていたらtrue
     */
    fun hasFlash(): Boolean {
        return !flashModeSpecs.isEmpty()
    }

    @SuppressLint("LogNotTimber")
    @Suppress("UNUSED_PARAMETER")
    private fun chooseShotSize(
        targetSizes: List<CaptureSize>,
        width: Int,
        height: Int,
        minWidth: Int,
        minHeight: Int
    ): CaptureSize {
        val requestLong = Math.max(minWidth, minHeight)
        val requestShort = Math.min(minWidth, minHeight)

        val target = targetSizes[0]

        // 逆方向（プレビューサイズの小さいもの）から、最低限の要件を満たすアイテムを探しておく
        for (index in targetSizes.indices.reversed()) {
            val size = targetSizes[index]
            val captureLong = Math.max(size.width, size.height)
            val captureShort = Math.min(size.width, size.height)
            if (captureLong >= requestLong && captureShort >= requestShort) {
                Log.d("Camera", "Choose [$captureLong x $captureShort] Req[$requestLong x $requestShort]")
                return size
            }
        }
        return target
    }

    /**
     * IDからプレビューサイズを逆引きする
     */
    @Throws(CameraException::class)
    fun getPreviewSize(id: String): CaptureSize {
        for (size in previewSizes) {
            if (size.id == id) {
                return size
            }
        }
        throw CameraSpecNotFoundException(id)
    }

    /**
     * IDから撮影サイズを逆引きする
     */
    @Throws(CameraException::class)
    fun getJpegPictureSize(id: String): CaptureSize {
        for (size in jpegPictureSizes) {
            if (size.id == id) {
                return size
            }
        }
        throw CameraSpecNotFoundException(id)
    }

    /**
     * IDから撮影サイズを逆引きする
     */
    @Throws(CameraException::class)
    fun getRawPictureSize(id: String): CaptureSize {
        for (size in rawPictureSizes) {
            if (size.id == id) {
                return size
            }
        }
        throw CameraSpecNotFoundException(id)
    }

    companion object {

        @Throws(CameraException::class)
        fun getSpecs(context: Context, api: CameraApi, type: CameraType): CameraSpec {
            var targetApi = api
            if (targetApi == CameraApi.Default) {
                targetApi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CameraApi.Camera2
                } else {
                    CameraApi.Legacy
                }
            }

            if (targetApi == CameraApi.Camera2) {
                // Camera2
                return Camera2SpecImpl.getSpecs(context, type)
            } else {
                TODO("LegacyCameraSpecImpl()")
            }
        }

        @Throws(CameraException::class)
        fun getSpecs(context: Context, type: CameraType): CameraSpec {
            return getSpecs(context, CameraApi.Default, type)
        }
    }
}
