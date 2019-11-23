package com.gfisca2

import java.io._

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}
import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
 * Sample code to get the stock data from the API (Alpha Vantage).
 */
object DataFetcher {

  val config: Config = ConfigFactory.load("config.conf")
  val LOG: Logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {

    val keys : String = config.getString("datafetcher.keys")
    val keyslist : ListBuffer[String] = new ListBuffer()
    keys.split(",").toList.foreach(e => keyslist += e)

    LOG.info("Starting data fetch from Alpha Vantage...")

    val bufferedSource = Source.fromFile(config.getString("sim.sp_500"))
    for (line <- bufferedSource.getLines) {

      val key = keyslist.head
      keyslist += keyslist.head
      keyslist.remove(0)
      val cols = line.split(",").map(_.trim)
      val currentTicker = cols(0).replace("\"","")
      val src = scala.io.Source
      val response = src.fromURL("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol="+currentTicker+"&outputsize=full&apikey="+key+"&datatype=csv")

      val content = response.mkString

      val csvFile = "data/" + currentTicker + ".csv"
      val csvWriter = new BufferedWriter(new FileWriter(csvFile))

      csvWriter.write(content)
      csvWriter.close()

      Thread.sleep(12000)

    }

    bufferedSource.close

    LOG.info("Done.")

  }

}
