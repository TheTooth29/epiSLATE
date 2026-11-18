package com.nuvio.tv.core.trial

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TrialBuildReportTest {
    private val build = TrialBuildReport(107111, "1.0.0-beta.4-trial", 7)
    @Test fun legacyServerKeepsLegacySignedProtocol() {
        assertNull(TrialBuildReport.negotiate(JSONObject(), build))
        assertNull(TrialBuildReport.negotiate(JSONObject().put("build_metadata_version", 2), build))
        assertEquals("challenge\n123456", TrialBuildReport.message("challenge", "redeem", "123456", null))
        assertEquals("challenge\n", TrialBuildReport.message("challenge", "status", "", null))
    }
    @Test fun negotiatedReportBindsEveryFieldToBothSignedActions() {
        assertEquals(build, TrialBuildReport.negotiate(JSONObject().put("build_metadata_version", 1), build))
        assertEquals("challenge\n123456\nbuild:v1:107111:1.0.0-beta.4-trial:7",
            TrialBuildReport.message("challenge", "redeem", "123456", build))
        assertEquals("challenge\n\nbuild:v1:107111:1.0.0-beta.4-trial:7",
            TrialBuildReport.message("challenge", "status", "not-part-of-status-proof", build))
        val body = build.attach(JSONObject())
        assertEquals(2, body.getInt("proof_version"))
        assertEquals(107111, body.getJSONObject("app_build").getInt("version_code"))
        assertEquals(7, body.getJSONObject("app_build").getInt("build_number"))
    }
    @Test fun unnumberedBuildDoesNotGuessTheReleaseNumber() {
        val local = TrialBuildReport(107111, "local-trial")
        assertFalse(local.attach(JSONObject()).getJSONObject("app_build").has("build_number"))
        assertEquals("\nbuild:v1:107111:local-trial:", local.proofSuffix())
    }
}
