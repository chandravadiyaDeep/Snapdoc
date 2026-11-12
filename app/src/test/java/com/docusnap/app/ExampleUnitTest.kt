package com.docusnap.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun releaseVersion_matchesV020() {
        // Keep the release metadata test aligned with the current app version.
        assertEquals("0.2.0", BuildConfig.VERSION_NAME)
    }
}
