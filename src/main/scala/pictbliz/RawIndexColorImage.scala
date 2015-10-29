package pictbliz

import java.awt.image.{DataBuffer, IndexColorModel, BufferedImage}

import scala.annotation.tailrec
import scala.collection.mutable

import enrich.bufferedimage._
import enrich.packedcolor._

object RawIndexColorImage {

  final val UNUSED = 0xaaffffff // (alpha = 170) is not appear in IndexColoredImage. Must be 0 or 255.

  final val INIT_COLOR = 0xff000000 // 0xff000000 (non-alpha black)

  def fromBufferedImage(buf: BufferedImage): RawIndexColorImage = {
    val pix = buf.pixelsByte
    val cm = buf.indexColorModel
    val raw = RawIndexColorImage(Array.ofDim[Int](pix.length), Array.ofDim[Int](cm.getMapSize), buf.getWidth)

    val used = raw.writePixelsWithMarkUsed(pix)
    raw.writePalette(cm)
    raw.markUnusedPalette(used)
    raw
  }

  /* return empty RawIndexColorImage.
     'empty' means its pixels filled index 0, and palette filled UNUSED color.
   */
  def fromSize(pixelSize: Int, paletteSize: Int, width: Int): RawIndexColorImage = {
    RawIndexColorImage(Array.ofDim[Int](pixelSize), Array.fill(paletteSize)(UNUSED), width)
  }

}

case class RawIndexColorImage private (pixels: Array[Int], palette: Array[Int], width: Int) {
  import RawIndexColorImage._

  def drawImage(that: RawIndexColorImage, x: Int, y: Int): Unit = {
    val height = pixels.length / width

    // copying palette-0 (background color)
    if (palette(0) == UNUSED) palette(0) = that.palette(0)

    var i = 0
    while(i < that.pixels.length) {
      if (that.pixels(i) != 0) {
        val dx = i % that.width
        val dy = i / that.width
        val sx = x + dx
        val sy = y + dy
        // bounds checking
        if (0 <= sx && sx < width && 0 <= sy && sy < height) {
          //println(s"write: $sx, $sy")
          val idx = (sy * width) + sx
          setColor(idx, that.color(i))
        }
      }
      i += 1
    }
  }

  @inline final def color(idx: Int, index0AsAlpha: Boolean = false): Int = {
    val pIdx = pixels(idx)
    val res = palette(pIdx)
    if (index0AsAlpha && pIdx == 0) res & 0x00ffffff
    else res
  }

  // TODO: implement extending palette above 256!
  def setColor(idx: Int, color: Int): Unit = {
    findPalette(color) match {
      case Some(pNum) => pixels(idx) = pNum
      case None =>
        findPalette(UNUSED) match {
          case Some(eIdx) =>
            palette(eIdx) = color
            pixels(idx) = eIdx
          case None => sys.error(s"There is no unused palette idx. color: $color")
        }
    }
  }

  def clear(color: Int): Unit = {
    palette(0) = color
    foreachWithIndex(i => pixels(i) = 0)
  }
  
  def length = pixels.length

  def height = length / width

  def findPalette(targetColor: Int): Option[Int] = {
    val res = palette.indexOf(targetColor)
    if (res != -1) Some(res) else None
  }

  // overwrite palette from IndexColorModel.
  def writePalette(src: IndexColorModel): Unit = {
    require(src.getMapSize == palette.length)

    src.getRGBs(palette)
  }

  // overwrite (int)pixels from (byte)pixels.
  def writePixels(srcPixel: Array[Byte]): Unit = {
    var i = 0
    while(i < srcPixel.length) {
      val idx = srcPixel(i) & 0xff // byte -> Int
      pixels(i) = idx
      i += 1
    }
  }
  
  def writePixelsWithMarkUsed(srcPixel: Array[Byte], used: mutable.BitSet = mutable.BitSet.empty): mutable.BitSet = {
    var i = 0
    while(i < srcPixel.length) {
      val idx = srcPixel(i) & 0xff // byte -> Int
      pixels(i) = idx
      used += idx
      i += 1
    }
    used.result()
  }
  
  private def markUnusedPalette(): Unit = {
    val used = mutable.BitSet.empty
    var i = 0
    while(i < pixels.length) {
      val idx = pixels(i) & 0xff
      used += idx
      i += 1
    }
    markUnusedPalette(used)
  }

  private def markUnusedPalette(used: mutable.BitSet): Unit = {
    var i = 0
    while(i < palette.length) {
      if (!used(i) && i != 0) palette(i) = UNUSED
      i += 1
    }
  }

  // convert each color in palette to creating BufferedImage.
  private def restoreUnusedPalette(replaceColor: Int = INIT_COLOR): Unit = {
    var i = 0
    while(i < palette.length) {
      if (palette(i) == UNUSED) palette(i) = 0xff000000 | replaceColor
      else palette(i) = 0xff000000 | palette(i)
      i+=1
    }
  }

  def hasEmptyPalette: Boolean = palette.contains(UNUSED)

  def countEmptyPalette: Int = palette.count(_ == UNUSED)

  def countPalette: Int = palette.length - countEmptyPalette

  def toBufferedImage(replaceUnusedPaletteColor: Int = INIT_COLOR, transparent: Boolean = false): BufferedImage = {
    restoreUnusedPalette(replaceUnusedPaletteColor)

    val cm = new IndexColorModel(8, palette.length, palette, 0, transparent, -1,  DataBuffer.TYPE_BYTE)
    val buf = new BufferedImage(width, pixels.length / width, BufferedImage.TYPE_BYTE_INDEXED, cm)

    val pix = buf.pixelsByte
    var i = 0
    while(i < pix.length) {
      pix(i) = pixels(i).toByte
      i += 1
    }
    buf
  }

  @inline final def foreachWithIndex[A](f: Int => A): Unit = {
    var i = 0
    while(i < pixels.length) {
      f(i)
      i += 1
    }
  }

  @inline final def testAllPixel(that: RawIndexColorImage)(index0AsAlpha: Boolean = false)(withFilter: Int => Boolean)(pred: (Int, Int) => Boolean): Boolean = {
    @tailrec def rec(idx: Int = 0, ret: Boolean = true): Boolean = if (length <= idx) ret
    else {
      val cs = color(idx, index0AsAlpha)
      if (withFilter(cs)) {
        val co = that.color(idx, index0AsAlpha)
        if (!pred(cs, co)) {
          false
        } else rec(idx + 1, ret)
      } else rec(idx + 1, ret)
    }
    rec()
  }

}
