package moe.antimony.hoshi.features.sync

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveEndpointsTest {
    @After
    fun resetServer() {
        DriveEndpoints.serverUrl = null
    }

    @Test
    fun defaultsToGoogle() {
        assertFalse(DriveEndpoints.usesCustomServer)
        assertEquals("https://oauth2.googleapis.com/device/code", DriveEndpoints.deviceCodeUrl)
        assertEquals("https://oauth2.googleapis.com/token", DriveEndpoints.tokenUrl)
        assertEquals("https://www.googleapis.com/drive/v3/files?q=x", driveUrl("files", mapOf("q" to "x")))
        assertEquals("https://www.googleapis.com/upload/drive/v3/files", DriveEndpoints.upload("files"))
    }

    @Test
    fun customServerServesOAuthAndDriveFromOneOrigin() {
        DriveEndpoints.serverUrl = DriveEndpoints.normalize(" http://192.168.1.10:8737/ ")
        assertTrue(DriveEndpoints.usesCustomServer)
        assertEquals("http://192.168.1.10:8737/device/code", DriveEndpoints.deviceCodeUrl)
        assertEquals("http://192.168.1.10:8737/token", DriveEndpoints.tokenUrl)
        assertEquals("http://192.168.1.10:8737/drive/v3/files?q=x", driveUrl("files", mapOf("q" to "x")))
        assertEquals("http://192.168.1.10:8737/upload/drive/v3/files/a", DriveEndpoints.upload("files/a"))
    }

    @Test
    fun blankServerMeansGoogle() {
        assertNull(DriveEndpoints.normalize(""))
        assertNull(DriveEndpoints.normalize("  /"))
        assertNull(DriveEndpoints.normalize(null))
    }
}
