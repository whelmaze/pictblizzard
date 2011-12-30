package com.github.chuwb.pictbliz
package test

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import scala.collection.mutable.Stack

class TestSpec extends WordSpec with ShouldMatchers {

  import ScriptOps.implicits.string2URI
  
  "Ops" should {
    
    import ScriptOps._
    val lay = LayoutUnit(
      Map('size -> APoint(320, 60)),
      Map(
      'name->AreaUnit(Map('rect->ARect(5,10,200,13))),
      'icon->AreaUnit(Map('rect->ARect(0,0,32,32))),
      'desc->AreaUnit(Map('rect->ARect(8,25,280,13),
                                'x_interval-> AXInterval(5))),
      'cost->AreaUnit(Map('rect->ARect(280,45,30,15),
                                'font->AFont("Verdana", 'plain, 10))))
    )
    
    println(lay)

    val testvalues = Map(
      'name->Str("エターナルドリルクラッシュ"),
      'icon->Icon("icon/icon1.png"),
      'desc->Str("ひたすらドリルで突き刺す。相手は死ぬ"),
      'cost->Str("42"))

    (new Drawer(testvalues, lay, NullContext)).draw().write("temp/TestSpec.png")

    "a" in { val s = 1
      s should be (s) }
  }

  
  // "stack" should {
  //   val stack = new Stack[Int]
  //   "be empty" in {
  //     stack should be ('empty)
  //   }
  //   "complain when popped" in {
  //     evaluating { stack.pop() } should produce [NoSuchElementException]
  //   }
  // }

}
