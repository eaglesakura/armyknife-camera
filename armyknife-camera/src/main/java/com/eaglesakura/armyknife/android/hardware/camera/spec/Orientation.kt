package com.eaglesakura.armyknife.android.hardware.camera.spec

/**
 * カメラの回転角情報
 */
data class Orientation internal constructor(
    /**
     * 回転角を取得する
     */
    val degree: Int
) {

    /**
     * 縦向きである場合はtrue
     */
    val vertical: Boolean
        get() = degree == 90 || degree == 180

    /**
     * 横向きであればtrue
     */
    val horizontal: Boolean
        get() = !vertical

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val ROTATE_0 = Orientation(0)

        val ROTATE_90 = Orientation(90)

        val ROTATE_180 = Orientation(180)

        val ROTATE_270 = Orientation(270)

        fun fromDegree(degree: Int): Orientation {
            var rotate = degree
            rotate = normalizeDegree(rotate)
            rotate = rotate / 90 * 90 // 90度区切りに修正する
            return when (rotate) {
                0 -> ROTATE_0
                90 -> ROTATE_90
                180 -> ROTATE_180
                270 -> ROTATE_270
                else -> throw IllegalStateException("Rotate error($rotate)")
            }
        }

        private fun normalizeDegree(rotate: Int): Int {
            var result = rotate
            while (rotate < 0) {
                result += 360
            }

            while (rotate > 360) {
                result -= 360
            }

            return result
        }
    }
}