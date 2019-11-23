package com.gfisca2

import java.sql.Date
import java.text.SimpleDateFormat
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.{callUDF, input_file_name}
import org.apache.spark.sql.SparkSession
import org.slf4j.{LoggerFactory, Logger}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Code that performs the Monte Carlo simulation
 */
object Init {

  val config: Config = ConfigFactory.load("config.conf")
  val DATE = "yyyy-MM-dd"
  val toDate = new SimpleDateFormat(DATE)
  val history : ListBuffer[List[String]] = new ListBuffer()
  val local: Int = config.getInt("sim.local")
  val LOG: Logger = LoggerFactory.getLogger(getClass)
  val startDate: String = config.getString("sim.start")
  val endDate: String = config.getString("sim.end")
  val data: String = config.getString("sim.data_path")
  val output_dir: String = config.getString("sim.output_directory")

  /**
   * Main method of the Monte Carlo simulation
   */
  def main(args: Array[String]): Unit = {

    val conf: SparkConf = new SparkConf().setAppName("MonteCarloSimulation")

    if(local == 1)
      conf.setMaster("local")

    val spark: SparkSession = SparkSession.builder.config(conf).getOrCreate()
    val sc: SparkContext = spark.sparkContext

    LOG.info("Starting MonteCarlo simulation for stock portfolio losses.")

    val outputFolderName = config.getString("sim.preprocessing_output")

    spark.udf.register("get_only_file_name", (fullPath: String) => fullPath.split("/").last.replace(".csv", ""))

    // if the parameter "prepr" in the configuration file is set to 0, the preprocessing will be redone
    // prepr is set to 1 by default since the input files provided have already been preprocessed and saved.
    if(config.getInt("sim.prepr") == 0) {

      LOG.info("Preprocessing files...")

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

    LOG.info("Creating the dataframe...")

    val stocksDF = spark.read.format("csv")
      .option("header", "true")
      .load(data)
      .filter($"timestamp".geq(startDate) && $"timestamp".leq(endDate))
      .select("ticker", "timestamp", "adjusted_close")

    // list of distinct dates in the desired time period
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

    LOG.info("Starting simulation...")

    // here we gather all the csv lines outputted by each simulation and save them in a single file
    val output = sc.parallelize(1 to 1000).flatMap(i => play(i, datesList, selectedStocks))

    LOG.info("Saving output files...")

    output.saveAsTextFile(output_dir)

    spark.stop()

    LOG.info("Done!")

  }

  /**
   * Simplified function for portfolio initialization: we select three well known stocks from the list.
   * We suppose that we are playing with three stocks and buying all of them at the beginning (1 share per stock).
   *
   * @param stocks list of chosen stocks in the desired time period
   */
  def initPortfolio(id : Int, stocks : collection.Map[(String, Date), Double]) : Portfolio = {

    LOG.info("Initializing first portfolio...")

    val initStock1 = config.getString("sim.init_stock_1")
    val initStock2 = config.getString("sim.init_stock_2")
    val initStock3 = config.getString("sim.init_stock_3")

    val init : collection.Map[(String, Date), Double] = stocks
      .filter((k: ((String, Date), Double)) => k._1._2 == toDate.parse(config.getString("sim.start")))
      .filter((k:((String, Date), Double)) => k._1._1 == initStock1 || k._1._1 == initStock2 || k._1._1 == initStock3)

    val initStocks = mutable.Map[String, (Date, Double)]()
    init.foreach(k => initStocks.put(k._1._1, (k._1._2, 1.0)))

    val initValue = init.reduce((a, b) => ((a._1._1, a._1._2), a._2 + b._2))

    new Portfolio(id, new Date(toDate.parse(config.getString("sim.start")).getTime), initValue._2, 0.0, initStocks, stocks)

  }

  /**
   * Function that handles buying and selling stocks for the time period defined, generating one portfolio
   * per each day. The portfolios are then added to a list from which the csv lines referring to each day
   * will be extracted at the end of the simulation.
   *
   * @param simId the simulation id
   * @param dates the dates list defining the time period of interest
   * @param stocks the stocks pool
   * @return a list of csv lines referring to one simulation
   */
  def play(simId : Int, dates: List[Date], stocks : collection.Map[(String, Date), Double]): List[String] = {

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
    val csvLinesList = csvLines.toList
    csvLinesList

  }

}
