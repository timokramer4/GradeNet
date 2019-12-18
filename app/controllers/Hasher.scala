package controllers

import com.roundeights.hasher.Implicits._

object Hasher {
  def generateHash(str: String): String ={
    val hashed = str./*salt("AySe9BbU4JrxnYLIBReKoWRq3vfbN7dG").*/sha256.hex
    return hashed
  }
}
