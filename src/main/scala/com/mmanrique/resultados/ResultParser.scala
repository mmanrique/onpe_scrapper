package com.mmanrique.resultados

trait ResultParser[A <: Resultado] {
  def getResults(content: String): A
}
