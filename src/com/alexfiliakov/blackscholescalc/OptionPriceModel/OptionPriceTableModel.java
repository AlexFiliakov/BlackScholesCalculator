package com.alexfiliakov.blackscholescalc.OptionPriceModel;

import javafx.scene.control.TableColumn;

import javax.management.monitor.Monitor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.apache.commons.math3.distribution.NormalDistribution;

import com.alexfiliakov.blackscholescalc.StockModel.StockModel;
import com.sun.xml.internal.ws.api.Component;

import sun.net.www.content.text.plain;
import sun.swing.table.DefaultTableCellHeaderRenderer;
import sun.util.spi.CalendarProvider;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.math.RoundingMode;
import java.text.DecimalFormat;


public class OptionPriceTableModel {
	/**
	 * This class generates the table of option prices for the underlying stock
	 */
	
	final private static String[] COLUMN_NAMES = {"Expiration", "Strike", "Call Price", "Put Price"};
	final private static int TRADING_DAYS_IN_YEAR = 252;
	
	private TableModel tableModel; //holds option prices
	
	// creates the price table data
	public OptionPriceTableModel(StockModel stockModel) throws Exception {
		// Black-Scholes is calculated in years, make sure all units are in years!
		double riskFreeRate = getRiskFreeRate();
		double riskFreeForce = Math.log(1+riskFreeRate/100);
		double spotPrice = getSpotPrice(stockModel.getStockSymbol()); // get asset price (last close price)
		double annualVolatility = stockModel.getAnnualVolatility();
		
		Calendar[] expirationDays = getExpirationDays(); // list of expiration days to calculate
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		
		// calculate option prices at Strike Prices based on weekly SD deviation in returns
		double[] strikePricesArr = new double [7];
		for (int i=-3; i<=3; i++) {
			strikePricesArr[i+3] = spotPrice * Math.exp(i*annualVolatility/Math.sqrt(52));
		}
		
		String[][] rowData = new String[expirationDays.length * strikePricesArr.length][COLUMN_NAMES.length];

		for (int expDayIndex=0; expDayIndex < expirationDays.length; expDayIndex++) {
			for (int strikePriceIndex=0; strikePriceIndex < strikePricesArr.length; strikePriceIndex++) {
				int rowDataIndex = expDayIndex*strikePricesArr.length + strikePriceIndex; // index of the data row (getting multiple Strike Prices for each Expiration Day)
				rowData[rowDataIndex][0] = sdf.format(expirationDays[expDayIndex].getTime());

				// set to format numbers to 2 decimals for strike prices
			    DecimalFormat format = new DecimalFormat();
			    format.setRoundingMode(RoundingMode.HALF_EVEN);
			    format.setMaximumFractionDigits(2);
			    format.setMinimumFractionDigits(2);
				
				rowData[rowDataIndex][1] = format.format(strikePricesArr[strikePriceIndex]) + " ";
				
				// set 4 decimals for option prices
			    format.setMaximumFractionDigits(4);
			    format.setMinimumFractionDigits(4);
			    
			    double businessYearsToDate = ((double)businessDaysToDate(expirationDays[expDayIndex]))/TRADING_DAYS_IN_YEAR;
			    double callOptionPrice, putOptionPrice;
			    callOptionPrice = getCallOptionPrice(spotPrice, strikePricesArr[strikePriceIndex], riskFreeForce, annualVolatility, businessYearsToDate);
			    putOptionPrice = getPutOptionPrice(spotPrice, strikePricesArr[strikePriceIndex], riskFreeForce, annualVolatility, businessYearsToDate);
			    
				rowData[rowDataIndex][2] = format.format(callOptionPrice) + " ";
				rowData[rowDataIndex][3] = format.format(putOptionPrice) + " ";
			}
		}
		
		tableModel = new DefaultTableModel(rowData, COLUMN_NAMES);
	}
	
	public TableModel getTableModel() {
		return tableModel;
	}
	
    /*
     * 
     *    Helper Methods
     * 
     */
	
    /*
     * This method gets the last trade price (spot price) using Yahoo API
     * https://greenido.wordpress.com/2009/12/22/yahoo-finance-hidden-api/
     */
    private double getSpotPrice (String stockSymbol) throws Exception {
    	double spotPrice;
		URL url = null;
		URLConnection urlConn = null;
		InputStreamReader inStream = null;
		BufferedReader br = null;

		/*
		 * This returns a CSV file with a single cell indicating the current price
		 */
		try {
			url = new URL("http://finance.yahoo.com/d/quotes.csv?s="
					+ stockSymbol
					+ "&f=l1"); // l1 indicates the Last Trade price
			urlConn = url.openConnection();
			inStream = new InputStreamReader(urlConn.getInputStream());
			br = new BufferedReader(inStream);

			spotPrice = Double.parseDouble(br.readLine());
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
		
		return spotPrice;
    }
    
    /*
     * This method gets the risk-free rate using the 10-yr treasury rate
   	 * 	Partial solution is: take current 10-yr treasury rate, subtract 1% historical risk premium
     * 	Ref: http://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAA&url=http%3A%2F%2Fsom.yale.edu%2F~spiegel%2Fmgt541%2FLectures%2FEstimatingCAPMInputs.ppt&ei=AvSRVYS4MImt-AHd7LjgBg&usg=AFQjCNFSKQUQmq8wGdCUfXUgX8YYyPX6mQ&sig2=11oTNuIAtqBSYVRk7iktqQ&bvm=bv.96783405,d.cWw
     */
    private double getRiskFreeRate() throws Exception {
    	double tenYearTNotePrice = getSpotPrice("^TNX"); // get the 10-yr T-Note Price
    	return tenYearTNotePrice - 1.;
    }
    
    private Calendar[] getExpirationDays() {
    	ArrayList<Calendar> expirationDaysList = new ArrayList<Calendar>();
    	
    	Calendar today = Calendar.getInstance();
    	today.setFirstDayOfWeek(Calendar.MONDAY);
    	// need to get today's close day (weekend = Friday, otherwise previous day)
    	
    	Calendar todayClosingDay = (Calendar)today.clone();  //todo: before 4pm EST, closing is still today
    	
    	Calendar expirationDayToAdd = (Calendar)todayClosingDay.clone();
    	switch(todayClosingDay.get(Calendar.DAY_OF_WEEK)) {
    	case Calendar.MONDAY:
    		expirationDayToAdd.add(Calendar.DATE, 4);
    		break;
    	case Calendar.TUESDAY:
    		expirationDayToAdd.add(Calendar.DATE, 3);
    		break;
    	case Calendar.WEDNESDAY:
    		expirationDayToAdd.add(Calendar.DATE, 2);
    		break;
    	case Calendar.THURSDAY:
    		expirationDayToAdd.add(Calendar.DATE, 1);
    		break;
    	case Calendar.FRIDAY:
    		expirationDayToAdd.add(Calendar.DATE, 7);
    		break;
    	case Calendar.SATURDAY:
    		expirationDayToAdd.add(Calendar.DATE, 6);
    		break;
    	case Calendar.SUNDAY:
    		expirationDayToAdd.add(Calendar.DATE, 5);
    		break;
    	}
    	
    	// add 8 weeks
    	expirationDaysList.add((Calendar)expirationDayToAdd.clone());
    	
    	for (int i=0; i<7; i++) {
	    	expirationDayToAdd.add(Calendar.DATE, 7);
	    	expirationDaysList.add((Calendar)expirationDayToAdd.clone());
    	}
    	
    	// add 8 quarters (13-week periods)
    	expirationDayToAdd.add(Calendar.DATE,(52/4-8)*7);
    	expirationDaysList.add((Calendar)expirationDayToAdd.clone());
    	
    	for (int i=0; i<7; i++) {
	    	expirationDayToAdd.add(Calendar.DATE, 52/4*7);
	    	expirationDaysList.add((Calendar)expirationDayToAdd.clone());
    	}
    	
    	Calendar[] expirationDaysArr = new Calendar[expirationDaysList.size()];
    	expirationDaysArr = expirationDaysList.toArray(expirationDaysArr);
    	
    	return expirationDaysArr;
    }
    
    private int businessDaysToDate(Calendar date) {
    	// calculates business days until close of the date (excluding today)
    	
    	//TODO: account for holidays (or use TRADING_DAYS_IN_A_YEAR as approx)
    	
    	int businessDayCount = 0;
    	
    	Calendar today = Calendar.getInstance();
    	while (date.getTimeInMillis()-today.getTimeInMillis() > 0) {
    		today.add(Calendar.DATE, 1);
    		switch (today.get(Calendar.DAY_OF_WEEK)) {
    		case Calendar.SATURDAY:
    		case Calendar.SUNDAY:
    			break;
			default:
    			businessDayCount++; // only increment on weekday
    			break;
    		}
    	}
    	
    	return businessDayCount;
    }
    
    // Black-Scholes Call Options formula
    private double getCallOptionPrice(double spotPrice, double strikePrice, double riskFreeForce, double annualVolatility, double timeInYears) {
    	double d1, d2, Nd1, Nd2, callOptionPrice;
    	
    	final NormalDistribution normalDistribution = new NormalDistribution();
    	
    	d1 = (Math.log(spotPrice/strikePrice) + (riskFreeForce + annualVolatility*annualVolatility/2)*timeInYears);
    	d1 /= annualVolatility*Math.sqrt(timeInYears);
    	
    	d2 = d1 - annualVolatility*Math.sqrt(timeInYears);
    	
    	Nd1 = normalDistribution.cumulativeProbability(d1);
    	Nd2 = normalDistribution.cumulativeProbability(d2);
    	
    	callOptionPrice = Nd1*spotPrice - Nd2*strikePrice*Math.exp(-riskFreeForce*timeInYears);

    	return callOptionPrice;
    }
    
    // use Put-Call Parity to get the Put option from the corresponding Call option
    private double getPutOptionPrice(double spotPrice, double strikePrice, double riskFreeForce, double annualVolatility, double timeInYears) {
    	double callOptionPrice = getCallOptionPrice(spotPrice, strikePrice, riskFreeForce, annualVolatility, timeInYears);
    	
    	double putOptionPrice = callOptionPrice - spotPrice + strikePrice*Math.exp(-riskFreeForce*timeInYears);
    	
    	return putOptionPrice;
    }
    
	// formats the representing JTable
    public static void formatTable(JTable table) {
		DefaultTableCellHeaderRenderer headerRenderer = new DefaultTableCellHeaderRenderer();
		headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
		
		for (int i = 0; i < COLUMN_NAMES.length ; i++) {
			table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
			table.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
		}
		
		table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
		
		table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
	}
}