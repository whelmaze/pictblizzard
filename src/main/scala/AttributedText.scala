package com.github.chuwb.pictbliz

import scala.collection.mutable

class AttributedText(val raw: String) {

  val (ranges, string) = parse(raw)
  def iter = ranges.iterator

  private[this] def parse(source: String): (List[AttributeRange], String) = {

    val acc = mutable.ListBuffer.empty[AttributeRange]
    val reseter = """\c[0]"""
    val sb = new StringBuffer()
    
    val addedsource = reseter + source + reseter
    
    def rec(trimmed: String, start: Int, diff: Int): Unit = {
      val re = """\\([a-z])\[(\d+)\]([^\\]*)\\""".r
      val matchResult = re.findFirstMatchIn(trimmed)
      if (matchResult.isDefined) {
        val m = matchResult.get
        val str = m.group(3)

        if (str.length != 0) {
          val command = m.group(1).head
          val comindex = m.group(2).head
          
          val nextStart = m.start + str.length
          acc += AttributeRange(m.start, nextStart, comindex)
          println(trimmed,nextStart)
          sb.append(str)
          rec(m.before + str + "\\" + m.after.toString, 0, 0)
        } else {
          rec(m.before + str + "\\" + m.after.toString, 0, 0)
        }
      }
    }

    rec(addedsource, 0, 0)
    (acc.toList, sb.toString)
  }
  
}
