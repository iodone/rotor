package rotor.engine.rql

/**
  * Created by iodone on {19-3-21}.
  */


import fastparse._, NoWhitespace._

object Ast {

  sealed trait Context {
  }

  object Context {
    case class Identifier(name: String) extends Context {
      def getText = name
    }
    case class Format(name: String) extends Context {
      def getText = name
    }
    case class Path(name: String) extends Context {
      def getText = name
    }
    case class Table(name: String) extends Context {
      def getText = name
    }
    case class Db(name: String) extends Context {
      def getText = name
    }

    case class Expr(name: String, value: String) extends Context {
      def getText = (name, value)
    }
    case class Where(value: Seq[Expr]) extends Context {
      def getText = value.map(_.getText)
    }
    case class Source(format: Format, path: Path) extends Context
  }


  sealed trait Action
  object Action {
    case class ConnectAction(format: Context.Format, db: Context.Db, where: Option[Context.Where]) extends Action
    case class LoadAction(source: Context.Source, table: Context.Table, where: Option[Context.Where]) extends Action
    case class SelectAction(body: String) extends Action
  }

}

object RQLParser {

  def load[_:P] = P( ("load" | "LOAD") ~ space ~ format ~ "." ~ path ~ space ~ (("options" | "where") ~ space ~ expr ~ space ~ booleanExpr.rep).? ~ "as" ~ space ~ table ).map {
    case (format, path, None, table) =>
      val source = Ast.Context.Source(format, path)
      Ast.Action.LoadAction(source, table, None)
    case (format, path, Some((h, t)), table) =>
      val source = Ast.Context.Source(format, path)
      val where = Ast.Context.Where(h +: t)
      Ast.Action.LoadAction(source, table, Some(where))
  }

  def select[_:P] = P( ("select" | "SELECT") ~ space ~ CharsWhile(_ != ';') ).!.map(Ast.Action.SelectAction)

  def connect[_:P] = P( ("connect" | "CONNECT") ~ space ~ format ~ space ~ (("options" | "where") ~ space ~ expr ~ space ~ booleanExpr.rep).? ~ "as" ~ space ~ db ).map {
    case (format, None, db) => Ast.Action.ConnectAction(format, db, None)
    case (format, Some((h, t)), db) => {
      val where = Ast.Context.Where(h +: t)
      Ast.Action.ConnectAction(format, db, Some(where))
    }
  }
  def comment[_:P] = ???

  def booleanExpr[_:P] = P("and" ~ space ~ expr ~ space )
  def expr[_:P] = P( identifier.! ~ "=" ~ string.! ).map(Ast.Context.Expr.tupled)
  def format[_:P] = P( identifier ).!.map(Ast.Context.Format)
  def path[_:P] = P( backquoted | identifier ).!.map(Ast.Context.Path)
  def table[_:P] = P( identifier ).!.map(Ast.Context.Table)
  def db[_:P] = P( identifier ~ ("." ~ identifier).rep ).!.map(Ast.Context.Db)
  def identifier[_:P] = P( (CharIn("a-zA-Z") | CharIn("0-9") | "_").rep(1) | backquoted ).!.map(Ast.Context.Identifier)

  // base element
  def stringChars(c: Char) =  c != '\"' && c != '\\'
  def escape[_: P]        = P( "\\" ~ (CharIn("\"/\\\\bfnrt")))
  def string[_:P] = P( "\"" ~ (CharPred(stringChars) | escape).rep ~ "\"" )
  def backquoted[_:P] = P( "`" ~ (CharPred(_ != '`') | "``").rep ~ "`" )
  def space[_: P]         = P( CharsWhileIn(" \r\n", 1) )
  def spaceOrNot[_: P]         = P( CharsWhileIn(" \r\n", 0) )

  def rql[_:P] = P( (spaceOrNot ~ (load | select | connect) ~ ";" ~ spaceOrNot ).rep(1) )

  def parseRql(rqlScript: String)= {
    val parsed = parse(rqlScript, rql(_))
    val result = parsed match {
      case f: Parsed.Failure => throw new Exception(f.trace().longTerminalsMsg)
      case s: Parsed.Success[Seq[Product with Serializable with Ast.Action]] =>
        s.value
    }
    result
  }


  def main(args: Array[String]) = {
    val rql0 =
      """
        |select * from table_1 as table_2;
        |
        |load tidb.`db2.tidb_news_info` as tidb_news_info;
        |
        |connect es where `es.nodes`="192.168.200.152" and `es.port`="9200" and `es.net.http.auth.user`="test" and `es.net.http.auth.pass`="123456" as db1;
      """.stripMargin
//    val Parsed.Success(a, b) = parse(rql0, rql(_))
//    println(a, b)

  }
}
