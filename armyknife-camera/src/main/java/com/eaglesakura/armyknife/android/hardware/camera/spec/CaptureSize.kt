package com.eaglesakura.armyknife.android.hardware.camera.spec

/**
 * 撮影・プレビュー用のサイズを返す
 */
data class CaptureSize(
    /**
     * The number of pixels for horizontal.
     */
    val width: Int,

    /**
     * The number of pixels for vertical.
     */
    val height: Int
) {

    /**
     * the "Aspect ID" near by aspect ratio.
     */
    val aspectType: Aspect = Aspect.getNearAspect(aspect)

    /**
     * ピクセル数をメガピクセル単位で取得する
     *
     * @return 計算されたメガピクセル
     */
    val megaPixel: Double
        get() = (width * height).toDouble() / 1000.0 / 1000.0

    /**
     * ユーザー表示用のメガピクセル数を取得する。
     * <br></br>
     * 小数点第一位まで計算する
     * <br></br>
     * 例) 5.0
     * <br></br>
     * 例)13.1
     *
     * @return 表示用のメガピクセル
     */
    @Suppress("unused")
    val megaPixelText: String
        get() = "%.1f".format(megaPixel)

    /**
     * アスペクト比表示用テキストを取得する
     * 例) 16:9
     */
    @Suppress("unused")
    val aspectText: String
        get() = aspectType.aspectText

    /**
     * This value is aspect ratio.
     * example) When aspect is "16:9" returns 1.7777
     */
    val aspect: Double
        get() = width.toDouble() / height.toDouble()

    /**
     * 一意に識別するためのIDを取得する
     */
    val id: String
        get() = "pic(${width}x$height)"

    /**
     * This method returns preview window size.
     * The area of returns value less than area of window.
     */
    fun getPreviewSizeInWindow(windowWidth: Int, windowHeight: Int): Array<Int> {
        val width = (windowHeight.toDouble() * aspect + ROUND_UP).toInt()
        if (width <= windowWidth) {
            return arrayOf(width, windowHeight)
        }

        return arrayOf(windowWidth, (windowWidth.toDouble() / aspect + ROUND_UP).toInt())
    }

    /**
     * This method returns preview window size.
     * The area of returns value greater than area of window.
     */
    fun getPreviewSizeWrapWindow(windowWidth: Int, windowHeight: Int): Array<Int> {
        val sizeInWindow = getPreviewSizeInWindow(windowWidth, windowHeight)
        if (sizeInWindow[0] < windowWidth) {
            val scale = windowWidth.toDouble() / sizeInWindow[0].toDouble()
            sizeInWindow[0] = windowWidth
            sizeInWindow[1] = (sizeInWindow[1] * scale + ROUND_UP).toInt()
        } else {
            val scale = windowHeight.toDouble() / sizeInWindow[1].toDouble()
            sizeInWindow[0] = (sizeInWindow[0] * scale + ROUND_UP).toInt()
            sizeInWindow[1] = windowHeight
        }
        return sizeInWindow
    }

    companion object {
        private const val ROUND_UP = 0.999999999
    }

    enum class Aspect(
        /**
         * 横ピクセル数 / 縦ピクセル数のアスペクト比を取得する
         */
        val aspect: Double,

        /**
         * アスペクト比のテキストを取得する
         * <br></br>
         * 例：16:9
         */
        val aspectText: String
    ) {
        /**
         * 縦横1:1
         */
        WH1x1(1.0, "1:1"),

        /**
         * 縦横3x2
         */
        WH3x2(3.0 / 2.0, "3:2"),
        /**
         * 縦横4:3
         */
        WH4x3(4.0 / 3.0, "4:3"),

        /**
         * 縦横16:9
         */
        WH16x9(16.0 / 9.0, "16:9"),

        /**
         * 縦横16:10
         */
        WH16x10(16.0 / 10.0, "16:10");

        companion object {
            /**
             * 最も近いアスペクト比を取得する
             */
            internal fun getNearAspect(aspect: Double): Aspect {
                var diffNear = 99999999.0
                var result: Aspect = WH3x2

                val values = values()
                for (value in values) {
                    val checkDiff = Math.abs(value.aspect - aspect)
                    if (checkDiff < diffNear) {
                        // 差が小さいならコレにする
                        result = value
                        // 次はコレが比較対象
                        diffNear = checkDiff
                    }
                }
                return result
            }
        }
    }

    /**
     * CaptureSizeの縦横比を満たし、かつminWidth/minHeight以上の大きさを返却する
     *
     * @param flipOrientation 縦横サイズを入れ替えている場合はtrue
     * @param minWidth 最小限の幅
     * @param minHeight 最小限の高さ
     * @return 新しい縦横サイズ
     */
    @Suppress("unused")
    fun getViewSize(flipOrientation: Boolean, minWidth: Int, minHeight: Int): Array<Int> {
        var aspect = aspect.toFloat()
        if (flipOrientation) {
            aspect = height.toFloat() / width.toFloat()
        }
        val result = arrayOf(minWidth, minHeight)
        result[0] = (minHeight * aspect).toInt()
        if (result[0] < minWidth) {
            result[0] = minWidth
            result[1] = (minWidth / aspect).toInt()
        }
        return result
    }
}
