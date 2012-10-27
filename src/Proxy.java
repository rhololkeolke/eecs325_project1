import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


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
	final static int threadsPerCore = 10;
	
	public static boolean quit = false; // this doesn't need to be volatile because only one thread will update it
	
	public static void main(String[] args) throws IOException {
		
		// this will store the accepted client connection before it
		// is sent to a new requestThread
		Socket client = null;
		
		// watcher thread
		// keeps track of other threads so that
		// they can be removed when they finish
		WatcherRunner watcherRunner = new WatcherRunner();
		Thread watcher = new Thread(watcherRunner);
		watcher.start();
		
		// Determine the number of threads
		int threadCount = 0;
		int cpus = Runtime.getRuntime().availableProcessors();
		int maxThreads = cpus * threadsPerCore; /// how many threads should be run per core
		maxThreads = (maxThreads > 0 ? maxThreads : 1); // make sure there is at least one thread
		
		// This will execute request threads
		ExecutorService requestExecutor = new ThreadPoolExecutor(maxThreads,
				                                                 maxThreads,
				                                                 1,
				                                                 TimeUnit.MINUTES,
				                                                 new ArrayBlockingQueue<Runnable>(maxThreads, true),
				                                                 new ThreadPoolExecutor.DiscardPolicy());
		
		// first open the ServerSocket so that requests can be listened for
		ServerSocket server = null;
		try {
			System.out.println("Starting Proxy Server on port " + PORT);
			server = new ServerSocket(PORT);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't open port " + PORT);
			quit = true; // will skip the accept and go straight to clean up
		}
		
		
		System.out.println("Press Ctrl-D to exit");
		Thread inputWatcher = new Thread(new InputWatcherRunner(quit, server));
		inputWatcher.start();
		
		while(!quit)
		{
			// Listen for client requests
			// when a request is found spawn a new thread
			try {
				System.out.println("Listening for a request");
				client = server.accept();
				
				System.out.println("Got a request");
				requestExecutor.submit(new RequestRunner(threadCount, client, dnsCache));
				threadCount++;
				
				
				
			} catch (IOException e1) {
				System.err.println("ERROR: Problem accepting client request");
				break;
			}
		}
		
		// Clean up
		try {
			System.out.println("Shutting down Proxy Server on port " + PORT);
			server.close();
			System.out.println("Shutting down Threads");
			requestExecutor.shutdown();
			
			try {
				if(!requestExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
					System.out.println("\t Trying to shutdown threads again");
					requestExecutor.shutdownNow();
				}
			} catch (InterruptedException ex) {
				System.out.println("\t Trying to shutdown threads again");
				requestExecutor.shutdownNow();
			}
			
			for(String key : dnsCache.keySet())
			{
				System.out.println(dnsCache.get(key));
			}
		} catch (IOException e) {
			System.err.println("ERROR: Problem closing socket on port " + PORT);
			System.exit(1);
		}
	}
}
