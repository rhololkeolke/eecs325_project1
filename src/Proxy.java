import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Proxy {
	
	public final static int PORT = 9090;
	
	public final static ConcurrentMap<String, InetAddress> dnsCache = new ConcurrentHashMap<String, InetAddress>();
	
	public static void main(String[] args) {
		
		System.out.println("Starting proxy on port " + PORT);
		try {
			listen();
		} catch (IOException e) {
			System.err.println(e);
		}
	}
	
	public static void listen() throws IOException {
		
		ServerSocket proxySocket = null;
		Socket clientSocket = null;
		try{
			// create a socket to listen for requests
			proxySocket = new ServerSocket(PORT);
			
			clientSocket = null;
			while(true) { // loop until the entire program is killed
				
				// this blocks until a request is sent to the server
				clientSocket = proxySocket.accept();
				
				// start a new thread for the request
				// so that multiple streams can be serviced
				// simultaneously
				Thread requestThread = new RequestThread(clientSocket);
				requestThread.start();
			}
		} catch (IOException e) {
			System.err.println("Something went wrong with the listening socket...quitting");
		} finally {
			if(proxySocket != null)
				proxySocket.close();
			if(clientSocket != null)
				clientSocket.close();
		}
	}
	
	public static class RequestThread extends Thread{
		public final Socket clientSocket;
		
		public static final Pattern requestPattern = Pattern.compile("^(\\w+)\\s+(\\S+)\\s+HTTP/(1\\.\\d)\\s*$");
		public static final Pattern contentLengthPattern = Pattern.compile("^Content-Length:\\s+(\\d+)\\s*$"); // grab the content length (if present) group1 is the number of bytes
		
		public RequestThread(Socket c)
		{
			clientSocket = c;
		}
		
		public void run() {
			int bytesRead = 0;
			final byte[] request = new byte[4096];
			byte[] reply = new byte[4096];
			
			Socket serverSocket = null;
			try {
				final InputStream cis = clientSocket.getInputStream(); // this is used for any data not in the header
				final OutputStream cos = clientSocket.getOutputStream(); 
				
				/*
				 * This section will parse the header and create a new one to send to the server
				 */

				// the header will be in standard ASCII so its fine to use a BufferedReader
				BufferedReader cbr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				
				String headerLine;
				if((headerLine = cbr.readLine()) == null) // null if stream is closed. NOTE: This will block until something is read or stream is closed
				{
					//throw new IOException("Error reading request Header 1"); // if nothing was read then nothing else can be done for this request
					return;
				}
				
				System.out.println("Received header");
				System.out.println("\t" + headerLine);
				
				// parse the first line of the request
				Matcher headerMatcher = requestPattern.matcher(headerLine);
				headerMatcher.matches();
				String verb = headerMatcher.group(1);
				String url = headerMatcher.group(2);
				String version = headerMatcher.group(3);
				
				
				if(verb == null || url == null || version == null)
					throw new IOException("Erorr reading request header 2");

				
				// parse out the path from the url
				URL serverUrl = new URL(url);
				
				// build the header
				String newHeaderLine = new String(verb + " " + serverUrl.getPath() + " HTTP/" + version + "\r\n");
				
				System.out.println("New Header:");
				System.out.println("\t" + newHeaderLine);
				System.out.println("");
				
				// Now that the new header is created open a socket to the host in the url on the port specified
				
				int port;
				if((port = serverUrl.getPort()) == -1)
					port = 80;
				try{ 
					serverSocket = new Socket(serverUrl.getHost(), port);
				} catch (IOException e) {
					PrintWriter out = new PrintWriter(cos, true);
					out.println("Proxy server can't connect to " + serverUrl.getHost() + ":" + port + "\n" + e);
					out.close();
					clientSocket.close();
					return;
				}
				
				// setup the input and output streams for the server
				InputStream sis = serverSocket.getInputStream();
				OutputStream sos = serverSocket.getOutputStream();
				
				// write the new header line to the server
				sos.write(newHeaderLine.getBytes());
				sos.flush();
				
				// stores the number from the Content-length field of the header
				// should tell the proxy if there is extra data such as in a POST request
				int contentLen = 0;
				
				// get the rest of the header
				while(true)
				{
					headerLine = cbr.readLine(); // this will block until a full line is available
					
					// debugging purposes
					System.out.println("\t" + headerLine);
					
					// see if this is the content-length line
					Matcher contentLengthMatcher = contentLengthPattern.matcher(headerLine);
					if(contentLengthMatcher.matches())
					{
						// convert the content length number from a string to an integer
						contentLen = Integer.parseInt(contentLengthMatcher.group(1));
					}
					
					if(headerLine.matches("^Proxy-Connection:.*"))
					{
						System.out.println("Found the proxy-connection line skipping it");
						continue;
					}
					else if(headerLine.equals(""))
					{
						sos.write("Connection: close\r\n\r\n".getBytes());
						//sos.write("\r\n".getBytes());
						sos.flush();
						
						// would get the next n bytes where n is the number in content-length
						System.out.println("Reached end of header");
						System.out.println("Content-length: " + contentLen);
						if(contentLen > 0)
						{
							while((bytesRead = cis.read(request)) != -1 && contentLen > 0)
							{
								System.out.println("Read " + bytesRead);
								sos.write(request);
								sos.flush();
								contentLen -= bytesRead;
								System.out.println("Content length remaining: " + contentLen);
							}
						}
						
						// all done reading the request
						System.out.println();
						break;
					}
					else
					{
						// if it isn't the end of the header then
						// simply copy what was received to the server
						sos.write((headerLine + "\r\n").getBytes());
						sos.flush();
					}
				}
				
				try{
					while((bytesRead = sis.read(reply)) != -1)
					{
						System.out.println("Received " + bytesRead + " bytes");
						//System.out.println(new String(reply, 0, bytesRead));
						cos.write(reply);
						cos.flush();
					}
				} catch (IOException e) {
					System.err.println("Exception 1\n\t" + e);
				}
				
				clientSocket.close();
				serverSocket.close();

				return;
			} catch (IOException e) {
				System.err.println(e);
			} finally { // clean up the sockets and streams
				try{
					if(serverSocket != null)
						serverSocket.close();
					if(clientSocket != null)
						clientSocket.close();
				} catch (IOException e) {}
			}
			
		}
	}
	
	private static Socket parseHeader(String header) throws IOException
	{
		String hostname;
		int port;
		int start, end, colon; // markers for start of hostname, end of hostname and start of port (if present)
		if((start = header.indexOf("Host: ")) < 0)
			return null;
		else
			start += 6;
		
		if((end = header.indexOf('\n', start)) < 0)
			return null;
		
		colon = header.indexOf(':', start);
		
		
		if(colon >= end)
		{
			// no port specified
			hostname = header.substring(start, end-1);
			port = 80;
		}
		else
		{
			// port specified
			hostname = header.substring(start, colon);
			port = Integer.parseInt(header.substring(colon+1, end-1));
		}
		
		System.out.println("Connecting to " + hostname + " on port " + port);
		
		InetAddress hostAddress = lookupDns(hostname);
		
		return new Socket(hostAddress, port);
	}
	
	private static InetAddress lookupDns(String hostname) throws UnknownHostException
	{
		InetAddress hostAddress = null;
		
		// if the DNS record is in the cache return it
		if((hostAddress = dnsCache.get(hostname)) != null)
			return hostAddress;
		
		// DNS records wasn't in the cache
		// lookup DNS
		hostAddress = InetAddress.getByName(hostname);
		// add the DNS record to the cache for future use
		dnsCache.put(hostname, hostAddress);
		return hostAddress;
	}
}