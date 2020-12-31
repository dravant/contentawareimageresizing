package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Exception
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.sqrt

fun main(args: Array<String>) {
    try {
    if (args.indexOf("-in") < 0 || args.indexOf("-out") < 0) {
        throw Exception("Wrong input.")
    } else {
        val inFile: String = args[args.indexOf("-in") + 1]
        val outFile: String = args[args.indexOf("-out") + 1]
        val removeX: Int = args[args.indexOf("-width") + 1].toInt()
        val removeY: Int = args[args.indexOf("-height") + 1].toInt()
        resize(inFile,outFile, removeX, removeY)

    }
    } catch (e: Exception) {
        println(e.message)
    }
}


class Image(var inImage: BufferedImage, direction: String = "vertical") {

    var width = inImage.width
    var height = inImage.height
    private var pixels: MutableList<MutableList<Pixel>> = MutableList(width) {
        MutableList(height) { Pixel(0, 0, 0.0, null) } }
    var seam: MutableList<Pair<Int,Int>> = mutableListOf()
    init{
        if (direction == "horizontal")
        {
            transpose(inImage)


        }
        width = inImage.width
        height = inImage.height
        pixels= MutableList(width) {
            MutableList(height) { Pixel(0, 0, 0.0, null) }}

        for (j in 0 until height) {
            for (i in 0 until width) {
                pixels[i][j] = Pixel(i, j, calculateEnergy(i, j), null)// calculateEnergy(i,j), null)
            }
        }
        for (j in 0 until height) {
            for (i in 0 until width) {
                pixels[i][j].currentDistance = calculateCurrentDistance(i,j)
            }
        }
    }
    class Pixel(val x: Int, val y: Int, var energy: Double, var previous: Pixel? = null) {
        var currentDistance = Double.MAX_VALUE / 2000
    }

    private fun calculateDeltaX(x: Int, y: Int): Double {
        var deltaX2 = 0.0
        for (ch in listOf("r", "g", "b")) {
            deltaX2 += calculateDifX(x, y, ch).toDouble().pow(2.0)
        }
        return deltaX2
    }

    private fun calculateDeltaY(x: Int, y: Int): Double {
        var deltaY2 = 0.0
        for (ch in listOf("r", "g", "b")) {
            deltaY2 += calculateDifY(x, y, ch).toDouble().pow(2.0)
        }
        return deltaY2

    }

    private fun calculateDifX(x: Int, y: Int, rgb: String): Int {
        return getPixelColor(x + 1, y, rgb) - getPixelColor(x - 1, y, rgb)
    }

    private fun calculateDifY(x: Int, y: Int, rgb: String): Int {
        return getPixelColor(x, y + 1, rgb) - getPixelColor(x, y - 1, rgb)
    }

    private fun getPixelColor(x: Int, y: Int, clr: String): Int {
        val c = Color(inImage.getRGB(x, y))
        return when (clr) {
            "r" -> c.red
            "g" -> c.green
            "b" -> c.blue
            else -> 0
        }
    }

    private fun calculateEnergy(x: Int, y: Int): Double {
        val deltaX2: Double = when (x) {
            0 -> calculateDeltaX(1, y)
            width - 1 -> calculateDeltaX(width - 2, y)
            else -> calculateDeltaX(x, y)
        }
        val deltaY2: Double = when (y) {
            0 -> calculateDeltaY(x, 1)
            height - 1 -> calculateDeltaY(x, height - 2)
            else -> calculateDeltaY(x, y)
        }

        return sqrt(deltaX2 + deltaY2)
    }

    private fun calculateCurrentDistance(x: Int, y:Int): Double{
        return if (y == 0) {
            pixels[x][y].energy
        } else {
            var minEn = Double.MAX_VALUE/2.0
            for (i in x - 1..x + 1){
                if (i >= 0 && i <= width - 1 && y - 1 < height - 1){
                    if (pixels[i][y - 1].currentDistance < minEn) {
                        minEn = pixels[i][y - 1].currentDistance
                        pixels[x][y].previous = pixels[i][y - 1]
                    }
                }
            }
            minEn + pixels[x][y].energy
        }
    }

    fun removeSeam(direction: String = "vertical"): BufferedImage{
        var minEn: Double = Double.MAX_VALUE/2.0
        var minA = 0

        for (i in 0 until width) {
            if (pixels[i][height - 1].previous != null && pixels[i][height - 1].currentDistance < minEn) {
                minEn = pixels[i][height - 1].currentDistance
                minA = i
            }
        }
        drawPath(pixels[minA][height - 1])
        if (direction == "horizontal")
        {
            transpose(inImage)
        }
        return inImage
    }

    private fun transpose(image: BufferedImage){
        val tempImage = BufferedImage(height, width, BufferedImage.TYPE_INT_RGB)
        for (j in 0 until height) {
            for (i in 0 until width) {
                tempImage.setRGB(j,i, image.getRGB(i,j))
            }
        }
        inImage = tempImage
    }

    private fun drawPath(last: Pixel) {
        seam.add(Pair(last.x,last.y))
        if (last.previous != null && last.previous!!.y > - 1) {
            drawPath(last.previous!!)
        }
    }
}
fun removeOneSeam(inFile: String, direction: String) = removeOneSeam(ImageIO.read(File(inFile)), direction)
fun removeOneSeam(inFile: BufferedImage, direction: String): BufferedImage {

    val image = Image(inFile, direction)
    var temp = image.removeSeam(direction)
    if (direction == "horizontal"){
        for (coordinates in image.seam){
            for (y in coordinates.first until temp.height - 2){
                    temp.setRGB(coordinates.second, y , temp.getRGB(coordinates.second, y + 1))

            }
        }
        temp = temp.getSubimage(0,0,temp.width, temp.height - 1)
    }
    if (direction == "vertical"){
        for (coordinates in image.seam){
            for (x in coordinates.first until temp.width - 2){
                temp.setRGB(x, coordinates.second, temp.getRGB(x + 1, coordinates.second))
            }
        }
        temp = temp.getSubimage(0,0,temp.width - 1, temp.height)
    }

    return temp
}
fun resize(inFile: String, outFile: String, sizeX: Int, sizeY: Int){
    var tempImage = removeOneSeam(inFile, "init")
    repeat(sizeX) {
         tempImage = removeOneSeam(tempImage, direction = "vertical")
    }
    repeat(sizeY) {
        tempImage = removeOneSeam(tempImage, direction = "horizontal")

    }
    ImageIO.write(tempImage, "png", File(outFile))

}
