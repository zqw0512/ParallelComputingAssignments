package com.qzhu;

import java.util.concurrent.atomic.AtomicReference;

public class SpinLock {
	private AtomicReference<Thread> lockingThread = new AtomicReference<Thread>();

	public boolean lock() {
		while (true) {
			// if lockingThread=null and successfully updated the locking thread
			// to current thread
			// then it means I get the lock, return it
			if (lockingThread.compareAndSet(null, Thread.currentThread())) {
				return true;
			}
			// otherwise, means this thread failed to acquire the lock, keep
			// spinning
		}
	}

	public boolean unlock() {
		return lockingThread.compareAndSet(Thread.currentThread(), null);
	}

}
