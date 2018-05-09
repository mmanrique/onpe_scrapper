package com.mmanrique

class DataRetriever {
  def run(): Unit = {
    val pais = new Collector().colectarDatos()
    println(pprint.tokenize(pais).mkString)

  }
}

object DataRetriever {
  def main(args: Array[String]): Unit = {
    val application = new DataRetriever
    application.run()
  }
}
