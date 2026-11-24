package com.nuvio.tv.core.auth

import org.junit.Assert.*
import org.junit.Test

class TestPasswordLoginPolicyTest {
    @Test fun onlyOfficialTestPackageOffersExperiment() {
        assertTrue(TestPasswordLoginPolicy.available("com.nuvio.livetv.test", false))
        assertFalse(TestPasswordLoginPolicy.available("com.nuvio.livetv", false))
        assertFalse(TestPasswordLoginPolicy.available("com.nuvio.livetv.trial", false))
        assertFalse(TestPasswordLoginPolicy.available("com.nuvio.appdebug", false))
        assertFalse(TestPasswordLoginPolicy.available("com.nuvio.livetv.test.other", false))
    }
    @Test fun customServerKeepsExistingCapabilityBehavior() {
        assertFalse(TestPasswordLoginPolicy.available("com.nuvio.livetv.test", true))
    }
    @Test fun customerErrorNeverEchoesProviderOrResponseBody() {
        val response = "Nuvio response with private-token-example"
        val shown = TestPasswordLoginPolicy.errorMessage(response)
        assertEquals("Unable to sign in.  Please try again.", shown)
        assertFalse(shown.contains("Nuvio"))
        assertFalse(shown.contains("private-token-example"))
    }
    @Test fun expectedErrorsHaveUsefulNeutralCopy() {
        assertEquals("Email or password wasn't accepted.  Please try again.",
            TestPasswordLoginPolicy.errorMessage("Invalid credentials"))
        assertEquals("Unable to connect.  Check your internet connection and try again.",
            TestPasswordLoginPolicy.errorMessage("Network timeout"))
    }
}
