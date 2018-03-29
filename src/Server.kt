import java.io.*
import java.lang.Exception
import java.net.*
import java.nio.file.*
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.ArrayList

private var client: InetAddress? = null
private var clientPort: Int? = null
private val location = System.getProperty("user.dir") + File.separator
private var type = ""
private var name = ""

private var arg0 = ""
private var arg1 = ""
private var arg2 = ""

private var indexA: Byte = 0
private var indexB: Byte = 0

/**
 * Created by Alex on 3/13/2018.
 *
 * Arguments are
 * arg0: ipv4 | ipv6
 * arg1: stop | slide
 * arg2: drop | keep
 */
fun main(args: Array<String>) {
    if (args.size != 3) {
        println("Incorrect number of arguments!")
        println("Exiting")
        System.exit(0)
    } else {
        println("Running with: ${args[0]}, ${args[1]}, ${args[2]}")
        arg0 = args[0]; arg1 = args[1]; arg2 = args[2]
        if (!Data.checkArgs(arg0, arg1, arg2)) {
            println("Incorrect arguments given!")
            System.exit(0)
        }
    }

    if (arg1 == "stop") {
        println("Running stop and wait\n")
        connectStop()
    } else if (arg1 == "slide") {
        println("Running sliding window")
        sendSlide()
    }
}

private fun connectStop() {
    val socket = DatagramSocket(Data.port)

    var inBytes = ByteArray(128)
    var outBytes: ByteArray
    var inPacket: DatagramPacket
    var outPacket: DatagramPacket

    var queue: Queue<ByteArray>

    socket.run {
        inPacket = DatagramPacket(inBytes, inBytes.size)
        socket.receive(inPacket)
        client = inPacket.address
        clientPort = inPacket.port
        val link = Data.getRequestFile(inPacket.data)
        try {
            setFile(link.trim())
            if (!existImg()) {
                saveImg(link.trim())
                println("Image downloaded")
            }
        }catch (e: Exception) {
            println("Bad link")
            outBytes = Data.ERR(1)
            outPacket = DatagramPacket(outBytes, outBytes.size, client, clientPort!!)
            send(outPacket)
            System.exit(0)
        }
        queue = splitImg(getImg()!!)

        outBytes = Data.DATA(indexA, indexB, queue.poll())
        while(true) {
            soTimeout = 1000
            try {
                inBytes = ByteArray(128)
                outPacket = DatagramPacket(outBytes, outBytes.size, client, clientPort!!)
                inPacket = DatagramPacket(inBytes, inBytes.size)

                if (arg2 == "drop" && chance())
                    print("Dropping $indexA:$indexB")
                else {
                    print("Sending $indexA:$indexB ")
                    send(outPacket)
                }

                receive(inPacket)

                if (Data.checkBlock(indexA, indexB, Data.getBlock(inPacket.data))) {
                    print("Ack received")

                    if (indexB == 127.toByte()) {
                        indexA++; indexB = 0
                    }else indexB++

                    if (!queue.isEmpty()) {
                        println(", sending next packet")
                        outBytes = ByteArray(512 + 4)
                        outBytes = Data.DATA(indexA, indexB, queue.poll())
                        if (queue.isEmpty())
                            outBytes[0] = 1
                    } else { println(); break; }

                }else
                    println("Bad ack, sending again")
            } catch (s: SocketTimeoutException) {
                println("No ack received, sending again")
            }
        }
        println("End of queue")

        close()
    }
    indexA = 0
    indexB = 0
}

private fun sendSlide() {
    val socket = DatagramSocket(Data.port)

    var inBytes = ByteArray(128)
    var outBytes: ByteArray
    var inPacket: DatagramPacket
    var outPacket: DatagramPacket

    var list: ArrayList<ByteArray>

    socket.run {
        inPacket = DatagramPacket(inBytes, inBytes.size)
        socket.receive(inPacket)
        client = inPacket.address
        clientPort = inPacket.port
        val link = Data.getRequestFile(inPacket.data)
        try {
            setFile(link.trim())
            if (!existImg()) {
                saveImg(link.trim())
                println("Image downloaded")
            }
        } catch (e: Exception) {
            println("Bad link")
            outBytes = Data.ERR(1)
            outPacket = DatagramPacket(outBytes, outBytes.size, client, clientPort!!)
            send(outPacket)
            System.exit(0)
        }

        list = imgList(getImg()!!)

        var first = 0
        var last = first + Data.window
        var block: ByteArray
        var blockIndex: Int
        var b: ByteArray

        while (true) {
            soTimeout = 1000
            try {
                inBytes = ByteArray(128)
                inPacket = DatagramPacket(inBytes, inBytes.size)
                for (i in first until last) {
                    outBytes = ByteArray(512 + 4)
                    outBytes = Data.DATA(indexA, indexB, list[i])
                    if (i == list.size - 1)
                        outBytes[0] = 1
                    outPacket = DatagramPacket(outBytes, outBytes.size, client, clientPort!!)

                    if (arg2 == "drop" && chance())
                        println("Dropping $indexA:$indexB ")
                    else {
                        println("Sending $indexA:$indexB ")
                        send(outPacket)
                    }

                    if (indexB == 127.toByte()) {
                        indexA++; indexB = 0
                    }else indexB++
                }
                receive(inPacket)
                block = Data.getBlock(inPacket.data)
                blockIndex = Data.blockToInt(block)

                if (blockIndex in first-1..last) {
                    println("Ack received for ${block[0]}:${block[1]}")
                    if (first == list.size - 1)
                        break
                    if (blockIndex == list.size-1) break

                    first = blockIndex+1
                    last = if (first + Data.window >= list.size) list.size
                    else first + Data.window
                }else {
                    println("Incorrect ack, sending again")
                }
                b = Data.intToBlock(first)
                indexA = b[0]
                indexB = b[1]

            }catch (s: SocketTimeoutException) {
                println("No ack received, sending again")
            }
        }
        println("End of list")

        close()
    }
    indexA = 0
    indexB = 0
}

private fun chance(): Boolean =
        (1..100).random() == 1

private fun ClosedRange<Int>.random() =
        Random().nextInt(endInclusive - start) +  start

private fun splitImg(bytes: ByteArray): Queue<ByteArray> {
    var index = 0
    val queue: Queue<ByteArray> = LinkedList<ByteArray>()
    var qbyte = ByteArray(512)
    qbyte.fill(0)
    for (i in 0 until bytes.size) {
        qbyte[index++] = bytes[i]
        if (index == 512) {
            index = 0
            queue.add(qbyte.clone())
            qbyte = if (bytes.size - i > 512)
                ByteArray(512)
            else ByteArray(bytes.size - i - 1)
        }
    }
    println(queue.size)
    queue.add(qbyte)
    return queue
}

private fun imgList(bytes: ByteArray): ArrayList<ByteArray> {
    var index = 0
    val list = ArrayList<ByteArray>()
    var lbyte = ByteArray(512)
    lbyte.fill(0)
    for (i in 0 until bytes.size) {
        lbyte[index++] = bytes[i]
        if (index == 512) {
            index = 0
            list.add(lbyte.clone())
            lbyte = ByteArray(512)
        }
    }
    list.add(lbyte)
    return list
}

private fun saveImg(link: String) {
    println("Downloading image from: $link")
    val img = ImageIO.read(URL(link))
    val out = FileOutputStream("$location$name.$type")
    ImageIO.write(img, type, out)
    out.close()
}

private fun getImg(): ByteArray? {
    val path = Paths.get("$location$name.$type")
    return Files.readAllBytes(path)
}

private fun existImg(): Boolean {
    val path = Paths.get("$location$name.$type")
    return Files.exists(path)
}

private fun setFile(url: String) {
    val file = URL(url).file
    name = file.substring(file.lastIndexOf('/') + 1, file.length - 4)
    type = file.substring(file.length - 3)
}
