package controllers

import com.roundeights.hasher.Implicits._

object Hasher {
  def generateHash(str: String, salt: Boolean): String = {
    if (salt) {
      str.salt("AySe9BbU4JrxnYLIBReKoWRq3vfbN7dG").sha256.hex
    } else {
      return str.sha256.hex
    }
  }

  def generateRandomPassword(length: Option[Int]): String = {
    val rand = new scala.util.Random(System.nanoTime)
    val password = new StringBuilder(length.getOrElse(12))
    val validChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    for (i <- 0 to length.getOrElse(12)) {
      password.append(validChars(rand.nextInt(validChars.length)))
    }
    password.toString
  }
}