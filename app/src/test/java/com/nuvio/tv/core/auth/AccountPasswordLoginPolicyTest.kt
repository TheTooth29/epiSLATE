package com.nuvio.tv.core.auth

import org.junit.Assert.*
import org.junit.Test

class AccountPasswordLoginPolicyTest {
    @Test fun onlyApprovedOfficialPackagesOfferPasswordLogin() {
        assertTrue(AccountPasswordLoginPolicy.available("com.nuvio.livetv.test", false))
        assertTrue(AccountPasswordLoginPolicy.available("com.nuvio.livetv", false))
        assertFalse(AccountPasswordLoginPolicy.available("com.nuvio.livetv.trial", false))
        assertFalse(AccountPasswordLoginPolicy.available("com.nuvio.appdebug", false))
        assertFalse(AccountPasswordLoginPolicy.available("com.nuvio.livetv.test.other", false))
    }
    @Test fun customServerKeepsExistingCapabilityBehavior() {
        assertFalse(AccountPasswordLoginPolicy.available("com.nuvio.livetv.test", true))
        assertFalse(AccountPasswordLoginPolicy.available("com.nuvio.livetv", true))
    }
    @Test fun customerErrorNeverEchoesProviderOrResponseBody() {
        val response = "Nuvio response with private-token-example"
        val shown = AccountPasswordLoginPolicy.errorMessage(response)
        assertEquals("Unable to sign in.  Please try again.", shown)
        assertFalse(shown.contains("Nuvio"))
        assertFalse(shown.contains("private-token-example"))
    }
    @Test fun expectedErrorsHaveUsefulNeutralCopy() {
        assertEquals("Email or password wasn't accepted.  Please try again.",
            AccountPasswordLoginPolicy.errorMessage("Invalid credentials"))
        assertEquals("Unable to connect.  Check your internet connection and try again.",
            AccountPasswordLoginPolicy.errorMessage("Network timeout"))
    }
}
