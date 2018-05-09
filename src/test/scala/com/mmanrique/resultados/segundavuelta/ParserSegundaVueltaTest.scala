package com.mmanrique.resultados.segundavuelta

import org.scalatest.FunSuite

import scala.io.Source

class ParserSegundaVueltaTest extends FunSuite {
  test("Generate Result") {
    val parser = new ParserSegundaVuelta()
    val content = Source.fromResource("results/sample_1.txt").getLines().mkString
    val resultado = parser.getResults(content)
    assert(resultado.isInstanceOf[SegundaVuelta2016])
    assert(resultado.blancos == 0)
    assert(resultado.nulos == 13)
    assert(resultado.impugnados == 0)
    assert(resultado.total == 267)
    assert(resultado.ppk == 129)
    assert(resultado.fp == 125)
  }
}
