package com.mmanrique.geojson

import com.fasterxml.jackson.annotation.JsonProperty

case class Feature(@JsonProperty("type") typeName: String, properties: Map[String, String], geometry: GeoGeometry) {

}
