# CS441 - Engineering of Distributed Objects for Cloud Computing: homework 2
### Giorgia Fiscaletti, UIN: 669755907

## Introduction

The purpose of this homework was to develop a Spark program for parallel processing of the predictive engine for stock portfolio losses using Monte Carlo simulation in Spark.
This project is fully developed in Scala and can be compiled using SBT. The application can be deployed and run locally, on an Hortonworks Virtual Machine and on the Google Computing Engine (GCE).
 
## Instructions

### IntelliJ IDEA

### IntelliJ IDEA (to run the application locally on your machine)

- Open IntelliJ IDEA and select "Check out from Version Control" in the welcome screen
- Select "Git"
- Enter the repository URL (below), click on "Clone" and confirm when asked:
```
https://giorgiafiscaletti2@bitbucket.org/giorgiafiscaletti2/giorgia_fiscaletti_hw3.git
```
- A window for SBT import will appear: leave the default settings and confirm
- Go in /giorgia_fiscaletti_hw3/src/main/resources/config.conf and change local from 0 to 1
- Go in /giorgia_fiscaletti_hw3/src/main/scala/com/gfisca2 and run Init.scala

Otherwise:

- Open a terminal and type:
```
git clone https://giorgiafiscaletti2@bitbucket.org/giorgiafiscaletti2/giorgia_fiscaletti_hw3.git
```
- Open IntelliJ IDEA and select "Import project" in the welcome screen
- Select the project folder
- Choose "Import project from external model", select sbt and click on "Next"
- Leave the default SBT configuration and confirm
- Go in /giorgia_fiscaletti_hw3/src/main/resources/config.conf and change local from 0 to 1
- Go in /giorgia_fiscaletti_hw3/src/main/scala/com/gfisca2 and run Init.scala

### SBT CLI (to run the application locally on your machine)

- Open a terminal and type:
```
git clone https://giorgiafiscaletti2@bitbucket.org/giorgiafiscaletti2/giorgia_fiscaletti_hw3.git
```
- To run the tests, type the following command:
```
sbt clean compile test
```
- Go in /giorgia_fiscaletti_hw3/src/main/resources/config.conf and change local from 0 to 1
- To run the Monte Carlo simulation, type the following command:
```
sbt clean compile run
```

## Project Structure

As mentioned above, the project has been fully developed in Scala. A configuration file is provided for the main tasks and for the tests.

### Utils

There are two "utils" scala objects that were used to get the data needed for the computation.

#### DataFetcher and GetTickers

- *DataFetcher:* an object that provides the code to download the stock data from the Alpha Vantage.
- *GetTickers:* an object that retrieves the tickers list from the SP500 file.

### Portfolio

Portfolio is the most important class of the application. It defines the daily portfolios with all the information needed for
the simulations. 

#### Parameters:

- *id:* the id of the simulation to which the portfolio belongs;
- *date:* the date of the portfolio;
- *value:* the portfolio value based on the owned stocks;
- *cash:* the cash value;
- *owned:* a map containing the owned stocks, alongside their purchase date and the shares owned;
- *stocks:* the map containing all the stock values for a desired time period.

#### Methods: 

- *computeOwnedValue:* method that computes the portfolio value for a specific day, based on the owned stocks.
It collects the values of the owned stocks for the specific day from the stocks dataset, multiplying it with the 
value of shares owned (in the "owned" map). It then reduces the values in the list summing them up, and returns 
a tuple (day, value).
- *action:* one of the core operation of the Monte Carlo simulation, this method defines the single (and partially random) 
action to be done for each day in the given time period. Firstly, it checks if the stocks owned have reached a stop loss
value, or if their gain experienced a plateau in the time that elapsed from the day of the purchase and the current date
(this last check is simplified and just illustrative). If that is the case, the stock(s) is inserted in a list and is 
then sold. After the selling, the new cash value is computed. It was assumed that a new stock is bought for each stock sold
(e.g. if 2 stocks are sold, then the simulator will buy 2 stocks). The new cash is divided in equal parts depending on the
stocks to buy. Then, the new stocks are picked randomly from the stocks pool and the shares are computed depending on 
the amount of cash assigned to each new stock. Finally, the owned map, cash and value are updated and a the new portfolio
for the new day is created. If there is no stop loss or gain plateau, no action is performed, and the method returns a new
portfolio with just the updated value of the new day.
- *getDiff:* method to compute the percentage difference of the value of a given stock from the purchase date to
the given date.
- *getOwned:* getter method that returns the owned map.
- *getCsvLine:* method that builds a string in csv format that will be a line in the output file.

### Init

### Tests

- TestUnits: a file to test some of the methods provided in the various map/reduce classes:
  
    - getBin(): to check if the bin for the number of co-authors computed by the function is correct;
    - getYearsBin2(): to check if the bin for the years computed by the function is correct;
    - computeAvg(): to check if the average value computed for the authors statistics is correct;
    - computeMax(): to check if the max value computed for the authors statistics is correct;
    - computeMedian(): to check if the median value computed for the authors statistics is correct.

### Configuration files

A simple configuration file config.conf is provided for the main tasks in order to have all the paths, filenames, tags and other variable in the same file, allowing to change the values easily if needed.
Another configuration file is provided for the tests.

## Hortonworks Sandbox

The following steps illustrate how to run the code on the Hortonworks Sandbox, before moving to the AWS cloud. 
The Hortonworks Sandbox offer all the services needed to furtherly test the code locally, including Hadoop, YARN and HDFS filesystem.

### Set up

- First off, download an hypervisor (e.g. [VMWare](https://www.vmware.com/ "VMWare Homepage"))
- Download the [Hortonworks Sandbox](https://www.cloudera.com/downloads/hortonworks-sandbox.html), selecting Hortonworks HDP
- Set up the virtual machine (click [here](https://www.cloudera.com/tutorials/learning-the-ropes-of-the-hdp-sandbox.html) for the Cloudera tutorial)
- Once the set up is completed, you will see the following screen, showing the IP address for the welcome screen and SSH: 
![Alt text](histograms/hortonworks.png)
- Git clone the repository following the same steps listed for the SBT CLI and go to the project root folder
- Assembly the project and generate the .jar file by typing:
```
sbt clean assembly
```

### Log in

- Copy the SSH IP address for your specific hypervisor and type:
```
ssh -p 2222 root@{HDP-IP-Address}
```
- The default password is **hadoop**, you will be required to change it after the first login

### Spark steps

- Upload the input folder (res) to the Hortonworks Sandbox via scp:
```
scp -P 2222 ${res_LOCAL_PATH} root@{HDP-IP-Address}:${res_REMOTE_PATH}
```
- Upload the input folder to the HDFS filesystem:
```
hdfs dfs -put ${res_REMOTE_PATH} res
```
- Upload the assembled .jar file to the Hortonworks Sandbox:
```
scp -P 2222 $JAR_PATH root@{HDP-IP-Address}:$JAR_REMOTE_PATH
```
- Run the Monte Carlo simulations:
```
spark-submit --deploy-mode cluster montecarlo.jar ${res_REMOTE_PATH} output_dir
```
Wait for the Spark application to complete execution...

To see the progress of the Spark application, follow the link that is printed to the terminal as soon as the computation starts.

### Get the results

- SSH to the Hortonworks Sandbox:
```
ssh -p 2222 root@{HDP-IP-Address}
```
- Merge and retrieve the results from the HDFS filesystem:
```
hdfs dfs -getmerge <remote_output_dir> <local_output_dir>
```

Please note that to repeat the computation you need to remove the remote output directory. To do so, type:
```
hdfs dfs -rm -r output_dir
```

## GCE (Google Computing Engine) deployment

The steps on GCE are very similar to the ones for the Hortonworks Sandbox. 


## Results


