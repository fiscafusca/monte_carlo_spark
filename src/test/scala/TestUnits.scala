import java.sql.Date
import java.text.SimpleDateFormat
import com.gfisca2.Portfolio
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.FlatSpec

class TestUnits extends FlatSpec {

  val DATE = "yyyy-MM-dd"
  val config: Config = ConfigFactory.load("testconf.conf")
  val toDate = new SimpleDateFormat(DATE)

  val id: Int = config.getInt("test.id")
  val date: Date = new Date(toDate.parse(config.getString("test.date")).getTime)
  val ownedValue: Double = config.getDouble("test.owned_value")
  val cash: Double = config.getDouble("test.cash")
  val stock1: String = config.getString("test.ticker1")
  val value1: Double = config.getDouble("test.value1")
  val stock2: String = config.getString("test.ticker2")
  val value2: Double = config.getDouble("test.value2")
  val stock3: String = config.getString("test.ticker3")
  val value3: Double = config.getDouble("test.value3")
  val purchaseDate: Date = new Date(toDate.parse(config.getString("test.owned_since")).getTime)
  val shares: Double = config.getDouble("test.shares")
  val purchaseValue1: Double = config.getDouble("test.purchase_val1")
  val purchaseValue2: Double = config.getDouble("test.purchase_val2")
  val purchaseValue3: Double = config.getDouble("test.purchase_val3")

  val testStocks: collection.Map[(String, Date), Double] = Map(
    (stock1, purchaseDate) -> purchaseValue1,
    (stock1, date) -> value1,
    (stock2, date) -> value2,
    (stock3, date) -> value3
  )

  val owned: collection.Map[String, (Date, Double)] = Map(
    stock1 -> (purchaseDate, shares),
    stock2 -> (purchaseDate, shares),
    stock3 -> (purchaseDate, shares)
  )

  val testPortfolio : Portfolio = new Portfolio(id, date, ownedValue, cash, owned, testStocks)

  val tuple: (Date, Double) = (date, ownedValue)
  behavior of "computeOwnedValue() method"
  it should "return " + tuple + " as the tuple of date/owned value for that date" in {
    val testValue = testPortfolio.computeOwnedValue(date, owned)
    assert(testValue == tuple)
  }

  val diff: Double = config.getDouble("test.diff")
  behavior of "getDiff() method"
  it should "return " + diff + " as the percentage difference from purchase value to current value" in {
    val testDiff = testPortfolio.getDiff(stock1, date)
    assert(testDiff == diff)
  }

  behavior of "getOwned() method"
  it should "return " + owned + " as the map of owned stock in the portfolio" in {
    val testOwned = testPortfolio.getOwned
    assert(testOwned == owned)
  }

  val csvLine: String = config.getString("test.csv_line")
  behavior of "getCsvLine() method"
  it should "return " + csvLine + " as the string in csv format displaying the daily value of the portfolio" in {
    val testCsvLine = testPortfolio.getCsvLine
    assert(testCsvLine == csvLine)
  }

  /* the change of values between the dates defined in the config file is not enough to trigger selling,
   * hence we are keeping all the stocks in we already own in the test portfolio
   */
  behavior of "action() method"
  val testStartValue: (Date, Double) = testPortfolio.computeOwnedValue(date, owned)
  it should "return a portfolio value that is equal to " + testStartValue in {
    val newTestPortfolio = testPortfolio.action(date)
    val newPortfolioValue = newTestPortfolio.computeOwnedValue(date, owned)
    assert(newPortfolioValue == testStartValue)
  }

}
