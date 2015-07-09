
package com.alexfiliakov.blackscholescalc.StockModel;

import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.Comparator;

public class StockModel {
	/*
	 * This class predicts stock prices based on the Lognormal Stock Model
	 * underlying the Black-Scholes Model of European-style option pricing
	 * (Disclaimer: its validity is highly disputed and this is not intended
	 * to be used for financial predictions)
	 * 
	 * The model uses two parameters: mean and volatility.
	 * These are estimated from historic data, and then used to generate predicted values.
	 */
	
	// ICNPF_VAL aka Z-Score stands for "inverse cumulative normal probability function value"
	// it is the factor of volatility to use in calculating upper/lower bounds of
	// our prediction to construct a 95% confidence interval around the expected value
	private final double ICNPF_VAL = -1.959963985;
	
	private String stockSymbol;
	
	// lognormal model parameters
	private double annualMean, annualVolatility;
	
	// stores up to 1 year of data, but may be less for new stocks
	private ArrayList<HistoricDataPoint> historicData = new ArrayList<HistoricDataPoint>();

	// stores weekly price predictions for 1 year
	private PredictedDataPoint[] predictedData = new PredictedDataPoint[52];
	
	public StockModel (String symbol) throws Exception {
		stockSymbol = symbol;
		try {
			retrieveHistoricData();
			validateAndCleanData();
			calculateModelParameters();
			calculatePredictions();
		} catch (Exception e) {
			throw e;
		}
	}
	
	// populate stock data from Yahoo Finance
	private void retrieveHistoricData() throws Exception {
		String csvDataPoint;
		URL url = null;
		URLConnection urlConn = null;
		InputStreamReader inStream = null;
		BufferedReader br = null;

		/*
		 * This returns a CSV file in the following format:
		 *   Date, Open, High, Low, Close, Volume, Adj Close
		 * If an invalid symbol is provided, loads the 404 page
		 */
		
		try {
			url = new URL("http://ichart.yahoo.com/table.csv?"
					+ "s=" + stockSymbol // must be a valid symbol, otherwise gives error page
					+ "&g=w&ignore=.csv"); // g is the interval to use (w for weeks)
			urlConn = url.openConnection();
			inStream = new InputStreamReader(urlConn.getInputStream());
			br = new BufferedReader(inStream);
			
			// make sure CSV has a valid header
			String headerLine = br.readLine();
			if (headerLine.compareTo("Date,Open,High,Low,Close,Volume,Adj Close") != 0) {
				throw new Exception("File header not as expected. Output:" + headerLine);
			}
	
			// parse stock data into historicData collection, store up to 52 values
			int weeksParsed = 0;
			while ((csvDataPoint = br.readLine()) != null && weeksParsed < 52) {
				// parse csv string
					StringTokenizer tokenizer = new StringTokenizer(csvDataPoint,",");
					Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(tokenizer.nextToken());
					double open = Double.parseDouble(tokenizer.nextToken());
					double high = Double.parseDouble(tokenizer.nextToken());
					double low = Double.parseDouble(tokenizer.nextToken());
					double close = Double.parseDouble(tokenizer.nextToken());
					
				historicData.add(HistoricDataPoint.addByDOCHL(date, open, close, high, low));
				weeksParsed++;
			}
			if (historicData.size()<10) {
				throw new Exception("Not enough data to model");
			}
		} catch (MalformedURLException e) {
			throw new Exception("Problem retrieving stock data");
		} catch (IOException e) {
			throw new Exception("Problem retrieving stock data");
		} catch (Exception e) {
			throw e;
		} finally {
			if (inStream != null)
				inStream.close();
			if (br != null)
				br.close();
		}
	}
	
	// makes sure all dates are one week apart
	// also orders the collection and does minor date cleanup
	private void validateAndCleanData() throws Exception {
		// sort collection by date
		Collections.sort(historicData,new Comparator<HistoricDataPoint>() {
			public int compare(HistoricDataPoint point1, HistoricDataPoint point2) {
				return point1.date.compareTo(point2.date);
			}
		});
		
		// Need to make sure dates are 1 week apart, otherwise missing data
		// Loop through the collection, make sure each date is 1 week from last
		// Otherwise throw an error
		Calendar dateToCheck = Calendar.getInstance();
		dateToCheck.setTime(historicData.get(0).date);

		// if earliest day is not Monday (=1), that means it's an IPO
		if (dateToCheck.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
			// throw out this data point because it represents less than 1 week's growth
			historicData.remove(0);
			dateToCheck.setTime(historicData.get(0).date);
		}
		
		for(HistoricDataPoint dataPoint : historicData) {
			// Calendar provides methods to add and subtract days
			Calendar cal = Calendar.getInstance();
			cal.setTime(dataPoint.date);
			
			// if Monday is a holiday, the market is closed and Yahoo reports week start as the next opening day
			// so we force our collection to store the corresponding Monday instead to make it easier to test data integrity
			if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
				cal.add(Calendar.DATE,Calendar.MONDAY-cal.get(Calendar.DAY_OF_WEEK));
				dataPoint.date = new Date(cal.getTimeInMillis());
			}

			if (dateToCheck.compareTo(cal) != 0) {
				throw new Exception ("Data is missing, can't continue. Expected: " + dateToCheck.toString() + "; Actual: " + dataPoint.date.toString());
			}

			// Increment by 7 days to test next data point
			cal.add(Calendar.DATE,7);
			dateToCheck=cal;
		}
	}
	
	// calculate model parameters based on historic data
	private void calculateModelParameters() {
		//retrieveHistoricData orders by date and requires weekly sequences, so we can assume perfect chronological order
		int sampleSize = historicData.size();
		int df = sampleSize-1; // degrees of freedom
		double weeklyMean = Math.log(historicData.get(sampleSize-1).close / historicData.get(0).close) / df;
		double weeklyVolatility = 0;
		// calculating volatility requires summing fractions of consecutive weekly closing prices, so iterator starts at 1
		double sumOfSquares = 0;
		for (int i=1;i<sampleSize;i++) {
			sumOfSquares+=Math.pow(Math.log(historicData.get(i).close/historicData.get(i-1).close), 2);
		}
		weeklyVolatility=Math.sqrt((sumOfSquares/df - Math.pow(weeklyMean,2))*df/(df-1));
		
		annualMean = 52*weeklyMean;
		annualVolatility = Math.sqrt(52)*weeklyVolatility;
	}
	
	// calculate predicted weekly close prices for the next year based on parameters
	private void calculatePredictions() {
		// retrieveHistoricData orders by date, so we can assume last element is the latest close price
		// lognormal model basically predicts future prices as fractions of last close price 
		HistoricDataPoint lastDataPoint = historicData.get(historicData.size()-1);
		
		for(int week=0;week<52;week++) {
			// Add 7 days to the last close date for every week into the future
			// Calendar provides methods for this
			Calendar cal = Calendar.getInstance();
			cal.setTime(lastDataPoint.date);
			cal.add(Calendar.DATE,7*(week+1));
			Date futureDate = new Date(cal.getTimeInMillis());

			double expectedClose = lastDataPoint.close * Math.exp(annualMean * (week+1)/52f);
			double expectedLowerBound = expectedClose * Math.exp(-0.5 * Math.pow(annualVolatility, 2) * (week+1)/52f
																 + ICNPF_VAL * annualVolatility * Math.sqrt((week+1) / 52f));
			double expectedUpperBound = expectedClose * Math.exp(-0.5 * Math.pow(annualVolatility, 2) * (week+1)/52f
					 											 - ICNPF_VAL * annualVolatility * Math.sqrt((week+1) / 52f));
			predictedData[week] = PredictedDataPoint.addByDEUL(futureDate, expectedClose, expectedUpperBound, expectedLowerBound);
		}
	}

	public String getStockSymbol() { return stockSymbol; }
	public double getAnnualMean() { return annualMean; }
	public double getAnnualVolatility() { return annualVolatility; }
	public ArrayList<HistoricDataPoint> getHistoricData() { return historicData; }
	public PredictedDataPoint[] getPredictedData() { return predictedData; }
}
