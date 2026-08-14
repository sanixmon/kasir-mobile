package com.kasir.mobile.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.content
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testSessionDtoDeserialization() {
        val jsonStr = """
            {
                "id": "s-test01",
                "queueNo": 1,
                "nama": "Budi Test",
                "items": [{"code": "SA", "qty": 2}],
                "startTime": 1700000000000,
                "tanggal": "2026-07-26",
                "payAwal": "cash"
            }
        """.trimIndent()

        val dto = json.decodeFromString<SessionDto>(jsonStr)
        assertEquals("s-test01", dto.id)
        assertEquals(1, dto.queueNo)
        assertEquals("Budi Test", dto.nama)
        assertEquals(1, dto.items.size)
        assertEquals("SA", dto.items[0].code)
        assertEquals(2, dto.items[0].qty)
        assertEquals(1700000000000L, dto.startTime)
        assertEquals("2026-07-26", dto.tanggal)
        assertEquals("cash", dto.payAwal)
    }

    @Test
    fun testTransactionDtoDeserialization() {
        val jsonStr = """
            {
                "id": "t-1001",
                "no": 1001,
                "queueNo": 5,
                "nama": "Siti Test",
                "tanggal": "2026-07-26",
                "startTime": 1700000000000,
                "endTime": 1700003600000,
                "items": "Scooter Anak x1",
                "ot": "-",
                "otDur": "-",
                "totalBase": 35000.0,
                "totalOT": 0.0,
                "totalTol": 0.0,
                "grandTotal": 35000.0,
                "totalAll": 35000.0,
                "payAwal": "cash",
                "cash": 35000.0,
                "qris": 0.0,
                "shift": "2026-07-26"
            }
        """.trimIndent()

        val dto = json.decodeFromString<TransactionDto>(jsonStr)
        assertEquals("t-1001", dto.id)
        assertEquals(1001L, dto.no)
        assertEquals(5, dto.queueNo)
        assertEquals("Siti Test", dto.nama)
        assertEquals(35000.0, dto.grandTotal, 0.01)
        assertEquals("2026-07-26", dto.shift)
    }

    @Test
    fun testUserDtoDeserialization() {
        val jsonStr = """
            {
                "username": "kasir1",
                "password": "secretpassword",
                "role": "cashier"
            }
        """.trimIndent()

        val dto = json.decodeFromString<UserDto>(jsonStr)
        assertEquals("kasir1", dto.username)
        assertEquals("secretpassword", dto.password)
        assertEquals("cashier", dto.role)
    }

    @Test
    fun testItemCatalogLookup() {
        val item = ItemCatalog.findByCode("SA")
        assertNotNull(item)
        assertEquals("Scooter Anak", item?.name)
        assertEquals(35000.0, item?.priceHour ?: 0.0, 0.01)
    }

    @Test
    fun testKasirRpcModelsSerialization() {
        val fetchResponseJson = """
            {
                "sessions": [
                    {
                        "id": "s-1",
                        "queueNo": 1,
                        "nama": "Test Session",
                        "items": [],
                        "startTime": 1700000000000,
                        "tanggal": "2026-07-26",
                        "payAwal": "cash"
                    }
                ],
                "transactions": [],
                "users": [
                    {"username": "admin", "password": "123", "role": "admin"}
                ],
                "settings": {"adminPass": "1234"}
            }
        """.trimIndent()

        val res = json.decodeFromString<FetchAllDataResponse>(fetchResponseJson)
        assertEquals(1, res.sessions.size)
        assertEquals(1, res.users.size)
        assertEquals("1234", res.settings["adminPass"])

        val actionResJson = """
            {
                "success": true,
                "error": null,
                "session": null
            }
        """.trimIndent()
        val actionRes = json.decodeFromString<ActionSuccessResponse>(actionResJson)
        assertTrue(actionRes.success)

        val verifyAdminJson = """{"valid": true}"""
        val verifyAdminRes = json.decodeFromString<VerifyAdminResponse>(verifyAdminJson)
        assertTrue(verifyAdminRes.valid)

        val deletionLogsJson = """
            {
                "logs": [
                    {
                        "id": 1,
                        "txnId": "t-1",
                        "txnNo": 100,
                        "txnNama": "Deleted Txn",
                        "txnTanggal": "2026-07-26",
                        "txnTotalAll": 50000.0,
                        "deletedAt": 1700000000000,
                        "deletedBy": "admin"
                    }
                ]
            }
        """.trimIndent()
        val deletionLogsRes = json.decodeFromString<DeletionLogsResponse>(deletionLogsJson)
        assertEquals(1, deletionLogsRes.logs.size)
        assertEquals("Deleted Txn", deletionLogsRes.logs[0].txnNama)
    }

    @Test
    fun testUserManagementRpcRequestSerialization() {
        val saveJson = """
            {"action": "save_user", "payload": {"username": "kasir2", "password": "rahasia123", "role": "cashier"}}
        """.trimIndent()
        val saveReq = json.decodeFromString<KasirRpcRequest>(saveJson)
        assertEquals("save_user", saveReq.action)
        assertEquals("kasir2", saveReq.payload["username"]?.jsonPrimitive?.content)
        assertEquals("rahasia123", saveReq.payload["password"]?.jsonPrimitive?.content)
        assertEquals("cashier", saveReq.payload["role"]?.jsonPrimitive?.content)

        val deleteJson = """
            {"action": "delete_user", "payload": {"username": "kasir2"}}
        """.trimIndent()
        val deleteReq = json.decodeFromString<KasirRpcRequest>(deleteJson)
        assertEquals("delete_user", deleteReq.action)
        assertEquals("kasir2", deleteReq.payload["username"]?.jsonPrimitive?.content)
    }
}
