package com.alexfiliakov.blackscholescalc.StockModel;

import java.util.Date;

public class PredictedDataPoint {
	/**
	 * This class is used to store predicted stock data which consists of
	 * Week Start Date, Expected Close Price, and 95% Confidence Interval Bounds 
	 */
	
	public Date date;
	public double expected,upperBound,lowerBound;
	
	// to remember the order of parameters being passed
	public static PredictedDataPoint addByDEUL(Date date, double expected, double upperBound, double lowerBound){
		return new PredictedDataPoint(date, expected, upperBound, lowerBound);
	}
	
	private PredictedDataPoint(Date date, double expected, double upperBound, double lowerBound) {
		this.date=date;
		this.expected=expected;
		this.upperBound=upperBound;
		this.lowerBound=lowerBound;
	}
}
