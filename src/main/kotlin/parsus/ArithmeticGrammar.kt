package com.example.parsus

import me.alllex.parsus.parser.*
import me.alllex.parsus.token.*
import kotlin.math.pow

import me.alllex.parsus.parser.*
import me.alllex.parsus.token.*
import kotlin.math.pow

sealed class Expr {
  data class Con(val value: Int) : Expr()
  data class Var(val name: String) : Expr()
  data class FuncCall(val name: String, val args: List<Expr>) : Expr()
  data class Neg(val expr: Expr) : Expr()
  data class Pow(val left: Expr, val right: Expr) : Expr()
  data class Mul(val left: Expr, val right: Expr) : Expr()
  data class Div(val left: Expr, val right: Expr) : Expr()
  data class Add(val left: Expr, val right: Expr) : Expr()
  data class Sub(val left: Expr, val right: Expr) : Expr()
}

object ExprParser : Grammar<Expr>() {
  // 空白文字を無視するトークンを定義
  init { regexToken("\\s+", ignored = true) }

  // トークンの定義
  val ident by regexToken("[a-zA-Z]+") // 変数名や関数名
  val int by regexToken("\\d+") // 整数値
  val lpar by literalToken("(")
  val rpar by literalToken(")")
  val comma by literalToken(",")
  val pow by literalToken("^")
  val times by literalToken("*")
  val div by literalToken("/")
  val plus by literalToken("+")
  val minus by literalToken("-")

  // 定数（整数）のパーサー
  val const by int map { Expr.Con(it.text.toInt()) }
  // 変数のパーサー
  val variable by ident map { Expr.Var(it.text) }
  // 関数呼び出しのパーサー
  val funcCall by ident * -lpar * separated(ref(::expr), comma) * -rpar map { (name, args) -> Expr.FuncCall(name.text, args) }

  // 括弧で囲まれた式のパーサー
  val braced by -lpar * ref(::expr) * -rpar

  // 項のパーサー（定数、関数呼び出し、変数、括弧式）
  val term: Parser<Expr> by parser {
    val neg = has(minus)
    val v = choose(const, funcCall, variable, braced)
    if (neg) Expr.Neg(v) else v
  }

  // べき乗のパーサー
  val powExpr by leftAssociative(term, pow) { l, _, r -> Expr.Pow(l, r) }

  // 乗除算のパーサー
  val mulExpr by leftAssociative(powExpr, times or div) { l, op, r ->
    if (op.token == times) Expr.Mul(l, r) else Expr.Div(l, r)
  }

  // 加減算のパーサー
  val addExpr by leftAssociative(mulExpr, plus or minus) { l, op, r ->
    if (op.token == plus) Expr.Add(l, r) else Expr.Sub(l, r)
  }

  // 式のパーサー
  val expr: Parser<Expr> by addExpr

  // ルートパーサー
  override val root by parser { expr() } // 入力の終わりまでパースする
}

class ArithmeticEvaluator(val variables: Map<String, Int>) {
  // 式を評価する関数
  fun evaluate(expr: Expr): Int = when (expr) {
    is Expr.Con -> expr.value
    is Expr.Var -> variables[expr.name] ?: error("未定義の変数: ${expr.name}")
    is Expr.FuncCall -> evaluateFunction(expr.name, expr.args)
    is Expr.Neg -> -evaluate(expr.expr)
    is Expr.Pow -> evaluate(expr.left).toDouble().pow(evaluate(expr.right).toDouble()).toInt()
    is Expr.Mul -> evaluate(expr.left) * evaluate(expr.right)
    is Expr.Div -> evaluate(expr.left) / evaluate(expr.right)
    is Expr.Add -> evaluate(expr.left) + evaluate(expr.right)
    is Expr.Sub -> evaluate(expr.left) - evaluate(expr.right)
  }

  // 関数を評価する関数
  private fun evaluateFunction(name: String, args: List<Expr>): Int {
    return when (name) {
      "AVG" -> {
        val argValues = args.map { evaluate(it) }
        val sum = argValues.sum()
        val average = sum.toDouble() / argValues.size
        average.toInt() // 必要に応じて四捨五入
      }
      else -> error("未知の関数: $name")
    }
  }
}

fun main() {
  val input = "AVG(A, B, C, (A + B + C))"
  val expr = ExprParser.parseOrThrow(input) // 式をパース

  val variableMap = mapOf("A" to 3, "B" to 4, "C" to 5) // 変数の値を定義
  val evaluator = ArithmeticEvaluator(variableMap)

  val result = evaluator.evaluate(expr) // 式を評価

  println("結果: $result") // 結果を表示
}
