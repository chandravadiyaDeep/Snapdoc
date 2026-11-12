package com.docusnap.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun releaseVersion_matchesV040() {
        // Keep the release metadata test aligned with the current app version.
        assertEquals("0.4.0", BuildConfig.VERSION_NAME)
    }
}
