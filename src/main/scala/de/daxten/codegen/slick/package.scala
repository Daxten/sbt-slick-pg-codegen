package de.daxten.codegen

package object slick {
  implicit class ExtString(s: String) {
    def toCamelCase: String = if (s.isEmpty) s else s.head.toLower + s.tail

    def toUpperCamelCase: String =
      if (s.isEmpty) s else s.head.toUpper + s.tail
  }
}
