package pictbliz
package sample

import com.typesafe.scalalogging.LazyLogging
import pictbliz.Values.Tkool2kDB
import scala.language.postfixOps

import Params._
import Layouts._

import scalaz.syntax.semigroup._

class TestSpec extends LazyLogging {

  def defaultStyle(fontName: String = "MS Gothic", size: Int = 12, style: Symbol = 'plain, inWin: Boolean = false): ParamSet =
      if (inWin)
        message |+| font(fontName, style, size)
      else
        font(fontName, style, size)

  @inline def r(i: Int): Int = scala.util.Random.nextInt(i)

  def run() {
    testReuseLayout()
    //testOldSpec()
    csvRead()
    faceSpec()
    charaSpec()
    battleSpec()
    dbRead()
  }

  def dbRead(): Unit = {
    import Values._

    val path = "testdata/no-v/RPG_RT.ldb"
    val vmap: Layouts.VMap = Map(
      "a" -> Tkool2kDB(path, "vocabulary", "ニューゲーム"),
      "filename" -> Text("a")
    )
    val interp = new Interpolator(vmap)

    val layout = WholeLayout(
      (320, 240),
      Seq(
        "a" -> PartLayout.ep(
          point(0, 0) |+|
          defaultStyle()
        )
      )
    )
    val gen = new Generator(layout)
    interp.iterator.map{ vm =>
      gen.genImage(vm)
    } foreach {
      _.write(Resource.tempDir + "db/")
    }

  }

  def csvRead(): Unit = {
    import Values._

    val path = "testdata/item.csv"
    val vmap: Layouts.VMap = Map(
      "itemno" -> CSV(path, 0),
      "name" -> CSV(path, 1),
      "win" -> Window("testdata/no-v/system6.png"),
      "t_price" -> CSV(path, 2), "t_desc" -> CSV(path, 3), "t_kind" -> CSV(path, 4), "t_atk" -> CSV(path, 6), "t_hit" -> CSV(path, 15),
      "price" -> Text("#{t_price} \\c[4]G\\c[0]"),
      "desc" -> Text("\\c[2]#{t_desc}\\c[0]"),
      "misc" -> Text("攻撃#{t_atk}, 命中率:#{t_hit}, #{t_kind}武器"),
      "filename" -> Text("#{itemno}-#{name}")
    )
    val interp = new Interpolator(vmap, ids = 0 to 76)

    val layout = WholeLayout(
      (320, 80),
      Seq(
        "win" -> PartLayout.ep(
          rect(0, 0, 320, 80) |+|
          frontColor("testdata/no-v/system6.png")),
        "name" -> PartLayout.ep(
          point(0, 0) |+|
          defaultStyle(style='bold, inWin=true)),
        "price" -> PartLayout.ep(
          rect(200, 0, 120, 30) |+|
          align(Align.Right, Align.Top) |+|
          interval(2, 2) |+|
          hemming(0xff003100, 1) |+|
          defaultStyle(inWin=true)),
        "desc" -> PartLayout.ep(
          point(10, 18) |+|
          defaultStyle(inWin=true)),
        "misc" -> PartLayout.ep(
          rect(10, 40, 310, 40) |+|
          align(Align.Right, Align.Bottom) |+|
          defaultStyle(size=12, inWin=true))
      )
    )
    val gen = new Generator(layout)

    interp.iterator.map { vm =>
      gen.genImage(vm)
    } foreach {
      _.write(Resource.tempDir + "items/")
    }
  }

  def faceSpec(): Unit = {
    import Values._

    val layout = WholeLayout(
      (320, 240),
      Seq(
        "a" -> PartLayout.ep(point(0, 0)),
        "b" -> PartLayout.ep(point(50, 0)),
        "c" -> PartLayout.ep(point(100, 0)),
        "d" -> PartLayout.ep(point(0, 50)),
        "e" -> PartLayout.ep(point(50, 50)),
        "f" -> PartLayout.ep(point(100, 50))
      )
    )
    val rs = "testdata/no-v/ds1.png"
    val vm = Map(
      "a" -> FaceGraphic(rs, 0, transparent = false),
      "b" -> FaceGraphic(rs, 1, transparent = false),
      "c" -> FaceGraphic(rs, 2, transparent = false),
      "d" -> FaceGraphic(rs, 3, transparent = false),
      "e" -> FaceGraphic(rs, 4, transparent = false),
      "f" -> FaceGraphic(rs, 5, transparent = false),
      "filename" -> Text("facetest")
    )
    val gen = new Generator(layout)
    gen.genImage(vm).write(Resource.tempDir)
  }

  def charaSpec(): Unit = {
    import Values._

    val layout = WholeLayout(
      (320, 240),
      Seq(
        (for(c <- 1 to 200) yield {
          c.toString -> PartLayout.ep(point(r(320), r(240)))
        }): _*
      )
    )
    val rs = "testdata/no-v/sl1.png"
    val _vm = (for(c <- 1 to 200) yield {
      c.toString -> CharaGraphic(rs, CharaProperty(r(8), r(4), r(3)))
    }).toMap
    val vm = _vm + ("filename" -> Text("charatest"))
    val gen = new Generator(layout)
    gen.genImage(vm).write(Resource.tempDir)
  }

  def battleSpec(): Unit = {
    import Values._

    val layout = WholeLayout(
      (320, 240),
      Seq(
        (for(c <- 1 to 20) yield {
          c.toString -> PartLayout.ep(point(r(320), r(240)))
        }): _*
      )
    )
    val rs = "testdata/no-v/bs1.png"
    val _vm = (for(c <- 1 to 20) yield {
      c.toString -> BattleGraphic(rs, r(15), transparent = true)
    }).toMap
    val vm = _vm + ("filename" -> Text("battletest"))
    val gen = new Generator(layout)
    gen.genImage(vm).write(Resource.tempDir)
  }

  def testReuseLayout(): Unit = {
    import Values._

    val lay = WholeLayout(
      (320, 240),
      Seq(
      "name" -> PartLayout.ep(
        rect(5,5,300,20) |+|
        autoExpand |+|
        defaultStyle()
      ),
      "icon" -> PartLayout.ep(
        rect(280,0,32,32) |+|
        defaultStyle()
      ),
      "desc" -> PartLayout.ep(
        rect(0, 20, 12, 2) |+|
        window("testdata/no-v/system6.png") |+|
        autoExpand |+|
        frontColor("testdata/no-v/system6.png") |+|
        defaultStyle(inWin = true)
      ),
      "cost" -> PartLayout.ep(
        rect(300,2,30,15) |+|
        font("Verdana", 'plain, 10)
      )
    ))

    lay.parts.foreach { case (k, p) => logger.info(s"$k: $p")}

    val v1 = Map(
      "name" -> Text("エターナルフォースブリザード"),
      "icon" -> Icon("testdata/icon/icon1.png"),
      "desc" -> Text("一瞬で相手の周囲の大気ごと氷結させる\n相手は死ぬ"),
      "cost" -> Text("42"))

    val v2 = Map(
      "name" -> Text("\\c[2]サマーサンシャインバースト"),
      "icon" -> Icon("testdata/icon/icon2.png"),
      "desc" -> Text("一瞬で太陽を相手の頭上に発生させる\n相手は\\c[4]死ぬ\n\n\\c[0]どう考えても自分も\\c[4]死ぬ"),
      "cost" -> Text("42")) //

    val v3 = Map(
      "name" -> Text("\\c[1]インフェルノ\\c[0]・\\c[2]オブ\\c[0]・\\c[3]メサイア"),
      "icon" -> Icon("testdata/icon/icon3.png"),
      "desc" -> Text("冥界王ダーク・インフェルノを召喚し半径8kmの大地に\n無差別に\\c[2]種\\c[0]を撒き散らしそれはやがて実を結ぶ"),
      "cost" -> Text("42"))
    
    val v4 = Map(
      "name" -> Text("ヘルズボルケイノシュート"),
      "icon" -> Icon("testdata/icon/icon1.png"),
      "desc" -> Text("\\c[6]死の世界\\c[0]から呼び寄せた\\c[8]闇\\c[0]の\\c[2]火弾\\c[0]を\nマッハ2でぶつける\n相手は\\c[4]死ぬ"),
      "cost" -> Text("42"))

    val v5 = Map(
      "name" -> Text("アルティメットインフィニティサンデイ"),
      "icon" -> Icon("testdata/icon/icon2.png"),
      "desc" -> Text("毎日が日曜日。\n相手は死ぬ"),
      "cost" -> Text("42"))

    val v6 = Map(
      "name" -> Text("エンパイア・ステート・ビル"),
      "icon" -> Icon("testdata/icon/icon3.png"),
      "desc" -> Text("1931年に建てられた高さ443m、102階建てのビル。\n相手は死ぬ"),
      "cost" -> Text("42"))

    val _vs = List(v1,v2,v3,v4,v5,v6)
    //val vs = List(v4)

    val vs = _vs.zipWithIndex.map { case (s, i) =>
      s + ("filename" -> Text(f"skill$i%2d"))
    }

    val gen = new Generator(lay)
    vs.map {
      gen.genImage(_)
    }.foreach { case res =>
      res.write(Resource.tempDir)
    }
  }

  def testOldSpec(): Unit = {
    import Values._

    val layout = WholeLayout(
      (320,240),
      Seq(
        "icon1" -> PartLayout.ep(point(0,0)),
        "icon2" -> PartLayout.ep(point(50,0)),
        "icon3"-> PartLayout.ep(point(100,0)),
        "str" -> PartLayout.ep(
          rect(10,30,200,30) |+|
          align(Align.Center, Align.Bottom) |+|
          border |+|
          font("Terminus-ja", 'plain, 12)
        ),
        "str2" -> PartLayout.ep(
          rect(10,60,200,120) |+|
          font("Terminus-ja", 'plain, 12) |+|
          align(Align.Center, Align.Center)
        )
      )
    )

    val valuemap = Map(
      'icon1 -> Icon("icon/icon1.png"),
      'icon2 -> Icon("icon/icon2.png"),
      'icon3 -> Icon("icon/icon3.png"),
      'str -> Text("test"),
      'str2 -> Text("a\nb\ncedf\ngiaasdasd\near"),
      'filename -> Text("test"))

    val gen = new Generator(layout)
    //val result = gen.genImage(valuemap)
    //result.write(Resource.tempdir)
  }


}
