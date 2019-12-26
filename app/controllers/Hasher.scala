package controllers

import com.roundeights.hasher.Implicits._
import scala.util.Random

object Hasher {
  def generateHash(str: String, salt: Boolean): String = {
    if(salt){
      str.salt("AySe9BbU4JrxnYLIBReKoWRq3vfbN7dG").sha256.hex
    } else {
      return str.sha256.hex
    }
  }

    def generateRandomPassword(): String = {
    var r: Random = Random
    var result: String = ""
    for(i <- 0 to 12){
      r = Random
      result += r.nextPrintableChar()
    }
    return result
  }
}