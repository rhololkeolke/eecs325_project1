import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;


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
	private Socket clientSocket; // client that was accepted
	
	public RequestRunner(int id, Socket client, Map<String, InetAddress> dnsCache) throws IOException
	{
		this.id = id;
		this.clientSocket = client;
	}
	
	/*
	 * This method is called when the thread is finished
	 * It closes threads spawned by this thread. It also
	 * closes any sockets that were used.
	 */
	private void cleanUp() throws IOException
	{

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
			System.out.println("Thread " + id + " New address was added to the cache");
			return addr;
		}
		else
		{
			System.out.println("Thread " + id + " This address was in the cache");
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
		try{
			BufferedReader bis = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			
			// read the request
			System.out.println("Thread " + id + ": Reading the request");
			StringBuilder requestBuilder = new StringBuilder();
			String bLine;
			while((bLine = bis.readLine()) != null)
			{
				requestBuilder.append(bLine + "\r\n");
				//System.out.println("Thread " + id + ": " + bLine + "\r");
				
				if(bLine.equals("")) // HTTP headers end with a blank line
					break;
				
			}
			
			String request = requestBuilder.toString();
			
			if(request.length() <= 0)
			{
				System.err.println("\n\tThread " + id + ": " + "Request has length of 0. Skipping this request.\n");
				bis.close();
				clientSocket.close();
				return;
			}
			
			// extract the host to connect to
			int start = request.indexOf("Host: ") + 6;
			int end = request.indexOf("\n", start);
			String host = request.substring(start, end-1);
			System.out.println("Thread " + id + ": Connecting to host " + host);
			
			// forward response from the proxy to the server
			// TODO replace this with the dns cacheing code
			Socket hostSocket = new Socket(host, 80);
			
			PrintWriter sos = new PrintWriter(hostSocket.getOutputStream(), true);
			sos.print(request);
			sos.flush();
			
			// forward the response from the server to the browser
			
			InputStream sis = hostSocket.getInputStream();
			OutputStream bos = clientSocket.getOutputStream();
			
			int n = 0;
			byte[] buffer = new byte[100000];
			do {
				n = sis.read(buffer);
				System.out.println("Thread " + id + ": Receiving " + n + " bytes");
				if(n > 0)
				{
					bos.write(buffer, 0, n);
					bos.flush();
				}
			} while (n > 0);
			
			bis.close();
			bos.close();
			clientSocket.close();
			
			sis.close();
			sos.close();
			hostSocket.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
