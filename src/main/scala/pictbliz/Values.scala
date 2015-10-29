package pictbliz

import java.awt.image.BufferedImage
import scalaz.syntax.semigroup._

import pictbliz.ext.PNG
import enrich.bufferedimage._

object Values {
  import Params._

  type Renderer = Params => ImagePart

  trait Value {
    def render(params: Params): ImagePart
  }

  // This is Value but must be convert other Values while interpolation.
  abstract class ConvertibleValue[T <: Value] extends Value {
    def render(params: Params): ImagePart = sys.error("huh?")
  }

  case class Text(str: String) extends Value {

    def render(params: Params): ImagePart = {
      val front = renderFront(params)
      val updatedP = autoExpandSizeWith(front).apply(params)
      val back = renderBackground(updatedP)

      back |+| front
    }

    def renderFront: Renderer = { params =>
      val strGraphics = StrGraphics.build(str, params)
      val res = strGraphics.processImage()
      strGraphics.dispose()

      ImagePart(Images.findBeginPoint(params, res.getWidth, res.getHeight), res)
    }

    def renderBackground: Renderer = { params =>
      val background = for (win <- params.window) yield {
        win.toValue.render(params)
      }
      background getOrElse Images.emptyPart
    }
  }

  case class Icon(path: String) extends Value {
    def render(params: Params): ImagePart = {
      val res: BufferedImage = PNG.read(path)
      ImagePart(Images.findBeginPoint(params, res.getWidth, res.getHeight), res)
    }
  }

  case class FaceGraphic(path: String, no: Int, transparent: Boolean) extends Value {
    def render(params: Params): ImagePart = {
      val image = PNG.read(path, transparent = transparent)
      val n = no
      val res = image.trim((n%4)*48, (n/4)*48, 48, 48) // for RPG Maker 200k
      ImagePart(Images.findBeginPoint(params, 48, 48), res.toBufferedImage(48))
    }
  }

  case class CharaGraphic(path: String, prop: CharaProperty, transparent: Boolean = true) extends Value {
    def render(params: Params): ImagePart = {
      import prop._
      val image = PNG.read(path, transparent = transparent)
      val (bx, by) = ((no%4)*72, (no/4)*128)
      val (sx, sy) = (act*24, dir*32)
      val res = image.trim(bx+sx, by+sy, 24, 32)
      ImagePart(Images.findBeginPoint(params, 24, 32), res.toBufferedImage(24))
    }
  }

  case class CharaProperty(no: Int, dir: Int, act: Int)

  case class BattleGraphic(path: String, no: Int, transparent: Boolean = true) extends Value {
    def render(params: Params): ImagePart = {
      val image = PNG.read(path, transparent = transparent)
      val res = image.trim(no%4*96, no/4*96, 96, 96)
      ImagePart(Images.findBeginPoint(params, 96, 96), res.toBufferedImage(96))
    }
  }

  // in Layout
  case class Window(systemGraphicsPath: String) extends Value {

    def render(params: Params): ImagePart = {

      val sysg = SystemGraphics.make(systemGraphicsPath)
      val rect = params.rect.getOrElse(Rect(0, 0, 1, 1))
      val buf = ImageUtils.newImage(rect.w, rect.h)

      val systemWindow = sysg.getSystemWindow(rect.w, rect.h, zoom=true)
      val newTo = buf.drawImageIndexColor(systemWindow, 0, 0).toBufferedImage(buf.getWidth)
      ImagePart((rect.x, rect.y), newTo)
    }
  }

  // CSV will be used interpolation, and convert to Values.Text when rendering.
  case class CSV(path: String, column: Int) extends ConvertibleValue[Text]

  // etc...
}
