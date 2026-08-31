// CursorEditor.kt
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class CursorEditor {
    @Parameter(names = ["--new"])
    private var newFlag: Boolean = false

    @Parameter(names = ["--size"])
    private var size: Int = 32

    @Parameter(names = ["--load"])
    private var loadFile: String? = null

    @Parameter(names = ["--save"])
    private var saveFile: String? = null

    @Parameter(names = ["--export"])
    private var exportFile: String? = null

    @Parameter(names = ["--hotspot"])
    private var hotspot: String? = null

    @Parameter(names = ["--color"])
    private var color: String? = null

    @Parameter(names = ["--pixel"])
    private var pixel: String? = null

    @Parameter(names = ["--fill"])
    private var fill: String? = null

    @Parameter(names = ["--preview"])
    private var preview: Boolean = false

    data class EditorData(val size: Int, val pixels: Array<IntArray>, val hotspot: IntArray, val palette: IntArray)

    private lateinit var pixels: Array<IntArray>
    private var currentColor = 1
    private var hotspotPos = intArrayOf(0, 0)
    private val palette = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
    private val colorMap = mapOf(
        0 to intArrayOf(255, 255, 255, 0),
        1 to intArrayOf(0, 0, 0, 255),
        2 to intArrayOf(255, 255, 255, 255),
        3 to intArrayOf(255, 0, 0, 255),
        4 to intArrayOf(0, 255, 0, 255),
        5 to intArrayOf(0, 0, 255, 255),
        6 to intArrayOf(255, 255, 0, 255),
        7 to intArrayOf(255, 0, 255, 255),
        8 to intArrayOf(0, 255, 255, 255)
    )

    private fun initPixels() {
        pixels = Array(size) { IntArray(size) }
    }

    private fun setPixel(x: Int, y: Int, colorIdx: Int) {
        if (x in 0 until size && y in 0 until size) {
            pixels[y][x] = colorIdx
        }
    }

    private fun getPixel(x: Int, y: Int): Int {
        return if (x in 0 until size && y in 0 until size) pixels[y][x] else 0
    }

    private fun fill(x: Int, y: Int, colorIdx: Int) {
        val target = getPixel(x, y)
        if (target == colorIdx) return
        val stack = mutableListOf(Pair(x, y))
        val visited = mutableSetOf<Pair<Int, Int>>()
        while (stack.isNotEmpty()) {
            val (cx, cy) = stack.removeAt(stack.size - 1)
            if (visited.contains(Pair(cx, cy))) continue
            visited.add(Pair(cx, cy))
            if (getPixel(cx, cy) == target) {
                setPixel(cx, cy, colorIdx)
                for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    val nx = cx + dx
                    val ny = cy + dy
                    if (nx in 0 until size && ny in 0 until size) {
                        stack.add(Pair(nx, ny))
                    }
                }
            }
        }
    }

    private fun toImage(): BufferedImage {
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val c = colorMap[pixels[y][x]] ?: intArrayOf(0, 0, 0, 0)
                img.setRGB(x, y, Color(c[0], c[1], c[2], c[3]).rgb)
            }
        }
        return img
    }

    private fun exportCur(filename: String) {
        val img = toImage()
        val pngName = filename.replace(".cur", ".png")
        ImageIO.write(img, "png", File(pngName))
        println("\u001B[32mЭкспортировано в $filename (как PNG)\u001B[0m")
    }

    private fun toData(): EditorData {
        return EditorData(size, pixels, hotspotPos, palette)
    }

    private fun fromData(data: EditorData) {
        this.size = data.size
        this.pixels = data.pixels
        this.hotspotPos = data.hotspot
    }

    private fun preview() {
        println("\u001B[36mПредпросмотр курсора (${size}x${size}), hotspot: (${hotspotPos[0]},${hotspotPos[1]})\u001B[0m")
        val chars = arrayOf("·", "█", "▓", "▒", "░", " ")
        for (y in 0 until size) {
            val line = StringBuilder()
            for (x in 0 until size) {
                val idx = pixels[y][x]
                line.append(if (idx < chars.size) chars[idx] else " ")
            }
            println(line)
        }
    }

    fun run() {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val type = object : TypeToken<EditorData>() {}.type

        if (loadFile != null) {
            val json = File(loadFile).readText()
            val data = gson.fromJson<EditorData>(json, type)
            fromData(data)
        } else if (newFlag || size > 0) {
            initPixels()
        } else {
            size = 32
            initPixels()
        }

        hotspot?.let {
            val parts = it.split(",")
            if (parts.size == 2) {
                hotspotPos[0] = parts[0].toInt()
                hotspotPos[1] = parts[1].toInt()
            }
        }

        color?.let {
            val colorMap = mapOf(
                "#000000" to 1, "#FFFFFF" to 2, "#FF0000" to 3,
                "#00FF00" to 4, "#0000FF" to 5, "#FFFF00" to 6,
                "#FF00FF" to 7, "#00FFFF" to 8
            )
            currentColor = colorMap[it.uppercase()] ?: 1
        }

        pixel?.let {
            val parts = it.split(",")
            if (parts.size == 2) {
                setPixel(parts[0].toInt(), parts[1].toInt(), currentColor)
            }
        }

        fill?.let {
            val parts = it.split(",")
            if (parts.size == 2) {
                fill(parts[0].toInt(), parts[1].toInt(), currentColor)
            }
        }

        saveFile?.let {
            val json = gson.toJson(toData())
            File(it).writeText(json)
            println("\u001B[32mПроект сохранён в $it\u001B[0m")
        }

        exportFile?.let {
            exportCur(it)
        }

        if (preview) {
            preview()
        }

        if (!newFlag && loadFile == null && saveFile == null && exportFile == null && !preview && pixel == null && fill == null) {
            println("Используйте --help для справки.")
        }
    }
}

fun main(args: Array<String>) {
    val editor = CursorEditor()
    JCommander.newBuilder().addObject(editor).build().parse(*args)
    editor.run()
}
