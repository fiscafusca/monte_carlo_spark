package com.gfisca2

import java.io._

import com.typesafe.config.{Config, ConfigFactory}
import scala.collection.mutable.ListBuffer
import scala.io.Source
import org.slf4j.{Logger, LoggerFactory}

/**
 * Code to extract the tickers names from the SP 500 file.
 */
object GetTickers {

  val config: Config = ConfigFactory.load("config.conf")
  val LOG: Logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {

    val file = config.getString("sim.tickerfile")
    val writer = new BufferedWriter(new FileWriter(file))
    val tickers: ListBuffer[String] = new ListBuffer()

    LOG.info("Starting tickers processing...")

    val bufferedSource = Source.fromFile(config.getString("sim.sp_500"))
    for (line <- bufferedSource.getLines) {

      val cols = line.split(",").map(_.trim)
      val currentTicker = cols(0).replace("\"", "")+','
      tickers += currentTicker

    }

    bufferedSource.close
    tickers.toList
    tickers.foreach(writer.write)
    writer.close()

    LOG.info("Done.")

  }

}