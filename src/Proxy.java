import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Proxy {
	/*
	 * Class:   Proxy
	 * 
	 * Author:    Devin Schwab
	 * 
	 * Case ID: dts34
	 * 
	 * Course:  EECS 325 - Networks
	 * 
	 * Created: 10/26/12
	 * 
	 * Description
	 * ------------
	 * This class is the main method in my proxy.
	 * It is responsible for listening for new client requests
	 * It also responsible for maintaining the DNS cache that is shared
	 * among all of the request threads.
	 */
	
	// DNS cache shared by all threads
	static Map<String, InetAddress> dnsCache = new ConcurrentHashMap<String, InetAddress>();
	
	final static int PORT = 9090;
	
	public static void main(String[] args) {
		
		// this will store the accepted client connection before it
		// is sent to a new requestThread
		Socket client = null;
		
		// watcher thread
		// keeps track of other threads so that
		// they can be removed when they finish
		WatcherRunner watcherRunner = new WatcherRunner();
		Thread watcher = new Thread(watcherRunner);
		watcher.start();
		
		// This will store a new RequestThread
		Thread request = null;
		
		// first open the ServerSocket so that requests can be listened for
		ServerSocket server = null;
		try {
			System.out.println("Starting Proxy Server on port " + PORT);
			server = new ServerSocket(PORT);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't open port " + PORT);
			System.exit(1);
		}
		
		// Listen for client requests
		// when a request is found spawn a new thread
		try {
			client = server.accept();
			
			try {
				request = new Thread(new RequestRunner(watcherRunner.numThreads(), client, dnsCache));
				request.start();
				watcherRunner.addThread(request);
			} catch (IOException e) {
				System.err.println("ERROR: IO Problem creating thread");
				System.err.println(e.getMessage());
				System.exit(1);
			}
			
			
		} catch (IOException e1) {
			System.err.println("ERROR: Problem accepting client request");
			System.exit(1);
		}
		
		// Clean up
		try {
			System.out.println("Shutting down Proxy Server on port " + PORT);
			server.close();
			System.out.println("Interrupting request threads");
			watcher.interrupt();
			System.out.println("Waiting for request threads to die");
			watcher.join();
			System.out.println("All done!");
		} catch (IOException e) {
			System.err.println("ERROR: Problem closing socket on port " + PORT);
			System.exit(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
