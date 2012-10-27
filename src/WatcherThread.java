
public class WatcherThread implements Runnable{
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
	

	public WatcherThread()
	{
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
