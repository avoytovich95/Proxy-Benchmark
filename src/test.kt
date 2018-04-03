import java.net.InetAddress
import java.net.URL
import java.util.*

/**
 * Created by Alex on 3/15/2018.
 */
fun main(args: Array<String>) {
    val url = URL("https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/16777216colors.png/220px-16777216colors.png   ")
    val file = url.file
    println(file)
    println(file.substring(file.lastIndexOf('/') + 1))
    println(file.substring(file.length - 3))
    println(file.substring(file.lastIndexOf('/') + 1, file.length - 4))
}

private fun chance(): Boolean =
        (1..100).random() == 1


private fun ClosedRange<Int>.random() =
        Random().nextInt(endInclusive - start) +  start