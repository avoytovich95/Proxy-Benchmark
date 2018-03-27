/**
 * Created by Alex on 3/6/2018.
 */
object Data {
    const val port = 2698

//    const val address4 = "127.0.0.1" //Local ip
//    const val address6 = "127.0.0.1"

    const val address4 = "129.3.20.26" //pi cs server
    const val address6 = "fe80::225:90ff:fe4d:f030"

    const val window = 10

    private val errors = arrayOf("Bad link provided")

    fun checkArgs(arg0: String, arg1: String, arg2: String): Boolean {
        if (arg0 != "ipv4" && arg0 != "ipv6") return false
        if (arg1 != "stop" && arg1 != "slide") return false
        if (arg2 != "drop" && arg2 != "keep") return false
        return true
    }

    fun RRQ(file: String): ByteArray = request(1, file)
    fun WRQ(file: String): ByteArray = request(2, file)
    fun DATA(aBlock: Byte, bBlock: Byte , data: ByteArray): ByteArray = addAll(byteArrayOf(0, 3, aBlock, bBlock), data)
    fun ACK(aBlock: Byte, bBlock: Byte): ByteArray = byteArrayOf(0, 4, aBlock, bBlock)
    fun ERR(op: Int): ByteArray = addAll(byteArrayOf(0, 5, 0, op.toByte()), errors[op-1].toByteArray(), byteArrayOf(0))

    fun getRequestFile(tftp: ByteArray): String {
        val bytes = ByteArray(tftp.size); bytes.fill(32)
        for (i in 2 until tftp.size)
            if (tftp[i] != (0).toByte()) bytes[i-2] = tftp[i]
        return String(bytes)
    }

    fun checkBlock(aBlock: Byte, bBlock: Byte, block: ByteArray): Boolean = aBlock == block[0] && bBlock == block[1]
    fun getBlock(tftp: ByteArray): ByteArray = byteArrayOf(tftp[2], tftp[3])
    fun getBlockA(tftp: ByteArray): Byte = tftp[2]
    fun getBlockB(tftp: ByteArray): Byte = tftp[3]
    fun blockToInt(block: ByteArray): Int = (block[0] * 128) + block[1]
    fun intToBlock(int: Int): ByteArray {
        val block = ByteArray(2)
        block[0] = (int / 128).toByte()
        block[1] = (int % 128).toByte()
        return block
    }
    fun getDataFrag(tftp: ByteArray): ImageFrag{
        val frag = ImageFrag(tftp[2], tftp[3], ByteArray(512))

        var last = tftp.size-1
        for (i in tftp.size-1 downTo 4)
            if (tftp[i] == 0.toByte()) last--

        for (i in 4 until tftp.size)
            frag.bytes[i-4] = tftp[i]
        return frag
    }
    fun getData(tftp: ByteArray): ByteArray {
        val bytes = ByteArray(512)
        for (i in 4 until tftp.size) {
            bytes[i-4] = tftp[i]
        }
        return bytes
    }


    fun getError(tftp: ByteArray): String {
        val bytes = ByteArray(tftp.size); bytes.fill(32)
        for (i in 4 until tftp.size)
            if (tftp[i] != 0.toByte()) bytes[i-2] = tftp[i]
        return "${tftp[2]}${tftp[3]}: ${String(bytes)}"
    }

    fun getOp(tftp: ByteArray): Byte = tftp[1]

    private fun request(op: Int, file: String): ByteArray {
        val opt = byteArrayOf(0, op.toByte())
        val fileByte = file.toByteArray()
        val end = byteArrayOf(0)
        return addAll(opt, fileByte, end)
    }

    private fun addAll(vararg bytes: ByteArray): ByteArray {
        var array = ByteArray(0)
        for (b in bytes)
            array += b
        return array
    }
}