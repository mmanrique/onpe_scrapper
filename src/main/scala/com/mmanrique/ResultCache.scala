package com.mmanrique

import java.io.FileWriter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.mmanrique.ResultCache._
import com.mmanrique.estructura._
import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

class ResultCache extends LazyLogging {
  val mapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  def getProvincia(ubigeo: String): Option[Provincia] = {
    val cachedResult = ResultCache.cacheProvinciaMap.get(ubigeo)
    if (cachedResult.isEmpty) {
      None
    } else {
      Option(mapper.readValue(cachedResult.get, classOf[Provincia]))
    }
  }

  def getDistrito(ubigeo: String): Option[Distrito] = {
    val cachedResult = ResultCache.cacheDistritoMap.get(ubigeo)
    if (cachedResult.isEmpty) {
      None
    } else {
      Option(mapper.readValue(cachedResult.get, classOf[Distrito]))
    }
  }

  def getDepartamento(ubigeo: String): Option[Departamento] = {
    val cachedResult = ResultCache.cacheDepartamentoMap.get(ubigeo)
    if (cachedResult.isEmpty) {
      None
    } else {
      Option(mapper.readValue(cachedResult.get, classOf[Departamento]))
    }
  }

  def getLocal(id: String): Option[Local] = {
    val cachedResult = ResultCache.cacheLocalMap.get(id)
    if (cachedResult.isEmpty) {
      None
    } else {
      Option(mapper.readValue(cachedResult.get, classOf[Local]))
    }
  }

  def getPais(): Option[Pais] = {
    val source = Source.fromResource(ResultCache.cachePais)
    val json = source.getLines().mkString
    try {
      val cachedPais = mapper.readValue(json, classOf[Pais])
      source.close()
      Option(cachedPais)
    } catch {
      case _: Exception => None
    }
  }


  def appendProvincia(provincia: Provincia): Unit = {
    val json = mapper.writeValueAsString(provincia)
    val line = provincia.ubigeo + "|" + json
    logger.info("Appending Provincia line [{}]", line)
    appendLineToFile(cacheProvincias, line)
  }

  def appendDepartamento(departamento: Departamento): Unit = {
    val json = mapper.writeValueAsString(departamento)
    val line = departamento.ubigeo + "|" + json
    logger.info("Appending departamentos line [{}]", line)
    appendLineToFile(cacheDepartamentos, line)
  }

  def appendDistrito(distrito: Distrito): Unit = {
    val json = mapper.writeValueAsString(distrito)
    val line = distrito.ubigeo + "|" + json
    logger.info("Appending Distrito line [{}]", line)
    appendLineToFile(cacheDistritos, line)
  }

  def appendLocal(local: Local, id: String): Unit = {
    val json = mapper.writeValueAsString(local)
    val line = id + "|" + json
    logger.info("Appending Local line [{}]", line)
    appendLineToFile(cacheLocal, line)
  }

  def appendPais(pais: Pais): Unit = {
    val json = mapper.writeValueAsString(pais)
    appendLineToFile(cachePais, json)
  }

}

object ResultCache {
  private val cacheProvincias = "output/provincias.txt"
  private val cacheDepartamentos = "output/departamentos.txt"
  private val cacheDistritos = "output/distritos.txt"
  private val cacheLocal = "output/locales.txt"
  private val cachePais = "output/pais.txt"
  val cacheProvinciaMap: Map[String, String] = readFile(cacheProvincias)
  val cacheDepartamentoMap: Map[String, String] = readFile(cacheDepartamentos)
  val cacheDistritoMap: Map[String, String] = readFile(cacheDistritos)
  val cacheLocalMap: Map[String, String] = readFile(cacheLocal)


  private def readFile(fileName: String): Map[String, String] = {
    val source = Source.fromResource(fileName)
    val result = source.getLines().
      map(_.split("\\|")).
      map(a => a(0) -> a(1)).
      toMap
    source.close()
    result
  }

  private def appendLineToFile(fileName: String, line: String): Unit = {
    val path = getClass.getResource("/" + fileName).getPath
    val fw = new FileWriter(path, true)
    fw.write(line)
    fw.write("\n")
    fw.flush()
    fw.close()
  }
}