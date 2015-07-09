package com.alexfiliakov.blackscholescalc.StockModel;

import java.util.Date;

public class HistoricDataPoint {
	/**
	 * This class is used to store historic price data which consists of
	 * Week Start Date, Week's Opening Price, Week's Close Price, Weekly High and Low
	 */
	
	public Date date;
	public double open, close, high, low;
	
	// to remember the order of parameters being passed
	public static HistoricDataPoint addByDOCHL(Date date, double open, double close, double high, double low) {
		return new HistoricDataPoint(date, open, close, high, low);
	}
	
	private HistoricDataPoint(Date date, double open, double close, double high, double low) {
		this.date=date;
		this.open=open;
		this.close=close;
		this.high=high;
		this.low=low;
	}
}
