package com.gfisca2

import java.io._

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.mutable.ListBuffer
import scala.io.Source
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object GetTickers {

  val config: Config = ConfigFactory.load("config.conf")

  def main(args: Array[String]): Unit = {

    System.setProperty("hadoop.home.dir", "/")

    val conf = new SparkConf().setAppName("demo").setMaster("local[2]")
    val spark: SparkSession = SparkSession.builder.config(conf).getOrCreate()

    val file = "res/tickers.csv"
    val writer = new BufferedWriter(new FileWriter(file))

    val tickers: ListBuffer[String] = new ListBuffer()
    val keys: String = config.getString("datafetcher.keys")
    val keyslist: ListBuffer[String] = new ListBuffer()
    keys.split(",").toList.foreach(e => keyslist += e)

    println("********** START **********")

    val rdd = spark.sparkContext.parallelize(Array(1 to 10))
    rdd.count()

    val bufferedSource = Source.fromFile("res/sp_500.csv")
    for (line <- bufferedSource.getLines) {

      val cols = line.split(",").map(_.trim)
      val currentTicker = cols(0).replace("\"", "")+','
      tickers += currentTicker

    }

    bufferedSource.close
    tickers.toList
    tickers.foreach(writer.write)
    writer.close()

    println("********** END **********")

  }

}