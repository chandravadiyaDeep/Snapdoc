package com.docusnap.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun releaseVersion_matchesV050() {
        // Keep the release metadata test aligned with the current app version.
        assertEquals("0.5.0", BuildConfig.VERSION_NAME)
    }
}
