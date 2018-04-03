import kotlin.collections.ArrayList
import java.io.FileOutputStream

/**
 * Created by Alex on 3/20/2018.
 */
class ImageBuilder(val type: String) {

    private val list = ArrayList<ImageFrag>()
    private lateinit var bytes: ByteArray
    var file = ""

    fun add(img: ImageFrag) {
        list.add(img)
    }

    fun build() {
        list.sort()
        var bytes = ByteArray(0)
        for (frag in list) {
            bytes += frag.bytes
        }
        this.bytes = bytes
    }

    fun save(location: String, index: Int) {
        file = location + "savedImg$index.$type"
        FileOutputStream(file).use { fos ->
            fos.write(bytes)
        }
    }
}

class ImageFrag(val aBlock: Byte, val bBlock: Byte, val bytes: ByteArray): Comparable<ImageFrag> {

    override fun compareTo(other: ImageFrag) = when {
        aBlock < other.aBlock -> -1
        aBlock > other.aBlock -> 1
        else -> when {
            bBlock < other.bBlock -> -1
            bBlock > other.bBlock -> 1
            else -> 0
        }
    }
}