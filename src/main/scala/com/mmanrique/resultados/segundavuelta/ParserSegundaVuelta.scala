package com.mmanrique.resultados.segundavuelta

import com.mmanrique.resultados.ResultParser
import com.typesafe.scalalogging.LazyLogging
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

class ParserSegundaVuelta extends ResultParser[SegundaVuelta2016] with LazyLogging {

  private val browser = JsoupBrowser()

  override def getResults(content: String): SegundaVuelta2016 = {
    try {
      val document = browser.parseString(content)
      val tablaResultados = document >> element(".cont-tabla1")
      val filas = (tablaResultados >> elementList("tr")).filter(!_.hasAttr("class"))
      val ultimos = filas.map(fila => (fila >> elementList("td")).last)
      val resultados = ultimos.map(element => element.text.toInt).toArray


      SegundaVuelta2016(
        resultados(0),
        resultados(1),
        resultados(2),
        resultados(3),
        resultados(4),
        resultados(5)
      )
    } catch {
      case e: Exception =>
        logger.error("Got exception while parsing result [{}]", content)
        logger.error("error", e)
        throw e
    }

  }
}
