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
}
