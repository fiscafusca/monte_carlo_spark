package com.gfisca2

import java.io.{BufferedWriter, FileWriter}
import java.sql.Date
import java.text.SimpleDateFormat

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.{callUDF, input_file_name}
import org.apache.spark.sql.{Row, SparkSession}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Init {

  val config: Config = ConfigFactory.load("config.conf")
  val DATE = "yyyy-MM-dd"
  val toDate = new SimpleDateFormat(DATE)

  //System.setProperty("hadoop.home.dir", "/")

  def main(args: Array[String]): Unit = {

    val conf: SparkConf = new SparkConf().setAppName("example").setMaster("local")
    val spark: SparkSession = SparkSession.builder.config(conf).getOrCreate()
    val sc: SparkContext = spark.sparkContext

    println("********** START **********")

    val tickers: RDD[String] = spark.sparkContext.textFile("res/tickers.csv").flatMap(e => e.split(','))
    val outputFolderName = "res/processed"

    // DEBUG
    //selected.foreach(e => println(e))

    spark.udf.register("get_only_file_name", (fullPath: String) => fullPath.split("/").last.replace(".csv", ""))

    if(config.getInt("sim.prepr") == 0) {

      val df = spark.read.format("csv")
        .option("header", "true")
        .load("data/*.csv")
        .drop("open", "high", "low", "close", "volume", "dividend_amount", "split_coefficient")
        .withColumn("ticker", callUDF("get_only_file_name", input_file_name()))
        .select("ticker", "timestamp", "adjusted_close")

      df.coalesce(1).write
        .option("header","true")
        .format("com.databricks.spark.csv")
        .save(outputFolderName)

    }

    import spark.implicits._

    val stocksDF = spark.read.format("csv")
      .option("header", "true")
      .load("res/processed/*.csv")
      .filter($"timestamp".geq(config.getString("sim.start")) && $"timestamp".leq(config.getString("sim.end")))
      .select("ticker", "timestamp", "adjusted_close")

    val datesDistinct = stocksDF.select("timestamp").distinct().orderBy($"timestamp".asc).collect().toList

    val datesList = datesDistinct.map(d => new Date(toDate.parse(d.toString().replace("[", "").replace("]", "")).getTime))

    val stocksRDD = stocksDF.rdd
    val stocksRDDMap : RDD[((String, Date), Double)] = stocksRDD.map(row => {
      ((row.getString(0), new Date(toDate.parse(row.getString(1)).getTime)), row.getString(2).toDouble)
    })

    /* Map:
    *  1) String -> ticker
    *  2) Date   -> timestamp
    *  3) Double -> value
    * */
    val selectedStocks: collection.Map[(String, Date), Double] = stocksRDDMap.collect().toMap[(String, Date), Double]

    val ciao = 0

    println("****** CALL *******")

    sc.parallelize(1 to 1000).foreach(i => play(i, datesList, selectedStocks))

    spark.stop()

    println("********** END **********")

  }

  /**
   * Simplified function for portfolio initialization: we select three well known stocks from the list.
   * We suppose that we are playing with three stocks and buying all of them at the beginning (1 share per stock).
   *
   * @param stocks list of chosen stocks in the desired time period
   */
  def initPortfolio(id : Int, stocks : collection.Map[(String, Date), Double]) : Portfolio = {

    val init : collection.Map[(String, Date), Double] = stocks
      .filter((k: ((String, Date), Double)) => k._1._2 == toDate.parse(config.getString("sim.start")))
      .filter((k:((String, Date), Double)) => k._1._1 == "GOOG" || k._1._1 == "AAPL" || k._1._1 == "MSFT")

    val initStocks = mutable.Map[String, (Date, Double)]()
    init.foreach(k => initStocks.put(k._1._1, (k._1._2, 1.0)))

    val initValue = init.reduce((a, b) => ((a._1._1, a._1._2), a._2 + b._2))

    new Portfolio(id, new Date(toDate.parse(config.getString("sim.start")).getTime), initValue._2, 0.0, initStocks, stocks)

  }

  def play(simId : Int, dates: List[Date], stocks : collection.Map[(String, Date), Double]): Unit = {

    val portfolioHistory : ListBuffer[Portfolio] = new ListBuffer()

    dates.foreach(d => {
        if(d.toString == config.getString("sim.start")) {
          val startPortfolio = initPortfolio(simId, stocks)
          portfolioHistory+=startPortfolio
        }
        else {
          val dailyPortfolio = portfolioHistory.last.action(d)
          portfolioHistory += dailyPortfolio
        }
      }
    )

    val csvLines : ListBuffer[String] = new ListBuffer()
    portfolioHistory.foreach(p => csvLines+=p.getCsvLine)
    val outputCsv = new BufferedWriter(new FileWriter("res/output/output_"+simId+".csv"))
    val csvLinesList = csvLines.toList
    csvLinesList.foreach(outputCsv.write)
    outputCsv.close()

  }

}
