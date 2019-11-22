package com.gfisca2

import java.sql.Date

import com.gfisca2.Init.{config, toDate}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
 * Portfolio class (simplified, to be updated)
 *
 * @param value current portfolio value of a specific day
 * @param cash cash value of a specific day (nearly 0 for every day => we want to spend all our cash)
 * @param owned stocks owned with date of purchase and shares
 * @param stocks list of chosen stocks in the desired time period
 */
class Portfolio(val id : Int, val date : Date, val value : Double, val cash : Double, val owned : collection.Map[String, (Date, Double)], val stocks : collection.Map[(String, Date), Double]) {

  val stopLoss: Double = -0.1
  val gainPlateaued : Double = 0.1

  /**
   * Computes the portfolio value for a specific day
   *
   * @param date the date of the day we want to compute the value of
   * @return a tuple (Date, Double) which indicates the day and the portfolio value
   */
  def computeOwnedValue(date : Date, ownedStocks : collection.Map[String, (Date, Double)]): (Date, Double) = {

    if(ownedStocks.isEmpty)
      return (date, 0.0)

    val values : ListBuffer[(Date, Double)] = new ListBuffer()

    ownedStocks.foreach(el => {
      values+=((date, stocks.get(el._1, date).get * el._2._2))
    })

    val dayValue : (Date, Double) = values.reduce((a, b) => (a._1, a._2 + b._2))

    dayValue

  }

  def action(date : Date): Portfolio = {

    val toSell : ListBuffer[(String, Double)] = new ListBuffer()

    owned.foreach(s => {
      //todo: add gain plateau case
      if(getDiff(s._1, date) <= stopLoss || (getDiff(s._1, date) >= 0 && getDiff(s._1, date) <= gainPlateaued))
        toSell+=((s._1, s._2._2))
    })

    val toSellMap = toSell.toMap
    // we assume that we always sell all our shares for each stock

    if(toSellMap.isEmpty)
      return new Portfolio(id+1, date, computeOwnedValue(date, owned)._2, cash, owned, stocks)

    val ownedAfterSell = owned.filter(s => s._2._2 != toSellMap.getOrElse(s._1, 0.0))
    val sold = owned.filter(s => s._2._2 == toSellMap.getOrElse(s._1, 0.0))
    val cashAfterSelling : Double = cash + computeOwnedValue(date, sold)._2

    //todo: sell and buy together as a single action, we always buy and sell together!

    // since we want to invest all our cash, selling is always followed by a purchase
    // we assume that we buy the same amount of stocks that we sold (e.g. if we sold 3 stocks, we buy 3 stocks)
    val toBuyNum = toSell.length
    val cashPerStock = cashAfterSelling/toBuyNum
    val toBuy : ListBuffer[(String, (Date, Double))] = new ListBuffer()
    val todayStocks = stocks.filter(s => s._1._2 == date)
    val keys = todayStocks.keys.toList

    // pick N = toBuyNum random stocks from the dataset
    0.until(toBuyNum).foreach(i => {
        val rand = new Random()
        val randPick: (String, Date) = keys(rand.nextInt(keys.size))
        val newStockValue : Double = stocks.getOrElse(randPick, 0.0)
        toBuy+=((randPick._1, (date, cashPerStock/newStockValue)))
      }
    )

    val toBuyMap = toBuy.toMap
    val newOwned = ownedAfterSell++toBuyMap
    val cashSpent = computeOwnedValue(date, toBuyMap)._2
    val newValue = computeOwnedValue(date, newOwned)._2
    val newCash = cashAfterSelling - cashSpent

    new Portfolio(id+1, date, newValue, newCash, newOwned, stocks)

  }

  def getDiff(ticker : String, date : Date): Double = {

    val start : Double = stocks.getOrElse((ticker, owned(ticker)._1), 0.0)
    val today : Double = stocks.getOrElse((ticker, date), 0.0)
    val diff = today - start
    val diffProportion = (diff/start)
    diffProportion

  }

  def getOwned: collection.Map[String, (Date, Double)] = {
    owned
  }

  def getCsvLine: String = {
    id + "," + date.toString + "," + value.toString + "\n"
  }

}

