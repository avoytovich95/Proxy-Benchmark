import kotlin.collections.ArrayList
import java.io.FileOutputStream

/**
 * Created by Alex on 3/20/2018.
 */
class ImageBuilder(val type: String) {

    class Block(val aBlock: Byte, bBlock: Byte)

    private val list = ArrayList<ImageFrag>()
    private val blocks = ArrayList<Block>()
    private lateinit var bytes: ByteArray

    var file = ""

    fun add(img: ImageFrag) {
        list.add(img)
        blocks.add(Block(img.aBlock, img.bBlock))
    }

    fun exists(aBlock: Byte, bBlock: Byte): Boolean =
            blocks.contains(Block(aBlock, bBlock))

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

    fun isEnd(): Boolean = bytes.size < 512

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