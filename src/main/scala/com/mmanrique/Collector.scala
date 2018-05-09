package com.mmanrique

import com.mmanrique.estructura._
import com.mmanrique.resultados.segundavuelta.{ParserSegundaVuelta, SegundaVuelta2016}
import com.typesafe.scalalogging.LazyLogging
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder

import scala.io.Source

class Collector extends LazyLogging {
  val browser = JsoupBrowser()
  val resultParser = new ParserSegundaVuelta
  val resultCache = new ResultCache

  def colectarDatos(): Pais = {
    val cachedPais = resultCache.getPais()
    if (cachedPais.isDefined) {
      logger.info("Using cached Pais")
      cachedPais.get
    } else {
      val departamentos = getDepartamentos
      val pais = Pais(departamentos)
      resultCache.appendPais(pais)
      pais
    }
  }

  def getDepartamentos: List[Departamento] = {
    val request = Source.fromResource("getDepartamentos.txt").getLines().mkString
    val str = getResponse(request)
    val document = browser.parseString(str)
    val elements1 = document >> elementList("select option")
    elements1.filter(!_.attr("value").isEmpty).map(e => createDepartamento(e.innerHtml, e.attr("value")))
  }

  def createDepartamento(name: String, ubigeo: String): Departamento = {
    logger.debug("Obteniendo datos de Departamento [{}]", name)
    val maybeDepartamento = resultCache.getDepartamento(ubigeo)
    if (maybeDepartamento.isDefined) {
      logger.info("Utilizando cache de Departamentos [{}]", name)
      maybeDepartamento.get
    } else {
      val provincias = getProvincias(ubigeo)
      val departamento = Departamento(name, ubigeo, provincias)
      resultCache.appendDepartamento(departamento)
      departamento
    }
  }

  def getProvincias(ubigeoDepartamento: String): List[Provincia] = {
    val format = Source.fromResource("getProvincias.txt").getLines().mkString
    val request = String.format(format, ubigeoDepartamento)
    val str = getResponse(request)
    val document = browser.parseString(str)
    val elements1 = document >> elementList("select option")
    elements1.filter(!_.attr("value").isEmpty).map(e => createProvincia(e.innerHtml, e.attr("value")))
  }


  def createProvincia(name: String, ubigeo: String): Provincia = {
    logger.debug("Obteniendo datos de Provincia [{}]", name)
    val maybeProvincia = resultCache.getProvincia(ubigeo)
    if (maybeProvincia.isDefined) {
      logger.info("Utilizando cache de Provincias [{}]", name)
      maybeProvincia.get
    } else {
      val distritos = getDistritos(ubigeo)
      val provincia = Provincia(name, ubigeo, distritos)
      resultCache.appendProvincia(provincia)
      provincia
    }
  }


  def getDistritos(ubigeoProvincia: String): List[Distrito] = {
    val format = Source.fromResource("getDistritos.txt").getLines().mkString
    val request = String.format(format, ubigeoProvincia)
    val str = getResponse(request)
    val document = browser.parseString(str)
    val elements1 = document >> elementList("select option")
    elements1.filter(!_.attr("value").isEmpty).map(e => createDistrito(e.innerHtml, e.attr("value")))
  }

  def createDistrito(name: String, ubigeo: String): Distrito = {
    logger.debug("Obteniendo datos de Distrito [{}]", name)
    val maybeDistrito = resultCache.getDistrito(ubigeo)
    if (maybeDistrito.isDefined) {
      logger.info("Utilizando Cache de Distritos [{}]", name)
      maybeDistrito.get
    } else {
      val locales = getLocales(ubigeo)
      val distrito = Distrito(name, ubigeo, locales)
      resultCache.appendDistrito(distrito)
      distrito
    }
  }

  def getLocales(ubigeoDistrito: String): List[Local] = {
    val format = Source.fromResource("getLocalesVotacion.txt").getLines().mkString
    val request = String.format(format, ubigeoDistrito)
    val str = getResponse(request)
    val document = browser.parseString(str)
    val elements1 = document >> elementList("select option")
    elements1.filter(!_.hasAttr("selected")).map(e => createLocal(e.innerHtml, e.attr("value")))
  }

  def createLocal(name: String, value: String): Local = {
    logger.info("Obteniendo datos de Local [{}]", name)
    val maybeLocal = resultCache.getLocal(value)
    if (maybeLocal.isDefined) {
      logger.info("Utilizando cache de Local [{}]", name)
      maybeLocal.get
    } else {
      val strings = value.split("\\?")
      val idLocal = strings(0)
      val ubigeoLocal = strings(1)
      val mesas = getMesas(idLocal, ubigeoLocal)
      val local = Local(name, mesas)
      resultCache.appendLocal(local, value)
      local
    }

  }

  def getMesas(idLocal: String, ubigeoLocal: String): List[Mesa] = {
    val format = Source.fromResource("displayActas.txt").getLines().mkString
    val request = String.format(format, ubigeoLocal, idLocal, ubigeoLocal)
    val str = getResponse(request)
    val document = browser.parseString(str)
    val elements = document >> elementList(".table17 td")
    elements.map(e => createMesa(e.text, ubigeoLocal))
  }

  def createMesa(idMesa: String, ubigeoLocal: String): Mesa = {
    logger.info("Obteniendo datos de Mesa [{}]", idMesa)
    val resultado = getResultado(idMesa, ubigeoLocal)
    Mesa(idMesa, resultado)
  }

  def getResultado(idMesa: String, ubigeoLocal: String): SegundaVuelta2016 = {
    logger.info("Obteniendo Resultados de mesa [{}]", idMesa)
    val format = Source.fromResource("displayMesas.txt").getLines().mkString
    val request = String.format(format, ubigeoLocal, idMesa)
    val str = getResponse(request)
    resultParser.getResults(str)
  }

  def getResponse(payload: String): String = {
    val client = HttpClientBuilder.create().build()
    val post = new HttpPost(Collector.COUNTRY_URI)
    post.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
    post.setHeader("X-Requested-With", "XMLHttpRequest")
    logger.debug("Enviando payload [{}]", payload)
    post.setEntity(new StringEntity(payload))
    val responseObject = client.execute(post)
    val inputStream = responseObject.getEntity.getContent
    val response = Source.fromInputStream(inputStream).getLines().mkString
    inputStream.close()
    logger.debug("Recibi response [{}]", response)
    response
  }

}

object Collector {
  val COUNTRY_URI: String = "https://www.web.onpe.gob.pe/modElecciones/elecciones/elecciones2016/PRP2V2016/ajax.php"
}
