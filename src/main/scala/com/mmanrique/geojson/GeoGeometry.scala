package com.mmanrique.geojson

import com.fasterxml.jackson.annotation.JsonProperty

case class GeoGeometry(@JsonProperty("type") typeName: String, coordinates: List[List[List[List[Double]]]]) {

}
