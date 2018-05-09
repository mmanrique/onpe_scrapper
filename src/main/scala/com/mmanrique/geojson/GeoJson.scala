package com.mmanrique.geojson

import com.fasterxml.jackson.annotation.JsonProperty

case class GeoJson(@JsonProperty("type") typeName: String, @JsonProperty("features") features: List[Feature]) {

}
