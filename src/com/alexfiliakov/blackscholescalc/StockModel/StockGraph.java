package com.alexfiliakov.blackscholescalc.StockModel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import javax.swing.JPanel;


public class StockGraph extends JPanel {
	/**
	 * This class generates graph of the stock
	 */
	
	// constants defining graph dimension and position
	private final int CANDLE_WIDTH = 5; // width of individual candle plot
	private final int CANDLE_SPACING = 2; // space between candles
	private final int GRAPH_X = 20;
	private final int GRAPH_Y = 20;
	private final int GRAPH_W = (CANDLE_WIDTH+CANDLE_SPACING)*52*2;
	private final int GRAPH_H = 400;
	
	// global extrema to scale all prices to chart
	private double globalHigh;
	private double globalLow;
	
	private ArrayList<HistoricDataPoint> historicData;
	private PredictedDataPoint[] predictedData;
	
    public StockGraph(StockModel stockModel) {
    	super();
    	historicData = stockModel.getHistoricData();
    	predictedData = stockModel.getPredictedData();
    	
    	findExtrema();
    }
	
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D)g;

        // draw stage (border, labels, etc)
        BasicStroke bs2 = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
        g2d.setStroke(bs2);
        g2d.setColor(Color.BLACK);
        
        g2d.drawRect(GRAPH_X, GRAPH_Y, GRAPH_W, GRAPH_H);
        
        // mark 5 price levels (high, low, and 3 in between)
        for (int i=0; i<=4; i++) {
        	int yPos = GRAPH_Y+i*GRAPH_H/4;
        	double priceLevel = globalLow + (globalHigh-globalLow)*(5-i)/5;
        	g2d.drawLine(GRAPH_X, yPos, GRAPH_X + GRAPH_W, yPos);
        	g2d.drawString(Double.toString(priceLevel), GRAPH_X + GRAPH_W + 5, yPos + 3);
        }
        
        // mark 3 dates (last close date, 1 year before close date, last prediction date)
		DateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH); // used to format date output
		Calendar cal = Calendar.getInstance(); // will use Calendar to get 1 year before last close (in case we don't have enough historic data)
		cal.setTime(historicData.get(historicData.size()-1).date);
		
		// label last close
		g2d.drawString(dateFormatter.format(cal.getTime()), GRAPH_X + 51*(CANDLE_WIDTH + CANDLE_SPACING), GRAPH_Y+GRAPH_H+15);
		
		// label 1 year ago
		cal.add(Calendar.DATE,-365);
		g2d.drawString(dateFormatter.format(cal.getTime()), GRAPH_X, GRAPH_Y+GRAPH_H+15);
		
		// label 1 year from close
		g2d.drawString(dateFormatter.format(predictedData[51].date), GRAPH_X + 101*(CANDLE_WIDTH + CANDLE_SPACING), GRAPH_Y+GRAPH_H+15);
		
        
        // draw candle chart from historic data
        int historicDataSize = historicData.size();
        for (int i=0; i<historicDataSize; i++) {
        	HistoricDataPoint point = historicData.get(i);
        	// scaled values are heights from top of the graph, so a higher stock price will have a lower scaled value
        	int scaledHigh = (int) Math.round(GRAPH_H * (globalHigh - point.high)/(globalHigh - globalLow));
        	int scaledLow = (int) Math.round(GRAPH_H * (globalHigh - point.low)/(globalHigh - globalLow));
        	
        	drawCandleFromXYWHOCHL (
        			g2d,
        			GRAPH_X + (52 - historicDataSize + i)*(CANDLE_WIDTH + CANDLE_SPACING),
        			GRAPH_Y + scaledHigh,
        			CANDLE_WIDTH,
        			scaledLow-scaledHigh,
        			point.open,
        			point.close,
        			point.high,
        			point.low);
        	
        }
        
        // lines connect two prediction points, which stem from the most recent close price
        double prevClose, prevCloseUpper, prevCloseLower;
        prevClose = prevCloseUpper = prevCloseLower = historicData.get(historicData.size()-1).close;
        
        for (int i=0; i<52; i++) {
        	int pointSpot = 52+i; // predictions start drawing from 52nd spot
        	double closeExpected = predictedData[i].expected;
        	double closeUpper = predictedData[i].upperBound;
        	double closeLower = predictedData[i].lowerBound;
        	
        	bs2 = new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
            g2d.setStroke(bs2);
            
            // draw expected close prices
            g2d.setColor(Color.BLUE);
            g2d.drawLine(
            		(int)(GRAPH_X + Math.round((pointSpot-0.5)*(CANDLE_WIDTH+CANDLE_SPACING))),
            		(int)(GRAPH_Y + Math.round(GRAPH_H * (globalHigh-prevClose)/(globalHigh-globalLow))),
            		(int)(GRAPH_X + Math.round((pointSpot+0.5)*(CANDLE_WIDTH+CANDLE_SPACING))),
            		(int)(GRAPH_Y + Math.round(GRAPH_H * (globalHigh-closeExpected)/(globalHigh-globalLow)))
            		);
            
            // draw upper bound
            g2d.setColor(Color.MAGENTA);
            g2d.drawLine(
            		(int)(GRAPH_X + Math.round((pointSpot-0.5)*(CANDLE_WIDTH+CANDLE_SPACING))),
            		(int)(GRAPH_Y + Math.round(GRAPH_H * (globalHigh-prevCloseUpper)/(globalHigh-globalLow))),
            		(int)(GRAPH_X + Math.round((pointSpot+0.5)*(CANDLE_WIDTH+CANDLE_SPACING))),
            		(int)(GRAPH_Y + Math.round(GRAPH_H * (globalHigh-closeUpper)/(globalHigh-globalLow)))
            		);
            
            
            // draw lower bound
            g2d.setColor(Color.CYAN);
            g2d.drawLine(
            		(int)(GRAPH_X + Math.round((pointSpot-0.5)*(CANDLE_WIDTH+CANDLE_SPACING))),
            		(int)(GRAPH_Y + Math.round(GRAPH_H * (globalHigh-prevCloseLower)/(globalHigh-globalLow))),
            		(int)(GRAPH_X + Math.round((pointSpot+0.5)*(CANDLE_WIDTH+CANDLE_SPACING))),
            		(int)(GRAPH_Y + Math.round(GRAPH_H * (globalHigh-closeLower)/(globalHigh-globalLow)))
            		);
            
            prevClose = closeExpected;
            prevCloseUpper = closeUpper;
            prevCloseLower = closeLower;
        }
    }
    
    // helper methods
    
    private void findExtrema() { // set globalHigh and globalLow
    	// find extrema among historic data
    	globalHigh = historicData.get(0).high;
    	globalLow = historicData.get(0).low;
    	for (HistoricDataPoint point : historicData) {
    		globalHigh = Math.max(globalHigh, point.high);
    		globalLow = Math.min(globalLow, point.low);
    	}
    	
    	// find extrema globally
    	for (PredictedDataPoint point : predictedData) {
    		globalHigh = Math.max(globalHigh, point.upperBound);
    		globalLow = Math.min(globalLow, point.lowerBound);
    	}
    	
    	globalHigh=Math.ceil(globalHigh/10)*10; // rounds max up to nearest 10s place
    	globalLow=Math.floor(globalLow/10)*10; // rounds min down to nearest 10s place
    }
    
    private void drawCandleFromXYWHOCHL(Graphics2D g2d, int x, int y, int w, int h, double open, double close, double high, double low ) {
    	// y corresponds to high
    	// y+h corresponds to low
    	// based on those, y-pos of open and close are computed
    	
    	// scaled values are heights from top, so a higher stock price will be a lower scaled value
    	int scaledOpen = (int)Math.round(h*(high-open)/(high-low));
    	int scaledClose = (int)Math.round(h*(high-close)/(high-low));
    	
    	BasicStroke bs2 = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
        g2d.setStroke(bs2);
        g2d.setColor((close>open)?Color.GREEN:Color.RED); // weekly gains are green, weekly losses are red
    	
    	//g2d.drawLine(x,y,x+w-1,y); // draw high line
    	//g2d.drawLine(x,y+h,x+w-1,y+h); // draw low line
    	g2d.drawLine(x+w/2,y,x+w/2,y+h); // draw center wick
    	if (open >= close) {
    		g2d.fillRect(x,y+scaledOpen,w,Math.max(scaledClose-scaledOpen,1));
    	} else {
    		g2d.fillRect(x,y+scaledClose,w,Math.max(scaledOpen-scaledClose,1));
    	}
    }
}
