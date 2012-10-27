import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.regex.Pattern;


public class RequestRunner implements Runnable{
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
	
	private int id; // used to differentiate between threads. Really more for debugging
	private Socket client; // client that was accepted
	
	// buffers used by client
	private PrintWriter clientOut;
	private BufferedReader clientIn;
	
	// socket and buffers used to contact server
	private Socket server;
	private PrintWriter serverOut;
	private BufferedReader serverIn;
	
	// sets how big of a buffer there should be
	private final static int READAHEAD = 100;
	
	public RequestRunner(int id, Socket client, Map<String, InetAddress> dnsCache) throws IOException
	{
		this.id = id;
		this.client = client;
		
		try {
			clientOut = new PrintWriter(client.getOutputStream(), true);
			clientIn = new BufferedReader(new InputStreamReader(client.getInputStream()));
		} catch (IOException e) {
			cleanUp();
			throw e;
		}
		
		// mark the beginning of the stream so that it can be returned to after
		// figuring out the domain name
		clientIn.mark(READAHEAD);
		
		// find the host directive in the header
		String hostLine;
		clientIn.readLine(); // ignore this line
		hostLine = clientIn.readLine();
		
		// extract the host name
		// first split on : giving three strings
		//        "Host" " <hostname>" "port"
		// take the second string and delete the
		// space at the front of it using substr
		String host = hostLine.split(":")[1].substring(1);
		
		System.out.println("Thread " + this.id + " request for " + host);
		
		// reset to the beginning of the input from the client
		clientIn.reset();
		
		InetAddress serverAddr = dnsLookup(host, dnsCache);
		
		try {
			server = new Socket(host, 80);
			
			serverOut = new PrintWriter(server.getOutputStream(), true);
			serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));
		} catch (IOException e) {
			cleanUp();
			throw e;
		}
	}
	
	/*
	 * This method is called when the thread is finished
	 * It closes threads spawned by this thread. It also
	 * closes any sockets that were used.
	 */
	private void cleanUp() throws IOException
	{
		// close client socket
		// and associated buffers
		if(clientOut != null)
			clientOut.close();
		if(clientIn != null)
			clientIn.close();
		if(client != null)
			client.close();
		
		// close server socket
		// and associated buffers
		if(serverOut != null)
			serverOut.close();
		if(serverIn != null)
			serverIn.close();
		if(server != null)
			server.close();
	}
	
	/*
	 * This method tries to lookup the DNS info in the cache.
	 * If it is not found then it uses the normal DNS resolution
	 * and adds the results to the cache. If neither method can
	 * find it then it throws the UnknownHostException
	 */
	private InetAddress dnsLookup(String host, Map<String, InetAddress> dnsCache) throws UnknownHostException
	{
		InetAddress addr;
		addr = dnsCache.get(host); // lookup via the cache
		if(addr == null) // if no results were found
		{
			addr = dnsLookup(host); // lookup via the old fashioned way
			dnsCache.put(host,addr); // save the entry for future use
			return addr;
		}
		else
		{
			return addr;
		}
	}
	
	/*
	 * This method does normal DNS resolution. If it cannot
	 * find the host it throws the UnknownHostException.
	 */
	private InetAddress dnsLookup(String host) throws UnknownHostException
	{
		return InetAddress.getByName(host);
	}

	/* 
	 * This method is where the main code of the thread
	 * is executed.
	 */
	@Override
	public void run() {
		// TODO This is where two forwarding threads are spawned
		
		try {
			cleanUp();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Thread " + id + " is finished");
	}

}
