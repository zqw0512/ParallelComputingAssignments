package com.qzhu;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelPi {
	private class PiRunnable implements Runnable {
		public PiRunnable(int id, int totalTimes, AtomicInteger currentTimes, AtomicInteger inCounter,AtomicInteger outCounter) {
			this.id = id;
			this.currentTimes = currentTimes;
			this.totalTimes = totalTimes;
			this.inCounter = inCounter;
			this.outCounter = outCounter;
		}

		private int id;
		private int totalTimes;
		private AtomicInteger currentTimes;
		private AtomicInteger inCounter;
		private AtomicInteger outCounter;

		@Override
		public void run() {
			System.out.println("thread [" + this.id + "] started");

			while (currentTimes.getAndIncrement() < totalTimes) {

				final double x = Math.random();
				final double y = Math.random();
				if (isInCircle(x, y, 1)) {
					inCounter.getAndIncrement();
				}else {
					outCounter.getAndIncrement();
				}
			}
			System.out.println("thread [" + this.id + "] ended");

		}
	};

	public static void main(String[] args) {
		ParallelPi spi = new ParallelPi();
		spi.calculatePi(10000000, 10);
	}

	/**
	 * This method run multiple threads simultaniously to calculate the pi, and
	 * return the calculated result of the pi.
	 * 
	 * @param totalTimes
	 *            the number of simulations to run
	 * @param threadCount
	 *            how many threads it will run
	 * @return
	 */
	public double calculatePi(final int totalTimes, int threadCount) {
		if (totalTimes <= 0) {
			throw new RuntimeException("invalid input: times, must >= 0");
		}
		if (threadCount <= 0) {
			throw new RuntimeException("invalid input: threadCount, must >= 0");
		}

		AtomicInteger inCounter = new AtomicInteger(0);
		AtomicInteger outCounter = new AtomicInteger(0);

		AtomicInteger currentTimes = new AtomicInteger(0);

		Thread[] threads = new Thread[threadCount];
		for (int i = 0; i < threadCount; i++) {
			Runnable r = new PiRunnable(i, totalTimes, currentTimes, inCounter,outCounter);
			threads[i] = new Thread(r);
		}
		
		long since = System.currentTimeMillis();
		for (int i = 0; i < threadCount; i++) {
			threads[i].start();
		}

		for (int i = 0; i < threadCount; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		double result = (inCounter.get() / (double) totalTimes) * 4;
		System.out.println("=== Parallel PI - Execution Result: ===");
		System.out.println("Total " + totalTimes + " times executed");
		System.out.println(inCounter.get() + " times in the circle");
		System.out.println(outCounter.get() + " times out of the circle");
		System.out.println("in + out numbers equals to total times? "+(outCounter.get()+inCounter.get()==totalTimes ));
		System.out.println("Approximate Pi is " + result);
		System.out.println("Actual Pi is " + Math.PI);
		double diff = (result - Math.PI) / Math.PI * 100;
		NumberFormat formatter = new DecimalFormat("#0.00000000");

		System.out.println("Difference is " + formatter.format(diff) + "%");
		System.out.println("Time spend is " + (System.currentTimeMillis() - since) + " ms");

		return result;
	}

	/**
	 * given a coordination[x,y], -1<=x<=1, -1<=y<=1, check if the coordination
	 * is inside the circle x^2+y^2=r^2
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private boolean isInCircle(final double x, final double y, final double r) {
		return (x * x + y * y) <= r * r;
	}
}
