package com.eaglesakura.armyknife.android.hardware.camera

import android.Manifest
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.eaglesakura.armyknife.android.hardware.camera.spec.CameraType
import com.eaglesakura.armyknife.android.hardware.camera.spec.FocusMode
import com.eaglesakura.armyknife.android.hardware.camera.spec.Scene
import com.eaglesakura.armyknife.android.hardware.camera.spec.WhiteBalance
import com.eaglesakura.armyknife.android.junit4.extensions.compatibleBlockingTest
import com.eaglesakura.armyknife.android.junit4.extensions.targetContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.* // ktlint-disable no-wildcard-imports
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraSpecTest {

    @Rule
    @JvmField
    val rule = GrantPermissionRule.grant(Manifest.permission.CAMERA)!!

    @Test
    fun getSpecs() = compatibleBlockingTest {
        val specs = CameraSpec.getSpecs(targetContext, CameraApi.Default, CameraType.Back)

        assertEquals(CameraType.Back, specs.type)
        specs.getJpegPictureSize(640, 480).also {
            assertThat(it.width).apply {
                isGreaterThan(0)
                isLessThanOrEqualTo(640)
            }
            assertThat(it.height).apply {
                isGreaterThan(0)
                isLessThanOrEqualTo(480)
            }
        }
    }

    @Test
    fun connectAndDisconnect() = compatibleBlockingTest {
        val specs = CameraSpec.getSpecs(targetContext, CameraApi.Default, CameraType.Back)
        val controlManager =
                CameraControlManager.newInstance(targetContext, CameraApi.Default, CameraConnectRequest(CameraType.Back))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            assertTrue(controlManager is Camera2ControlManager)
        }
        assertFalse(controlManager.connected)

        controlManager.connect(
                previewSurface = null,
                previewRequest = null,
                shotRequest = CameraPictureShotRequest(specs.fullJpegPictureSize)
        )
        try {
            assertTrue(controlManager.connected)
        } finally {
            controlManager.disconnect()
        }
    }

    @Test
    fun startPreview() = compatibleBlockingTest {
        val specs = CameraSpec.getSpecs(targetContext, CameraApi.Default, CameraType.Back)
        val controlManager =
                CameraControlManager.newInstance(targetContext, CameraApi.Default, CameraConnectRequest(CameraType.Back))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            assertTrue(controlManager is Camera2ControlManager)
        }
        assertFalse(controlManager.connected)

        controlManager.connect(
                previewSurface = null,
                previewRequest = CameraPreviewRequest(specs.minimumPreviewSize),
                shotRequest = CameraPictureShotRequest(specs.fullJpegPictureSize)
        )
        try {
            assertTrue(controlManager.connected)
        } finally {
            controlManager.disconnect()
        }
    }

    @Test
    fun takePicture() = compatibleBlockingTest {
        val specs = CameraSpec.getSpecs(targetContext, CameraApi.Default, CameraType.Back)
        val controlManager =
                CameraControlManager.newInstance(targetContext, CameraApi.Default, CameraConnectRequest(CameraType.Back))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            assertTrue(controlManager is Camera2ControlManager)
        }
        assertFalse(controlManager.connected)

        controlManager.connect(
                previewSurface = null,
                previewRequest = null,
                shotRequest = CameraPictureShotRequest(specs.fullJpegPictureSize)
        )
        try {
            assertTrue(controlManager.connected)

            val picture = controlManager.takePicture(
                    CameraEnvironmentRequest(
                            FocusMode.SETTING_AUTO,
                            Scene.SETTING_AUTO,
                            WhiteBalance.SETTING_AUTO
                    )
            )
            assertTrue(picture.buffer.isNotEmpty())
            assertEquals(specs.fullJpegPictureSize.width, picture.width)
            assertEquals(specs.fullJpegPictureSize.height, picture.height)

            // decode ok
            picture.decodeImage().also { bitmap ->
                assertEquals(
                        Math.max(specs.fullJpegPictureSize.width, specs.fullJpegPictureSize.height),
                        Math.max(bitmap.width, bitmap.height)
                )
                assertEquals(
                        Math.min(specs.fullJpegPictureSize.width, specs.fullJpegPictureSize.height),
                        Math.min(bitmap.width, bitmap.height)
                )
            }
        } finally {
            controlManager.disconnect()
        }
    }
}