package com.eaglesakura.armyknife.android.hardware.camera.spec

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eaglesakura.armyknife.android.junit4.extensions.compatibleTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureSizeTest {

    @Test
    fun getPreviewSizeInWindow() = compatibleTest {
        CaptureSize(640, 480).also { captureSize ->
            // just size.
            captureSize.getPreviewSizeInWindow(640, 480).also { previewSize ->
                assertEquals(2, previewSize.size)
                assertEquals(640, previewSize[0])
                assertEquals(480, previewSize[1])
            }
            // just with large size.
            captureSize.getPreviewSizeInWindow(640 * 2, 480 * 2).also { previewSize ->
                assertEquals(640 * 2, previewSize[0])
                assertEquals(480 * 2, previewSize[1])
            }
            // just with small size.
            captureSize.getPreviewSizeInWindow(640 / 2, 480 / 2).also { previewSize ->
                assertEquals(640 / 2, previewSize[0])
                assertEquals(480 / 2, previewSize[1])
            }
            // long width
            captureSize.getPreviewSizeInWindow(1280, 480).also { previewSize ->
                assertEquals(640, previewSize[0])
                assertEquals(480, previewSize[1])
            }
            // short width
            captureSize.getPreviewSizeInWindow(480, 480).also { previewSize ->
                assertEquals(480, previewSize[0])
                assertEquals(360, previewSize[1])
            }
            // long height
            captureSize.getPreviewSizeInWindow(640, 960).also { previewSize ->
                assertEquals(640, previewSize[0])
                assertEquals(480, previewSize[1])
            }
            // short height
            captureSize.getPreviewSizeInWindow(640, 360).also { previewSize ->
                assertEquals(480, previewSize[0])
                assertEquals(360, previewSize[1])
            }
        }
    }

    @Test
    fun getPreviewSizeWrapWindow() = compatibleTest {
        CaptureSize(640, 480).also { captureSize ->
            // just size.
            captureSize.getPreviewSizeWrapWindow(640, 480).also { previewSize ->
                assertEquals(2, previewSize.size)
                assertEquals(640, previewSize[0])
                assertEquals(480, previewSize[1])
            }
            // just with large size.
            captureSize.getPreviewSizeWrapWindow(640 * 2, 480 * 2).also { previewSize ->
                assertEquals(640 * 2, previewSize[0])
                assertEquals(480 * 2, previewSize[1])
            }
            // just with small size.
            captureSize.getPreviewSizeWrapWindow(640 / 2, 480 / 2).also { previewSize ->
                assertEquals(640 / 2, previewSize[0])
                assertEquals(480 / 2, previewSize[1])
            }
            // long width
            captureSize.getPreviewSizeWrapWindow(1280, 480).also { previewSize ->
                assertEquals(1280, previewSize[0])
                assertEquals(960, previewSize[1])
            }
            // short width
            captureSize.getPreviewSizeWrapWindow(480, 480).also { previewSize ->
                assertEquals(640, previewSize[0])
                assertEquals(480, previewSize[1])
            }
            // long height
            captureSize.getPreviewSizeWrapWindow(640, 960).also { previewSize ->
                assertEquals(1280, previewSize[0])
                assertEquals(960, previewSize[1])
            }
            // short height
            captureSize.getPreviewSizeWrapWindow(640, 360).also { previewSize ->
                assertEquals(640, previewSize[0])
                assertEquals(480, previewSize[1])
            }
        }
    }
}