package com.mmanrique.resultados.segundavuelta

import com.mmanrique.resultados.Resultado

case class SegundaVuelta2016(ppk: Int = 0, fp: Int = 0, blancos: Int = 0, nulos: Int = 0, impugnados: Int = 0, total: Int = 0) extends Resultado {


  def +(other: SegundaVuelta2016): SegundaVuelta2016 = {
    SegundaVuelta2016(ppk + other.ppk, fp + other.fp, blancos + other.blancos, nulos + other.nulos, impugnados + other.impugnados, total + other.total)
  }

}
