package org.vaadin.cored;

/**
 * 
 * Window close events are unreliable, so we'll use this kind of timer
 * to check if window has been closed
 *
 */
public class CleanupTimer implements Runnable  {

	private volatile int counter = 0;
	private final Runnable runnable;
	private final int timeout;
	
	public CleanupTimer(Runnable runnable, int timeoutSeconds) {
		this.runnable = runnable;
		this.timeout = timeoutSeconds;
	}

    public void start() {
        Thread t = new Thread(this);
        t.start();
    }

    public void reset() {
        counter = 0;
    }

    public void run() {
        while (counter++ < timeout) {
            try {
            	Thread.sleep(1000);
            } catch (InterruptedException e){
            	return;
            }
        }
        runnable.run();
    }
}
