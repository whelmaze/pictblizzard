package com.github.whelmaze.pictbliz

import scriptops.{Attrs, AttrMap}
import scriptops.Attrs._
import java.awt.image.BufferedImage
import scriptops.Attrs.AttrMap


object ImageBuilders {

  implicit val StrBuilder = new Buildable[Str] {
    def build(self: Str, attrmap: AttrMap) = {
      val strgraphics = StrGraphics.build(self.s, attrmap)
      val result = strgraphics.processImage()
      strgraphics.dispose()
      ResultImage(findBeginPoint(attrmap), result)
    }
  }

  implicit val IconBuilder = new Buildable[Icon] {
    def build(self: Icon, attrmap: AttrMap) = {
      val icon: BufferedImage = ext.PNG.read(self.uri)
      ResultImage(findBeginPoint(attrmap), icon)
    }
  }

  implicit val FaceGraphicBuilder = new Buildable[FaceGraphic] {
    def build(self: FaceGraphic, attrmap: AttrMap) = {
      sys.error("not implemented")
    }
  }

  implicit val WindowBuilder = new Buildable[AWindow] {
    def build(self: AWindow, attrmap: AttrMap) = {
      val sysg = SystemGraphics.fromPath(self.systemGraphicsPath)
      val ASize(w, h) = attrmap('size)
      val buf = ImageUtils.newImage(w, h)

      val syswin = sysg.getSystemWindow(w, h, zoom=true)
      val g = buf.createGraphics
      g.drawImage(syswin, null, 0, 0)
      g.dispose()
      ResultImage((0, 0), buf)
    }
  }

  implicit val TileBuilder = new Buildable[ATile] {
    def build(self: ATile, attrmap: AttrMap) = {
      sys.error("not implemented")
    }
  }

  implicit val BackgroundBuilder = new Buildable[ABackground] {
    def build(self: ABackground, attrmap: AttrMap) = {
      sys.error("not implemented")
    }
  }

  //util
  def findBeginPoint(attrmap: AttrMap): (Int, Int) = {
    AttrMap.findParam(attrmap, 'point, 'rect) map {
      case APoint(x, y)      => (x, y)
      case ARect(x, y, _, _) => (x, y)
    } getOrElse (0, 0)
  }
}

trait Buildable[T <: Drawable] {
  def build(self: T, attrmap: AttrMap): ResultImage
}

// 描画位置と描画するImageを持つ
case class ResultImage(pos: (Int, Int), img: BufferedImage)

object ImageBuilder {
  import ImageBuilders._

  final val emptyResult: ResultImage = ResultImage((0, 0), ImageUtils.newImage(1, 1))

  def build(a: Drawable, m: AttrMap): Option[ResultImage] = {
    Option(a match {
      case s: Str => buildImpl(s, m)
      case i: Icon => buildImpl(i, m)
      case f: FaceGraphic => buildImpl(f, m)
      case b: ABackground => buildImpl(b, m)
      case t: ATile => buildImpl(t, m)
      case w: AWindow => buildImpl(w, m)
      case NullValue => emptyResult
    })
  }

  private def buildImpl[T <: Drawable : Buildable](t: T, attrmap: AttrMap): ResultImage = {
    implicitly[Buildable[T]].build(t, attrmap)
  }

}

