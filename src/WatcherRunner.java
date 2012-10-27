import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class WatcherRunner implements Runnable{
	/*
	 * Class: WatcherThread
	 * 
	 * Author: Devin Schwab
	 * 
	 * Case ID: dts34
	 * 
	 * Created: 10/26/12
	 * 
	 * Description
	 * ------------
	 * Because server sockets block on accept
	 * a thread capable of tracking and closing
	 * other threads that are finished is needed.
	 * Otherwise the number of threads would be just keep increasing
	 */

	private List<Thread> threads;

	public WatcherRunner()
	{
		// make the list thread safe
		threads = Collections.synchronizedList(new LinkedList<Thread>());
	}
	
	/*
	 * Adds a thread to be watched
	 */
	public void addThread(Thread t)
	{
		threads.add(t);
	}
	
	/*
	 * returns the number of threads being watched
	 */
	public int numThreads()
	{
		return threads.size();
	}
	
	/*
	 * stops all threads
	 */
	private void killAll()
	{
		// interrupt all of the threads
		for(int i=0; i<threads.size(); i++)
		{
			threads.get(i).interrupt();
		}
		
		// keep watching the threads until all are dead
		while(threads.size() > 0)
		{
			pruneThreads();
			
			// take a break so that the entire processor isn't used up
			try{
				Thread.sleep(100);
			} catch (InterruptedException e) {
				
			}
		}
	}
	
	private void pruneThreads()
	{

		for(int i=0; i<threads.size(); i++)
		{
			if(!threads.get(i).isAlive())
			{
				threads.remove(i);
			}
		}
	}
	
	@Override
	public void run() {
		
		while(!Thread.currentThread().isInterrupted())
		{
			pruneThreads();
			
			// take a break so that the entire processor isn't used up
			try{
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
		}
		killAll();
	}

}
