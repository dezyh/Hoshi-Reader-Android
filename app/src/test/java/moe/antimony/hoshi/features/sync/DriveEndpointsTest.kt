package moe.antimony.hoshi.features.sync

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DriveEndpointsTest {
    @Test
    fun defaultsToGoogle() {
        val endpoints = DriveEndpoints.google()
        assertFalse(endpoints.usesCustomServer)
        assertEquals("https://oauth2.googleapis.com/device/code", endpoints.deviceCodeUrl)
        assertEquals("https://oauth2.googleapis.com/token", endpoints.tokenUrl)
        assertEquals("https://www.googleapis.com/drive/v3/files?q=x", endpoints.driveUrl("files", mapOf("q" to "x")))
        assertEquals("https://www.googleapis.com/upload/drive/v3/files", endpoints.upload("files"))
    }

    @Test
    fun customServerServesOAuthAndDriveFromOneOrigin() {
        val endpoints = DriveEndpoints(" http://192.168.1.10:8737/ ") {}
        assertTrue(endpoints.usesCustomServer)
        assertEquals("http://192.168.1.10:8737/device/code", endpoints.deviceCodeUrl)
        assertEquals("http://192.168.1.10:8737/token", endpoints.tokenUrl)
        assertEquals("http://192.168.1.10:8737/drive/v3/files?q=x", endpoints.driveUrl("files", mapOf("q" to "x")))
        assertEquals("http://192.168.1.10:8737/upload/drive/v3/files/a", endpoints.upload("files/a"))
    }

    @Test
    fun blankServerMeansGoogle() {
        assertNull(DriveEndpoints.normalize(""))
        assertNull(DriveEndpoints.normalize("  /"))
        assertNull(DriveEndpoints.normalize(null))
    }

    @Test
    fun savingPersistsOnlyAChangedServer() {
        val persisted = mutableListOf<String?>()
        val endpoints = DriveEndpoints("http://a:1") { persisted += it }
        assertFalse(endpoints.save("http://a:1/"))
        assertTrue(endpoints.save("http://b:2"))
        assertTrue(endpoints.save(" "))
        assertFalse(endpoints.usesCustomServer)
        assertEquals(listOf("http://b:2", null), persisted)
    }

    @Test
    fun clientRequestsFollowAServerChangeWithoutRestarting() = runTest {
        val endpoints = DriveEndpoints.google()
        val requested = mutableListOf<String>()
        val client = GoogleDriveClient(object : DriveAccessTokenProvider {
            override suspend fun accessToken() = "token"
        }, { SyncProvider.Gdrive }, {}, UnconfinedTestDispatcher(testScheduler), { address ->
            requested += address
            object : HttpURLConnection(URL(address)) {
                override fun setRequestMethod(value: String) { method = value }
                override fun connect() = Unit
                override fun disconnect() = Unit
                override fun usingProxy() = false
                override fun getOutputStream() = ByteArrayOutputStream()
                override fun getResponseCode() = 200
                override fun getInputStream() = """{"id":"a","name":"a.json","mimeType":"application/json","createdTime":""}""".byteInputStream()
            }
        }, endpoints)
        client.request("files")
        endpoints.save("http://192.168.1.10:8737")
        client.request("files")
        client.write(byteArrayOf(1), "a.json", "parent")
        assertEquals(
            listOf(
                "https://www.googleapis.com/drive/v3/files?",
                "http://192.168.1.10:8737/drive/v3/files?",
                "http://192.168.1.10:8737/upload/drive/v3/files?uploadType=multipart&fields=id%2Cname%2CmimeType%2Cmd5Checksum%2CcreatedTime",
            ),
            requested,
        )
    }
}
