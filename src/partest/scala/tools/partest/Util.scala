package scala.tools.partest

import scala.language.experimental.macros
import scala.collection.mutable

object Util {
  /**
   * `trace("".isEmpty)` will return `true` and as a side effect print the following to standard out.
   * {{{
   *   trace> "".isEmpty
   *   res: Boolean = true
   *
   * }}}
   *
   * An alternative to [[scala.tools.partest.ReplTest]] that avoids the inconvenience of embedding
   * test code in a string.
   */
  def trace[A](a: A): A = macro traceImpl[A]

  import scala.reflect.macros.blackbox.Context
  def traceImpl[A: c.WeakTypeTag](c: Context)(a: c.Expr[A]): c.Expr[A] = {
    import c.universe._
    import definitions._

    // xeno.by: reify shouldn't be used explicitly before the final release of 2.10.0,
    // because this impairs reflection refactorings
    //
    // val exprCode = c.literal(show(a.tree))
    // val exprType = c.literal(show(a.actualType))
    // reify {
    //   println(s"trace> ${exprCode.splice}\nres: ${exprType.splice} = ${a.splice}\n")
    //   a.splice
    // }

    c.Expr(Block(
      List(Apply(
        Select(Ident(PredefModule), TermName("println")),
        List(Apply(
          Select(Apply(
            Select(Ident(ScalaPackage), TermName("StringContext")),
            List(
              Literal(Constant("trace> ")),
              Literal(Constant("\\nres: ")),
              Literal(Constant(" = ")),
              Literal(Constant("\\n")))),
          TermName("s")),
          List(
            Literal(Constant(show(a.tree))),
            Literal(Constant(show(a.actualType))),
            a.tree))))),
      a.tree))
  }

  private def prettyArray0(a: Array[_]): collection.IndexedSeq[Any] = {
    prettyArray0(mutable.ArraySeq.make(a))
  }
  private def prettyArray0(a: mutable.ArraySeq[_]): collection.IndexedSeq[Any] = {
    new scala.collection.AbstractSeq[Any] with scala.collection.IndexedSeq[Any] {
      def length = a.length
      def apply(idx: Int): Any = a.apply(idx) match {
        case x: Array[_] => prettyArray0(x)
        case x => x
      }
      override def className = "Array"
    }
  }

  def prettyArray(a: Array[_]): collection.IndexedSeq[Any] = prettyArray0(a)

  implicit class ArrayDeep(val a: Array[_]) extends AnyVal {
    def prettyArray: collection.IndexedSeq[Any] = prettyArray0(a)
  }

}
