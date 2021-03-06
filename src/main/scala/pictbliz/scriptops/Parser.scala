package pictbliz
package scriptops

import scala.util.parsing.combinator.syntactical._
import scala.collection.mutable

import java.net.URI
import java.io.File

object Parser extends StandardTokenParsers {
  import NodeDefinition._
  import Layouts._

  private[scriptops] implicit class StringConv(val s: String) extends AnyVal {
    def toURI: URI = new File(s).toURI
  }

  lexical.delimiters ++= List("(",")","{","}","+","-","*","/","=","$",".",",","@",":","..", "[", "]", "#")
  lexical.reserved += ("val", "layout", "values", "value", "with", "generate")

  val layouts = mutable.Map.empty[String, WholeLayout]
  val valuemaps = mutable.Map.empty[String, VMap]

  lazy val all: Parser[List[Node]] = rep1(top)
  lazy val top: Parser[Node] = bind | gen
  lazy val bind: Parser[BindExpr] = "val" ~> ident ~ "=" ~ exp ^^ {
    case i ~ _ ~ (ex: Node) =>
      BindExpr(i, ex)
  }

  lazy val gen: Parser[GenerateExpr] = {
    "generate" ~> ident ~ "with" ~ repsep(ident, ",") ^^ {
      case name ~ _ ~ values =>
        GenerateExpr(Var(name), values.map(Var))
    }
  }

  lazy val exp = evaluable | layoutOne | valuesOne | layoutOnes | layoutDef | valuesDef
  lazy val evaluable: Parser[Node] = range | literal | numberseq //| (ident ^^ { case s => Var(s) })

  lazy val range: Parser[RangeValue] = numericLit ~ ".." ~ numericLit ^^ {
    case begin ~ _ ~ end => RangeValue(begin.toInt, end.toInt)
  }

  lazy val numberseq: Parser[NumberSeqValue] = "[" ~> repsep(numericLit, ",") <~ "]" ^^ {
    case seq => println("a"); NumberSeqValue(seq.map(_.toInt))
  }

  lazy val literal: Parser[NodeValue] = {
    numericLit ^^ { case n => NumberValue(n.toInt) } |
    stringLit ^^ {
      case s => StrValue(s.replace("\\n", "\n"))
    }
  }

  lazy val layoutOne: Parser[Node] = {
    ident ~ ":" ~ layoutApplys ^^ {
      case i ~ _ ~ a => LayoutOne(i, a)
    } | ident ^^ { s => Var(s) }
  }

  lazy val layoutOnes: Parser[LayoutOnes] = {
    "{" ~> rep1(layoutOne) <~ "}" ^^ { case ls => LayoutOnes(ls) }
  }

  lazy val layoutApply: Parser[LayoutApply] = {
    ident ~ "(" ~ repsep(evaluable, ",") <~ ")" ^^ {
      case name ~ _ ~ args =>
        LayoutApply(name, args)
    } | ident ^^ { s => LayoutApply(s, Nil) }
  }

  lazy val layoutApplys: Parser[List[LayoutApply]] = {
    layoutApply ^^ { case a => List(a) } |
    "{" ~> repsep(layoutApply, ",") <~ "}" ^^ { case a => a }
  }

  lazy val symbol = ":" ~> ident ^^ { case i => s":$i"}

  lazy val layoutDef: Parser[LayoutDef] = "layout" ~> "(" ~> layoutApplys ~ ")" ~ layoutOnes ^^ {
    case env ~ _ ~ as => LayoutDef(env, as.ls)
  }

  lazy val valuesDef: Parser[ValuesDef] = {
    "values" ~> "{" ~> rep1(valuesOne) <~ "}" ^^ {
      case s => ValuesDef(s)
    }
  }

  lazy val valuesOne: Parser[Node] = {
    ident ~ ":" ~ evaluable ^^ { case i ~ _ ~ ev => ValuesOne(i, ev) } |
    ident ~ ":" ~ valuesApply ^^ { case i ~ _ ~ va => ValuesOne(i, va) } |
    ident ^^ { s => Var(s) }
  }

  lazy val valuesApply: Parser[ValuesApply] = ident ~ "(" ~ repsep(evaluable, ",") <~ ")" ^^ {
    case i ~ _ ~ seq => ValuesApply(i, seq)
  }

  def parse(source: String): Seq[Any] = {
    all(new lexical.Scanner(source)) match {
      case Success(results, _) =>
        println(results.mkString("\n")) // => とりあえず文字列で出力
        results
      case Failure(msg, d) => println(msg); println(d.pos.longString); sys.error("")
      case Error(msg, _) => println(msg); sys.error("")
    }
  }


}

