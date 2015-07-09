package com.alexfiliakov.blackscholescalc;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.alexfiliakov.blackscholescalc.OptionPriceModel.OptionPriceTableModel;
import com.alexfiliakov.blackscholescalc.StockModel.StockGraph;
import com.alexfiliakov.blackscholescalc.StockModel.StockModel;

public class BlackScholesCalculator {
	/**
	 * @param args
	 */
	private static StockModel stockModel; // Lognormal Stock Model
	private static StockGraph stockGraph; // graphs the stock model
	private static OptionPriceTableModel optionPriceTableModel; // table of option prices
	private static String stockSymbol = null; // contains the current stock ticker symbol

	public static void main(String[] args) {
		// initializes GUI
		
		final JFrame frame = new JFrame("Black-Scholes Calculator");
		final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		// define components
		final JLabel lblActionLabel = new JLabel();
		lblActionLabel.setText("Enter stock symbol: ");
		
		final JLabel lblerrorMessage = new JLabel();
		lblerrorMessage.setText("");
		
		final JTextField fldTickerInput = new JTextField();
		fldTickerInput.setPreferredSize(new Dimension(100,24));
		fldTickerInput.setText("GOOG");
		
		// visual table of options prices
		final JTable optionsTable = new JTable();
		
		// radio buttons to allow alternating between stock chart and option prices
		final JRadioButton radShowStockChart = new JRadioButton("Lognormal Stock Chart");
		radShowStockChart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				splitPane.setBottomComponent(stockGraph);
			}
		});
		radShowStockChart.setEnabled(false);
		
		final JRadioButton radShowOptionPrices = new JRadioButton("Black-Scholes Option Prices");
		radShowOptionPrices.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				splitPane.setBottomComponent(new JScrollPane(optionsTable));
			}
		});
		radShowOptionPrices.setEnabled(false);
		
		final ButtonGroup radGroup = new ButtonGroup();
		radGroup.add(radShowStockChart);
		radGroup.add(radShowOptionPrices);
		
		// button to retrieve stock data and run models
		final JButton btnRunModel = new JButton("Run Model");
		btnRunModel.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae) {
				// stock symbols must contain only letters
				// nonexistent symbols won't retrieve any data
				try {
					lblerrorMessage.setText("");
					splitPane.setBottomComponent(null);
					
					stockSymbol = fldTickerInput.getText();
					if (!stockSymbol.matches("[a-zA-Z]+")) {
						throw new Exception ("input must contain letters");
					}
					
					// set up the stock model and visual graph
					stockModel = new StockModel(stockSymbol);
					stockGraph = new StockGraph(stockModel);
					
					// set up the option model and visual table
					optionPriceTableModel = new OptionPriceTableModel(stockModel);
					optionsTable.setModel(optionPriceTableModel.getTableModel());
					OptionPriceTableModel.formatTable(optionsTable);
					
					radShowStockChart.setEnabled(true);
					radShowOptionPrices.setEnabled(true);
					radShowOptionPrices.doClick();
				} catch (Exception e) {
					lblerrorMessage.setText("Error: " + e.getMessage());
				}
				frame.setVisible(true);
			}
		});
		
		// set up the control panel
		JPanel inputPanel = new JPanel();
		inputPanel.add(lblActionLabel);
		inputPanel.add(fldTickerInput);
		inputPanel.add(btnRunModel);
		inputPanel.add(lblerrorMessage);
		inputPanel.add(new JSeparator(SwingConstants.VERTICAL));
		inputPanel.add(radShowStockChart);
		inputPanel.add(radShowOptionPrices);
		
		splitPane.setTopComponent(inputPanel);
		splitPane.setDividerSize(0);
		
		// create the application window
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(850, 550);
		frame.setLocationRelativeTo(null);
		frame.add(splitPane);
		frame.setVisible(true);
	}
}
