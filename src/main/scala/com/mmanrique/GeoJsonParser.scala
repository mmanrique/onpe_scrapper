package com.mmanrique

import java.io.{File, FileWriter}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.mmanrique.estructura.{Departamento, Distrito, Pais, Provincia}
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

  def verifyDistritos(): Unit = {
    val distritos = Source.fromResource("geojson/input/distritos.json").getLines().mkString
    val geoJson = mapper.readValue(distritos, classOf[GeoJson])
    val peruMap = mutable.Map[String, mutable.Map[String, mutable.Map[String, Feature]]]()

    val stringToFeatures: Map[String, List[Feature]] = geoJson.features.groupBy(f => reformat(f.properties("name_1")))
    val departamentoProvinciaDistrito: Map[String, Map[String, List[Feature]]] = stringToFeatures.map(entry => (entry._1, entry._2.groupBy(f => reformat(f.properties("name_2")))))

    val pais: Pais = getPais
    val resultadosMap = generateMapDepartamentos(pais.departamentos)


    val buf = mutable.ListBuffer.empty[Feature]
    departamentoProvinciaDistrito.foreach(e1 => e1._2.foreach(e2 => e2._2.foreach(f => {
      val departamento = resultadosMap(e1._1)
      val provincia = departamento(e2._1)
      val name = reformat(f.properties("name_3"))
      if (provincia.contains(name)) {
        val distrito: Distrito = provincia(name)
        buf += merge(distrito, f, e1._1, e2._1, name)
      } else {
        logger.error("Distrito not found [{}, {}, {}]", e1._1, e2._1, name)
      }
    })))
    val str = mapper.writeValueAsString(GeoJson(geoJson.typeName, buf.toList))
    writeFile("distritos_summary.json", str)

  }

  def reformat(name: String): String = {
    replaceSpecialChars(name.toUpperCase)
  }

  def generateMapDepartamentos(departamentos: List[Departamento]): Map[String, Map[String, Map[String, Distrito]]] = {
    departamentos.map(d => (reformat(d.name), generateMapProvincia(d.provincias))).toMap
  }

  def generateMapProvincia(provincia: List[Provincia]): Map[String, Map[String, Distrito]] = {
    provincia.map(p => (reformat(p.name), generateMapDistrito(p.distritos))).toMap
  }

  def generateMapDistrito(distritos: List[Distrito]): Map[String, Distrito] = {
    distritos.map(d => (reformat(d.name), d)).toMap
  }

  def parseDistritos(): Unit = {
    val found = mutable.Set[String]()
    val distritos = Source.fromResource("geojson/input/distritos.json").getLines().mkString
    val geoJson = mapper.readValue(distritos, classOf[GeoJson])
    val geoJsonDistritos = geoJson.features.map(d => {
      val formattedName = replaceSpecialChars(d.properties("name_3").toUpperCase)
      val formatted_Province_Name = replaceSpecialChars(d.properties("name_2").toUpperCase)
      val key = formatted_Province_Name + "-" + formattedName
      (key, d)
    }).toMap

    val pais: Pais = getPais
    for (departamento <- pais.departamentos) {
      for (provincia <- departamento.provincias) {
        val formattedNameProvincia = replaceSpecialChars(provincia.name.toUpperCase)
        for (distrito <- provincia.distritos) {
          val formattedDistritoName = replaceSpecialChars(distrito.name.toUpperCase)
          val key = formattedNameProvincia + "-" + formattedDistritoName
          if (!geoJsonDistritos.contains(key)) {
            logger.info("Ubicacion not found in GeoJson[{}, {}, {}]", departamento.name, formattedNameProvincia, formattedDistritoName)
          } else {
            found += key
          }
        }
      }
    }
    geoJsonDistritos.toList.sortWith((a, b) => a._2.properties("name_1") < b._2.properties("name_1")).foreach(entry => {
      if (!found.contains(entry._1)) {
        logger.info("GeoJson not found in Resultados [{}, {}, {}]", entry._2.properties("name_1"), entry._2.properties("name_2"), entry._2.properties("name_3"))
      }
    })
  }

  def merge(provincia: Provincia, feature: Feature, name: String, nameDepartamento: String): Feature = {
    val geoGeometry = feature.geometry
    val typeName = feature.typeName
    val summary = dataGenerator.summaryProvincia(provincia)
    val properties = summary.getClass.getDeclaredFields.foldLeft(Map[String, String]()) {
      (init, field) =>
        field.setAccessible(true)
        init + (field.getName -> field.get(summary).toString)
    } + ("name" -> name) + ("departamento" -> nameDepartamento)
    Feature(typeName, properties, geoGeometry)
  }

  def merge(departamento: Departamento, feature: Feature, name: String): Feature = {
    val geoGeometry = feature.geometry
    val typeName = feature.typeName
    val summary = dataGenerator.summaryDepartamento(departamento)
    val properties = summary.getClass.getDeclaredFields.foldLeft(Map[String, String]()) {
      (init, field) =>
        field.setAccessible(true)
        init + (field.getName -> field.get(summary).toString)
    } + ("name" -> name)
    Feature(typeName, properties, geoGeometry)
  }

  def merge(distrito: Distrito, feature: Feature, departamento: String, provincia: String, name: String): Feature = {
    val geoGeometry = feature.geometry
    val typeName = feature.typeName
    val summary = dataGenerator.summaryDistrito(distrito)
    val properties = summary.getClass.getDeclaredFields.foldLeft(Map[String, String]()) {
      (init, field) =>
        field.setAccessible(true)
        init + (field.getName -> field.get(summary).toString)
    } + ("name" -> name)+("departamento" -> departamento)+("provincia" -> provincia)
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
      file.getParentFile.mkdirs()
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
    new GeoJsonParser().verifyDistritos()
  }
}
