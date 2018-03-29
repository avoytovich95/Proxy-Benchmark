import java.io.File
import java.net.*
import java.util.*
import kotlin.concurrent.thread
import javax.swing.JFrame
import javax.swing.text.StyleConstants.setIcon
import javax.swing.JLabel
import java.awt.FlowLayout
import javax.swing.ImageIcon
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.IOException



private var arg0 = ""
private var arg1 = ""
private var arg2 = ""

private val location = System.getProperty("user.dir") + File.separator
private var type = ""
private var file = ""

private var imgIndex = 0

/**
 * Created by Alex on 3/8/2018.
 *
 * Arguments are
 * arg0: ipv4 | ipv6
 * arg1: stop | slide
 * arg2: drop | keep
 *
 * https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/16777216colors.png/220px-16777216colors.png
 * https://www.hdwallpapers.in/walls/lg_g3_smoke_colors-wide.jpg
 * https://wallpapersite.com/images/wallpapers/cars-3-5120x2880-2017-pixar-animation-4k-8k-6461.jpg
 */
fun main(args: Array<String>) {
    if (args.size != 3) {
        println("Incorrect number of arguments!")
        println("Exiting")
        System.exit(0)
    } else {
        println("Running with: ${args[0]}, ${args[1]}, ${args[2]}\n")
        arg0 = args[0]; arg1 = args[1]; arg2 = args[2]
        if (!Data.checkArgs(arg0, arg1, arg2)){
            println("Incorrect arguments given")
            System.exit(0)
        }
    }

    print("Enter url: ")
    val url = readLine()
    type = url!!.trim()
    type = url.substring(url.length - 4)

    if (arg1 == "slide")
        receiveSlide(url)
    else if (arg1 == "stop")
        connectStop(url)
    displayImage()
}

private fun connectStop(url: String) {
    val socket = DatagramSocket()
    val ip = if (arg0 == "ipv6") InetAddress.getByName(Data.address6)
    else InetAddress.getByName(Data.address4)
    println(ip)

    var inBytes: ByteArray
    var outBytes: ByteArray
    var inPacket: DatagramPacket
    var outPacket: DatagramPacket
    var frag: ImageFrag
    val img = ImageBuilder(type)

    socket.run {
        outBytes = Data.RRQ(url)
        outPacket = DatagramPacket(outBytes, outBytes.size, ip, Data.port)

        send(outPacket)
        while (true) {
            inBytes = ByteArray(512 + 4)
            inPacket = DatagramPacket(inBytes, inBytes.size)

            soTimeout = 5000
            try {
                receive(inPacket)
                soTimeout = 500
                inBytes = inPacket.data
                if (Data.getOp(inBytes) == 3.toByte()) {
                    frag = Data.getDataFrag(inBytes)
                    if (arg2 == "drop" && chance()) {
                        Thread.sleep(500)
                        throw SocketTimeoutException("Packet Dropped")
                    }else {
                        print("Received ${frag.aBlock}:${frag.bBlock}... ")
                        outBytes = ByteArray(128)
                        outBytes = Data.ACK(frag.aBlock, frag.bBlock)
                        outPacket = DatagramPacket(outBytes, outBytes.size, ip, Data.port)
                        img.add(frag)
                        send(outPacket)
                        println("Ack sent")
                    }

                    if (frag.isEnd()) break

                } else if (Data.getOp(inBytes) == 5.toByte()) {
                    throw Exception("Error Code: ")
                }
                if (inBytes[0] == 1.toByte()){
                    println("End of transmission")
                    break
                }
            }catch (s: SocketTimeoutException) {
                println(s)
            }catch (e: Exception) {
                e.printStackTrace()
                println(Data.getError(inBytes))
                close()
                return
            }
        }
        close()
    }
    println("Building file")
    img.build()
    println("Saving file")
    img.save(location, imgIndex++)
    println("File saved")
    file = img.file
}

private fun receiveSlide(url: String) {    val socket = DatagramSocket()
    val ip = if (arg0 == "ipv6") InetAddress.getByName(Data.address6)
    else InetAddress.getByName(Data.address4)
    println(ip)

    var inBytes = ByteArray(512 + 4)
    var outBytes: ByteArray
    var inPacket: DatagramPacket
    var outPacket: DatagramPacket
    var frag: ImageFrag
    val img = ImageBuilder(type)

    socket.run {
        outBytes = Data.RRQ(url)
        outPacket = DatagramPacket(outBytes, outBytes.size, ip, Data.port)

        try {
            send(outPacket)

            var latest = ByteArray(2)
            val expected = ByteArray(2)

            while (true) {
                soTimeout = 5000
                for (i in 0 until Data.window) {
                    inBytes = ByteArray(512 + 4)
                    inPacket = DatagramPacket(inBytes, inBytes.size)
                    try {
                        receive(inPacket)
                        soTimeout = 500
                        inBytes = inPacket.data
                        frag = ImageFrag(Data.getBlockA(inBytes), Data.getBlockB(inBytes), Data.getData(inBytes))

                        if (Data.getOp(inBytes) == 5.toByte()) throw Exception("Error Code: ")

//                        if (arg2 == "drop" && chance()) {
//                            throw SocketTimeoutException("Packet Dropped")
//                        } else
                        if (Data.checkBlock(frag.aBlock, frag.bBlock, expected)) {
                            println("Received ${frag.aBlock}:${frag.bBlock}")
                            img.add(frag)
                            latest = byteArrayOf(frag.aBlock, frag.bBlock)
                            if (expected[1] == 127.toByte()) {
                                expected[0]++; expected[1] = 0
                            } else expected[1]++
                        }
                        if (inBytes[0] == 1.toByte()) break
                    }catch (s: SocketTimeoutException) { println(s) }

                }
                outBytes = ByteArray(128)
                outBytes = Data.ACK(latest[0], latest[1])
                outPacket = DatagramPacket(outBytes, outBytes.size, ip, Data.port)
                println("Sending ack for ${latest[0]}:${latest[1]}")
                send(outPacket)

                if (inBytes[0] == 1.toByte()) {
                    println("End of transmission")
                    break
                }
            }
        }catch (e: Exception) {
            e.printStackTrace()
            println(Data.getError(inBytes))
            close()
            return
        }
        close()
    }
    println("Building file")
    img.build()
    println("Saving file")
    img.save(location, imgIndex++)
    println("File saved")
    file = img.file
}

private fun chance(): Boolean =
    (1..100).random() == 1

private fun ClosedRange<Int>.random() =
        Random().nextInt(endInclusive - start) +  start

fun displayImage() {
    val img = ImageIO.read(File(file))
    val icon = ImageIcon(img)
    val frame = JFrame()
    val lbl = JLabel()
    lbl.icon = icon
    frame.run {
        layout = FlowLayout()
        setSize(img.width, img.height)
        add(lbl)
        isVisible = true
        toFront()
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    }
}