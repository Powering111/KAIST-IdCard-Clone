package com.payrespect.idcard

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.s1.mobilecard.s1mobilecard.JniS1Pass


class ApduService : HostApduService() {
    companion object {
        val status : MutableLiveData<String> by lazy {MutableLiveData<String>("")}

        private const val S1PASS_KEY = "KeyType:S1PASS\n" +
                "Version:1.0.0.0\n" +
                "Date:20201116192327\n" +
                "ContractNumber:N2940431\n" +
                "KeyData:XJHvWI6lb7T5pWiMmDJLOi03YcZBj3dgspj5gnhGVdO9VcWHmaJeREFUPMzS29JAzgLfvaRTwiPBFevFYXt9CVXXqrmxHPwJDleF4XSZKKwkhWe8tjDRjCfrXeK7BkFnmsxyZtYnUSEnqmhS6cWX7VggQs7B4z6S4FTS5yrqGwqfpJZGdr8OjWA14NCKeu+uBBKSVqd1uNu3GoXpR4X0J84KDPsfEm+vJ2+40b+2bBF3/VR88sGkZbQuPEkAKGa7nzlbLtXFOnUMA2W4ilQxpMleQNIXNqZh/c3Bv6SK3ymMeSrIS5fRd3JQ2BppUvpwrsXaCZHp3jfwS0jlMIuiKg==\n"
        private const val STR_SITE_KEY = "cba785a1997d0c35106d24f0d920f407"

        // strCardNum = studentcard.sccardid.substring(0, 6) + "9" + studentcard.sccardid.substring(7, 13);
        private var strCardNum : String? = "0000000000000" // Secret

        private const val INS_READ_RECORD = 0xB2.toByte()
        private const val INS_SELECT_FILE = 0xA4.toByte()
        private const val INS_INIT_TRANS = 0x52.toByte()
        private const val INS_INTERNAL_AUTH = 0x82.toByte()
        private const val INS_EXTERNAL_AUTH = 0x84.toByte()
        private const val INS_COMPLETE_TRANSACTION = 0x54.toByte()

        fun changeCardNum(newCardNum: String){
            strCardNum = newCardNum
            status.value = "[ KAIST ID Card ]\nCARD_NUM:$strCardNum\n"
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private fun updateCardNum(){
        scope.launch {
            strCardNum = baseContext.dataStore.data.first()[stringPreferencesKey("K")] ?: "0000000000000"
            Log.d("Apdu Service", "DataStore Load: $strCardNum")
        }
    }

    override fun onCreate() {
        Log.d("Apdu Service", "Created!")
        updateCardNum()
    }
    private fun toHeX(a: ByteArray?): String {
        val tmp = a?.joinToString("") { m -> String.format("%02X", m) } ?: ""
        return if(tmp.length % 2 == 1) "0$tmp" else tmp
    }

    private fun intToHex(a: Int): String{
        val tmp = Integer.toHexString(a)
        return if (tmp.length < 2) "0$tmp" else tmp
    }

    private fun toHex(a: ByteArray?): String {
        val tmp = a?.joinToString("") { m -> String.format("%02x", m) } ?: ""
        return if(tmp.length % 2 == 1) "0$tmp" else tmp
    }

    // from base source
    private fun hexStringToByteArray(str: String): ByteArray {
        val str2 = if(str.length % 2 != 0) str + "0" else str
        val str3 = str2 + "00"
        return str3.chunked(2).map{ it.toInt(16).toByte() }.toByteArray()
    }

    // from base source
    private fun add80Padding(str: String): String {
        val length = str.length % 32
        if (length % 32 != 0) {
            var str2 = str + "80"
            for (i in 1 until (32 - length) / 2) {
                str2 += "00"
            }
            return str2
        }
        return str + "80000000000000000000000000000000"
    }

    private var state = 0
    private var s1pass : JniS1Pass? = null

    private fun jniCall(commandApdu: ByteArray): ByteArray? {
        val personalInfo = add80Padding("9999999916${intToHex(strCardNum!!.length)}${toHex(strCardNum!!.toByteArray())}00")
        if(s1pass == null){
            s1pass = JniS1Pass()
        }
        val l = s1pass?.makeCommandAPDU(commandApdu, STR_SITE_KEY, personalInfo)
        return l
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray? {
        if(commandApdu == null || strCardNum == null){
            return byteArrayOf()
        }

        updateCardNum()
        val ins = commandApdu[1]

        val responseApdu = when(ins) {
            INS_READ_RECORD -> {
                state = 0
                val tmp = hexStringToByteArray(strCardNum!!).copyOf(18)
                tmp[16] = 0x90.toByte()
                tmp
            }
            INS_SELECT_FILE -> {
                JniS1Pass().loadKeyFile(S1PASS_KEY)
                state = 1
                jniCall(commandApdu)
            }
            INS_INIT_TRANS ->
                if(state == 1){
                    state = 2
                    jniCall(commandApdu)
                } else null
            INS_INTERNAL_AUTH ->
                if (state == 2){
                    state = 3
                    jniCall(commandApdu)
                } else null
            INS_EXTERNAL_AUTH ->
                if (state == 3){
                    state = 4
                    jniCall(commandApdu)
                } else null
            INS_COMPLETE_TRANSACTION ->
                if(state == 4){
                    state = 0
                    jniCall(commandApdu)
                }
                else null
            else -> {
                state = 0
                null
            }
        }
        val commandType = when(ins){
            INS_READ_RECORD -> "READ_RECORD"
            INS_SELECT_FILE -> "SELECT_FILE"
            INS_INIT_TRANS -> "INIT_TRANS"
            INS_INTERNAL_AUTH -> "INTERNAL_AUTH"
            INS_EXTERNAL_AUTH -> "EXTERNAL_AUTH"
            INS_COMPLETE_TRANSACTION -> "COMPLETE_TRANSACTION"
            else -> "UNKNOWN"
        }
        status.value = "> Received ${toHeX(commandApdu)} ($commandType)\nResponding ${toHeX(responseApdu)}\n"
        if(ins == INS_COMPLETE_TRANSACTION && responseApdu != null){
            status.value = "Authentication complete."
            status.value = ""
            state = 0
        }
        sendResponseApdu(responseApdu ?: byteArrayOf())
        return null
    }

    override fun onDeactivated(reason: Int) {
        Log.d("Apdu Service", "Deactivated!")
    }
}