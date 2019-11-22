package com.gfisca2

import java.io._

import com.typesafe.config.{Config, ConfigFactory}
import scalaj.http._
import scala.sys.process._

import scala.collection.mutable.ListBuffer
import scala.io.Source
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object DataFetcher {

  val config: Config = ConfigFactory.load("config.conf")

  def main(args: Array[String]): Unit = {

    System.setProperty("hadoop.home.dir", "/")

    val conf = new SparkConf().setAppName("demo").setMaster("local[2]")
    val spark: SparkSession = SparkSession.builder.config(conf).getOrCreate()

    val file = "res/tickers.csv"
    val writer = new BufferedWriter(new FileWriter(file))

    val tickers : ListBuffer[String] = new ListBuffer()
    val keys : String = config.getString("datafetcher.keys")
    val keyslist : ListBuffer[String] = new ListBuffer()
    keys.split(",").toList.foreach(e => keyslist += e)

    println("********** START **********")

    val rdd = spark.sparkContext.parallelize(Array(1 to 10))
    rdd.count()

    val bufferedSource = Source.fromFile("res/sp_500.csv")
    for (line <- bufferedSource.getLines) {

      val key = keyslist.head
      keyslist += keyslist.head
      keyslist.remove(0)
      val cols = line.split(",").map(_.trim)
      val currentTicker = cols(0).replace("\"","")
      // do whatever you want with the columns here
      tickers += currentTicker

      val src = scala.io.Source
      val response = src.fromURL("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol="+currentTicker+"&outputsize=full&apikey="+key+"&datatype=csv")
      //val response = src.fromURL("https://cloud.iexapis.com/stable/stock/"+currentTicker+"/chart/max?token=pk_75409f384c9248dc9bc23469cfffba3b")

      val content = response.mkString

      val csvFile = "data/" + currentTicker + ".csv"
      val csvWriter = new BufferedWriter(new FileWriter(csvFile))

      csvWriter.write(content)
      writer.close()
      csvWriter.close()

      Thread.sleep(12000)

      /*if(content(0) == '{') {
        println("********** ERROR **********")
        bufferedSource.close()
        return
      }*/

    }

    bufferedSource.close
    //tickers.toList
    //tickers.foreach(writer.write)
    writer.close()

    println("********** END **********")

  }

}
