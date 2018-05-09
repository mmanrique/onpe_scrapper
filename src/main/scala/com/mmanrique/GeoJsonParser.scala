package com.mmanrique

import java.io.{File, FileWriter}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.mmanrique.estructura.{Departamento, Pais, Provincia}
import com.mmanrique.geojson.{Feature, GeoJson}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.io.Source

class GeoJsonParser extends LazyLogging {
  val mapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  val resultCache = new ResultCache
  val dataGenerator = new DataGenerator

  def replaceSpecialChars(s: String): String = {
    s.replaceAll("á", "a").replaceAll("í", "i").replaceAll("Á", "A").replaceAll("Í", "I").replaceAll("É", "E").replaceAll("Ó", "O").replaceAll("Ú", "U")
  }

  def parseDepartamentos(): Unit = {
    val departamentos = Source.fromResource("geojson/input/departamento.json").getLines().mkString
    val geoJson = mapper.readValue(departamentos, classOf[GeoJson])
    val geoJsonDepartamentos = geoJson.features.map(f => (replaceSpecialChars(f.properties("name_1").toUpperCase), f)).toMap
    val pais: Pais = getPais
    val new_features = pais.departamentos.map(d => {
      val formatted = replaceSpecialChars(d.name.toUpperCase())
      val feature = geoJsonDepartamentos(formatted)
      merge(d, feature, formatted)
    })
    val str = mapper.writeValueAsString(GeoJson(geoJson.typeName, new_features))
    writeFile("departamentos_summary.json", str)
  }

  def parseProvincias(): Unit = {
    val found = mutable.Set[String]()
    val departamentos = Source.fromResource("geojson/input/provincias.json").getLines().mkString
    val geoJson = mapper.readValue(departamentos, classOf[GeoJson])
    val geoJsonProvincias = geoJson.features.map(f => (replaceSpecialChars(f.properties("name_2").toUpperCase), f)).toMap
    val pais: Pais = getPais
    val new_features = pais.departamentos.flatMap(d => d.provincias.map(p => (p, d.name))).map(t => {
      val provincia = t._1
      val departamentoName = replaceSpecialChars(t._2.toUpperCase())
      val formatted = replaceSpecialChars(provincia.name.toUpperCase())
      if (geoJsonProvincias.contains(formatted)) {
        val feature = geoJsonProvincias(formatted)
        Option(merge(provincia, feature, formatted, departamentoName))
      } else {
        None
      }
    }).filter(o => o.isDefined).map(o => o.get)
    val str = mapper.writeValueAsString(GeoJson(geoJson.typeName, new_features))
    writeFile("provincias_summary.json", str)
    //    pais.departamentos.flatMap(d => d.provincias).foreach(p => {
//      val formatted = replaceSpecialChars(p.name.toUpperCase())
//      if(!geoJsonProvincias.contains(formatted)){
//        logger.error("Provincia [{}] not found in geoJson", formatted)
//      }else{
//        found += formatted
//      }
//    })
//    geoJsonProvincias.foreach(a => {
//      if (!found.contains(a._1)) {
//        logger.error("GeoJson [{}] not found", a._1)
//      }
//    })
  }

  def merge(provincia:Provincia, feature:Feature, name:String, nameDepartamento:String):Feature ={
    val geoGeometry = feature.geometry
    val typeName = feature.typeName
    val summary = dataGenerator.summaryProvincia(provincia)
    val properties = summary.getClass.getDeclaredFields.foldLeft(Map[String, String]()) { (init, field) =>
      field.setAccessible(true)
      init + (field.getName -> field.get(summary).toString)
    } + ("name" -> name)+ ("departamento" -> nameDepartamento)
    Feature(typeName, properties, geoGeometry)
  }

  def merge(departamento: Departamento, feature: Feature, name: String): Feature = {
    val geoGeometry = feature.geometry
    val typeName = feature.typeName
    val summary = dataGenerator.summaryDepartamento(departamento)
    val properties = summary.getClass.getDeclaredFields.foldLeft(Map[String, String]()) { (init, field) =>
      field.setAccessible(true)
      init + (field.getName -> field.get(summary).toString)
    } + ("name" -> name)
    Feature(typeName, properties, geoGeometry)
  }

  private def getPais = {
    val maybePais = resultCache.getPais()
    assert(maybePais.isDefined)
    maybePais.get
  }


  def writeFile(fileName: String, content: String) = {
    val path = new File(".").getCanonicalPath
    val file: File = new File(path + "/" + fileName)
    logger.info("Path is located [{}]", file.getAbsolutePath)
    if (!file.exists()) {
      file.createNewFile()
    }
    val fw = new FileWriter(file)
    fw.write(content)
    fw.flush()
    fw.close()
  }


}

object GeoJsonParser {
  def main(args: Array[String]): Unit = {
    new GeoJsonParser().parseProvincias()
  }
}
