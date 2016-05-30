package com.qzhu;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class SequentialPi {
	
	
	
	public static void main(String[] args) {
		SequentialPi spi = new SequentialPi();
		spi.calculatePi(50000000);
	}
	/**
	 * This method take input parameter as the number of simulations to run, and return the 
	 * calculated result of the pi.  
	 * 
	 * @param times
	 * @return
	 */
	public double calculatePi(int times){
		if (times<=0) {
			throw new RuntimeException("invalid input: times, must >= 0");
		}
		long since = System.currentTimeMillis();
		int in=0;
		for (int i=0;i<times;i++){
			double x = Math.random();
			double y = Math.random();
			if ((x*x+y*y)<=1) {
				in++;
			}
		}
		double result = (in/(double)times)*4;
		System.out.println("=== Sequential PI - Execution Result: ===");
		System.out.println("Total "+times+" times executed");
		System.out.println(in+" times in the circle");
		System.out.println("Approximate Pi is "+result);
		System.out.println("Actual Pi is "+Math.PI);
		double diff=(result-Math.PI)/Math.PI*100;
		NumberFormat formatter = new DecimalFormat("#0.00000000");

		System.out.println("Difference is "+formatter.format(diff)+"%");
		System.out.println("Time spend is "+(System.currentTimeMillis()-since)+" ms");
		
		return result;
	}
	
}
