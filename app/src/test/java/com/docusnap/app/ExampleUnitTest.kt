package com.docusnap.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test fun initialVersion_isOne() {
        // Verify that the app module has the expected initial release version.
        assertEquals("1.0.0", BuildConfig.VERSION_NAME)
    }
}