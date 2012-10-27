
public class RequestThread implements Runnable{
	/*
	 * Class:   RequestThread
	 * 
	 * Author:  Devin Schwab
	 * 
	 * Case ID: dts34
	 * 
	 * Created: 10/26/12
	 * 
	 * Description
	 * ------------
	 * This class is used by the Thread class.
	 * 
	 * It is responsible for doing the DNS resolution
	 * for the request using the DNS resolve method.
	 * 
	 * It spawns two ForwardingThread threads. These threads
	 * are used to echo information from the client to the server
	 * and from the server to the client. These are separate threads
	 * so that there are no deadlocks when waiting for input from
	 * either endpoint of the communication. This also allows
	 * asynchronous communication between the server and the client.
	 */
	
	public RequestThread()
	{
		
	}
	
	/*
	 * This method is called when the thread is finished
	 * It closes threads spawned by this thread. It also
	 * closes any sockets that were used.
	 */
	private void cleanUp()
	{
		
	}

	/* 
	 * This method is where the main code of the thread
	 * is executed.
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
