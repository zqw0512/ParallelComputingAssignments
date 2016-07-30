package com.qzhu;

public class SpinLockTest {
	private static class TestRunnable implements Runnable {
		private SpinLock lock;
		private String id;

		public TestRunnable(String id, SpinLock lock) {
			this.id = id;
			this.lock = lock;
		}

		@Override
		public void run() {
			System.out.println(id + " started");
			long t0 = System.currentTimeMillis();

			System.out.println(id + " try to acquire lock at " + t0);
			
			//try acquire the lock
			lock.lock();
			long t1 = System.currentTimeMillis();
			System.out.println(id + " acquired lock at " + System.currentTimeMillis());
			
			//do random things(wait 1000ms to simulate the actual work)
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			lock.unlock();
			long t2 = System.currentTimeMillis();
			System.out.println(id + " release lock at " + t2);
			System.out.println(id + " Finished, total time spend is " + (t2 - t0) + " ms, total time waited is "
					+ (t1 - t0) + "ms");

		}
	}

	public static void main(String[] args) {
		SpinLock lock = new SpinLock();

		Runnable r1 = new TestRunnable("Thread #1", lock);
		Runnable r2 = new TestRunnable("Thread  #2", lock);
		Runnable r3 = new TestRunnable("Thread   #3", lock);
		new Thread(r1).start();
		new Thread(r2).start();
		new Thread(r3).start();
	}
}
