package com.mmanrique

import com.mmanrique.estructura.{Departamento, Distrito, Pais, Provincia}
import com.mmanrique.resultados.segundavuelta.SegundaVuelta2016
import com.typesafe.scalalogging.LazyLogging

class DataGenerator extends LazyLogging {

  val resultCache = new ResultCache

  def generateSummaryByPais(): Unit = {
    val maybePais = resultCache.getPais()
    assert(maybePais.isDefined, "Pais should be defined in cache")
    val vuelta2016 = summaryPais(maybePais.get)
    println(pprint.tokenize(vuelta2016).mkString)
  }

  def generateSummaryByDepartamento(): Unit = {
    val maybePais = resultCache.getPais()
    assert(maybePais.isDefined, "Pais should be defined in cache")
    val departamentoes = maybePais.get.departamentos.map(departamento => Departamento(departamento.name, departamento.ubigeo, Nil, summaryDepartamento(departamento)))

    departamentoes.foreach(departamento => {
      val prettyPrint = pprint.tokenize(departamento.segundaVuelta2016).mkString
      logger.info("Summary Departamento [{}]: [{}]", departamento.name, prettyPrint)
    })
  }

  def generateSummaryByProvincia(): Unit = {
    val maybePais = resultCache.getPais()
    assert(maybePais.isDefined, "Pais should be defined in cache")
  }

  def generateSummaryByDistrito(): Unit = {
    val maybePais = resultCache.getPais()
    assert(maybePais.isDefined, "Pais should be defined in cache")
  }


  def summaryDistrito(distrito: Distrito): SegundaVuelta2016 = {
    distrito.locales.toStream.flatMap(local => local.mesas).foldLeft(SegundaVuelta2016()) { (z, mesa) =>
      z + mesa.resultado
    }
  }

  def summaryProvincia(provincia: Provincia): SegundaVuelta2016 = {
    provincia.distritos.map(summaryDistrito).foldLeft(SegundaVuelta2016()) { (z, summaryDistrito) =>
      z + summaryDistrito
    }
  }

  def summaryDepartamento(departamento: Departamento): SegundaVuelta2016 = {
    departamento.provincias.map(summaryProvincia).foldLeft(SegundaVuelta2016()) { (z, summaryProvincia) =>
      z + summaryProvincia
    }
  }

  def summaryPais(pais: Pais): SegundaVuelta2016 = {
    pais.departamentos.map(summaryDepartamento).foldLeft(SegundaVuelta2016()) { (z, summaryDepartamento) =>
      z + summaryDepartamento
    }
  }

}

object DataGenerator {
  def main(args: Array[String]): Unit = {
    val generator = new DataGenerator()
//        generator.generateSummaryByDistrito()
//        generator.generateSummaryByProvincia()
    generator.generateSummaryByDepartamento()
    //    generator.generateSummaryByPais()
  }
}
