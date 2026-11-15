package com.nuvio.tv.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateInstallPolicyTest {
    @Test
    fun `published release feed is only for the regular full app`() {
        assertTrue(UpdateInstallPolicy.supportsReleaseUpdates("com.nuvio.livetv"))
        assertFalse(UpdateInstallPolicy.supportsReleaseUpdates("com.nuvio.livetv.test"))
        assertFalse(UpdateInstallPolicy.supportsReleaseUpdates("com.nuvio.app"))
        assertFalse(UpdateInstallPolicy.supportsReleaseUpdates("other.app"))
    }

    @Test
    fun `regular release cannot update test app or another app`() {
        assertFalse(UpdateInstallPolicy.isSelfUpdate("com.nuvio.livetv.test", 107102, "com.nuvio.livetv", 107105))
        assertFalse(UpdateInstallPolicy.isSelfUpdate("com.nuvio.livetv", 107104, "other.app", 107105))
    }

    @Test
    fun `valid update has same package and higher version code`() {
        assertTrue(UpdateInstallPolicy.isSelfUpdate("com.nuvio.livetv", 107104, "com.nuvio.livetv", 107105))
        assertTrue(UpdateInstallPolicy.isSelfUpdate("com.nuvio.livetv.test", 107102, "com.nuvio.livetv.test", 107103))
        assertFalse(UpdateInstallPolicy.isSelfUpdate("com.nuvio.livetv", 107105, "com.nuvio.livetv", 107105))
        assertFalse(UpdateInstallPolicy.isSelfUpdate("com.nuvio.livetv", 107105, "com.nuvio.livetv", 107104))
    }

    @Test
    fun `unreadable archive is rejected`() {
        assertFalse(UpdateInstallPolicy.isSelfUpdate("com.nuvio.livetv", 107104, null, null))
    }
}
