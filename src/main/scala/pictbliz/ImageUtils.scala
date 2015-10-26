package pictbliz

import java.awt.image._

import enrich.bufferedimage._

object ImageUtils {

  object ARGB {
    def unapply(c: Int): Option[(Int, Int, Int, Int)] = {
      val B = c & 0xFF
      val G = (c >> 8) & 0xFF
      val R = (c >> 16) & 0xFF
      val A = (c >> 24) & 0xFF
      Some((A, R, G, B))
    }
  }

  def mul(color: Int, amp: Double): Int = {
    if (amp < 1) {
      val a = color & 0xFF000000 >>> 24
      val r = color & 0xFF0000 >>> 16
      val g = color & 0xFF00 >>> 8
      val b = color & 0xFF
      val aa = a*amp; val ra = r*amp; val ga = g*amp; val ba = b*amp
      (aa.toInt << 24) | (ra.toInt << 16) | (ga.toInt << 8) | ba.toInt
    } else color
  }

  def add(c1: Int, c2: Int): Int = {
    val a1 = c1 & 0xFF000000 >>> 24; val a2 = c2 & 0xFF000000 >>> 24
    val r1 = c1 & 0xFF0000 >>> 16;   val r2 = c2 & 0xFF0000 >>> 16
    val g1 = c1 & 0xFF00 >>> 8;      val g2 = c2 & 0xFF00 >>> 8
    val b1 = c1 & 0xFF;              val b2 = c2 & 0xFF
    ((a1+a2) & 0xFF << 24) | ((r1+r2) & 0xFF << 16) | ((g1+g2) & 0xFF << 8) | ((b1+b2) & 0xFF)
  }

  def alpha(color: Int, value: Int): Int = (color & 0x00FFFFFF) | (value << 24)

  /**
   * 与えられたArrayの8近傍を取り、新しいArrayに入れて返す。
   * 配列の範囲外は引数として与えたdefaultの値を取る。
   *
   * @param arr 8近傍を調べるArray(1次元)
   * @param i 調べる位置のインデックス
   * @param w Arrayの横幅
   * @param default 配列の範囲外を表す値
   * @return 8近傍の値が入ったArray
   */
  def neighbor(arr: Array[Int], i: Int, w: Int, default: Int): Array[Int] = {
    // nante hidoi code nanda...
    val a = Array(0, 0, 0, 0, 0, 0, 0, 0)
    val u = i < w
    val b = (arr.length - i) <= w
    val r = (i+1)%w == 0
    val l = i%w == 0
    a(0) = if (u || l) -1 else i-1-w
    a(1) = if (u) -1 else i-w
    a(2) = if (u || r) -1 else i+1-w
    a(3) = if (l) -1 else i-1
    a(4) = if (r) -1 else i+1
    a(5) = if (b || l) -1 else i-1+w
    a(6) = if (b) -1 else i+w
    a(7) = if (b || r) -1 else i+1+w

    var j = 0; val len = arr.length
    while(j < 8) {
      a(j) = if (0 <= a(j) && a(j) < len) arr(a(j)) else default
      j += 1
    }
    a
    /*a.map { i=>
      if (0 <= i && i < arr.length) arr(i) else 0
    }*/
  }

  def newImage(w: Int, h: Int, typ: Int = BufferedImage.TYPE_BYTE_INDEXED): BufferedImage = new BufferedImage(w, h, typ)
  def newImage(size: (Int, Int)): BufferedImage = newImage(size._1, size._2)
  
  def pile(src: BufferedImage, targets: BufferedImage*): BufferedImage = ???
  
  def sameSizeImage(src: BufferedImage): BufferedImage = 
    new BufferedImage(src.getWidth, src.getHeight, src.getType)

  def extraSizeImage(src: BufferedImage, plus: Int): BufferedImage =
    new BufferedImage(src.getWidth+plus*2, src.getHeight+plus*2, src.getType)
  
  def copy(src: BufferedImage): BufferedImage = {
    val dest = sameSizeImage(src)
    dest.setData(src.getData)
    dest
  }
  
  def toARGBImage(src: BufferedImage): BufferedImage = {
    if (src.getType != BufferedImage.TYPE_BYTE_INDEXED)
      return src
    
    val dest = newImage(src.getWidth, src.getHeight)
    val srcPixel = src.pixelsByte
    val destPixel = dest.pixelsInt
    val cm = src.getColorModel
    srcPixel.indices foreach { i =>
      destPixel(i) = 0xff000000 | 0x00ffffff & cm.getRGB(srcPixel(i))
    }
    dest
  }
  
  def enableAlpha(src: BufferedImage, transColor: Int): BufferedImage = {
    val srcPixel = src.pixelsInt

    srcPixel.indices foreach { i =>
      if (srcPixel(i) == transColor)
        srcPixel(i) = 0x0
    }
    src
  }

  def enableAlphaIndexColor(src: BufferedImage, paletteIdx: Int = 0): BufferedImage = {
    import enrich.packedcolor._

    val raw = RawIndexColorImage.fromBufferedImage(src)

    if (raw.palette(paletteIdx).a == 0xff) {
      raw.palette(paletteIdx) = raw.palette(paletteIdx) & 0x00ffffff
    }
    raw.toBufferedImage(src.getWidth)
  }

  def synthesisIndexColor(src: BufferedImage, target: BufferedImage, maskcolor: Int = 0xFFFFFFFF): BufferedImage = {
    require(src.getType == BufferedImage.TYPE_BYTE_INDEXED && target.getType == BufferedImage.TYPE_BYTE_INDEXED,
      "source & target's image type should be 'TYPE_BYTE_INDEXED'")

    val rawTarget = RawIndexColorImage.fromBufferedImage(target)
    val srcCM = src.indexColorModel

    val dest = src.createRawIndexColorImage

    val used = dest.writePixelsWithMarkUsed(src)
    dest.writePalette(srcCM)
    dest.markUnusedPalette(used)

    // synthesis
    dest.foreachWithIndex { i =>
      if (dest.color(i) == maskcolor) {
        val targetColor = rawTarget.color(i)
        dest.setColor(i, targetColor)
      }
    }

    dest.restoreUnusedPalette()
    dest.toBufferedImage(src.getWidth)
  }
  
  def synthesis(src: BufferedImage, target: BufferedImage, maskcolor: Int = 0xFFFFFFFF): BufferedImage = {
    require(src.getType == BufferedImage.TYPE_INT_ARGB && target.getType == BufferedImage.TYPE_INT_ARGB,
        "source & target's image type should be 'TYPE_INT_ARGB'")

    val srcPixel    = src.pixelsInt
    val targetPixel = target.pixelsInt

    for {
      idy <- 0 until src.getHeight
      dy = idy * src.getWidth
      idx <- 0 until src.getWidth
      i = dy + idx
      if srcPixel(i) == maskcolor
    } {
      srcPixel(i) = targetPixel(i)
    }
    
    src
  }
}
