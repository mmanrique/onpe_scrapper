package com.mmanrique

import com.mmanrique.estructura.Distrito
import org.scalatest.FunSuite

class ResultCacheTest extends FunSuite {

  test("Should append a line to file") {
    val cache = new ResultCache
    cache.appendDistrito(Distrito("d1", "123", Nil))
  }

  test("Should parse lines correctly") {
    val cache = new ResultCache
    val maybeDistrito = cache.getDistrito("123")
    assert(maybeDistrito.isDefined)
  }

  test("Parse should work") {
    val value = "123|{\"name\":\"d1\",\"ubigeo\":\"123\",\"locales\":[]}"
    val strings = value.split("\\|")
    assert(strings.length == 2)
    assert(strings(0) == "123")
  }

}
