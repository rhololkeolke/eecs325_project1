import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Proxy3 {
	
	public final static int PORT = 9090;
	
	public static void main(String[] args) {
		
		System.out.println("Starting proxy on port " + PORT);
		try {
			listen();
		} catch (IOException e) {
			System.err.println(e);
		}
	}
	
	public static void listen() throws IOException {
		
		// create a socket to listen for requests
		ServerSocket proxySocket = new ServerSocket(PORT);
		
		Socket clientSocket = null;
		while(true) { // loop until the entire program is killed
			
			// this blocks until a request is sent to the server
			clientSocket = proxySocket.accept();
			
			// start a new thread for the request
			// so that multiple streams can be serviced
			// simultaneously
			Thread requestThread = new RequestThread(clientSocket);
			requestThread.start();
		}
	}
	
	private static Tuple<String, Socket> parseHeader(String header) throws Exception
	{
		Pattern requestPattern = Pattern.compile("^(\\w+)\\s+(\\S+)\\s+HTTP/(1\\.\\d)\\s*$");
		
		String[] lines = header.split("\r\n"); // split by the line endings
		
		StringBuilder newHeaderBuilder = new StringBuilder();
		
		String verb, url, version;
		String requestLine = null;
		String requestUrl;
		URL serverUrl = null;
		boolean foundLine = false;
		for(int i=0; i<lines.length; i++)
		{
			if(!foundLine)
			{
				Matcher requestMatcher = requestPattern.matcher(lines[i]);
				requestMatcher.matches();
				verb = requestMatcher.group(1);
				url = requestMatcher.group(2);
				version = requestMatcher.group(3);
				if(verb == null || url == null || version == null)
				{
					newHeaderBuilder.append(lines[i] + "\r\n");
					continue; // this line didn't have it
				}
				
				System.out.println("Chaning old Request Line: ");
				System.out.println("\t" + lines[i]);
				serverUrl = new URL(url);
				
				if(serverUrl.getQuery() != null)
					requestUrl = serverUrl.getPath() + "?" + serverUrl.getQuery();
				else
					requestUrl = serverUrl.getPath();
				
				requestLine = new String(verb + " " + requestUrl + " HTTP/" + version + "\r\n");
				foundLine = true;
			}
			else
			{
				newHeaderBuilder.append(lines[i] + "\r\n");
			}
		}
		
		newHeaderBuilder.append("\r\n");
		
		if(requestLine == null)
			throw new Exception("Unable to parse header. HTTP request line");
		
		
		String newHeader = requestLine + newHeaderBuilder.toString();
		System.out.println("\t" + requestLine);
		
		// now open a socket to that server
		int port;
		if((port = serverUrl.getPort()) == -1)
			port = 80;
		
		System.out.println("Opening new connection to " + serverUrl.getHost() + ":" + port);
		Socket serverSocket = new Socket(serverUrl.getHost(), port);
		
		return new Tuple<String, Socket>(newHeader, serverSocket);
	}
	
	/*
	private static Socket parseHeaderOld(String header) throws IOException
	{
		Pattern requestPattern = Pattern.compile("^(\\w+)\\s+(\\S+)\\s+HTTP/(1\\.\\d)\\s*$");
		
		if(header == null) // mind as well stop if the input is null
			return null;
		
		String[] lines = header.split("\r\n"); // split the string by lines
		String verb, url, version;
		String requestLine, requestUrl;
		URL serverUrl;
		for(int i=0; i<lines.length; i++)
		{
			Matcher requestMatcher = requestPattern.matcher(lines[i]); // attempt to match this iterations line
			requestMatcher.matches();
			verb = requestMatcher.group(1);
			url = requestMatcher.group(2);
			version = requestMatcher.group(3);
			if(verb == null || url == null || version == null)
				continue;
			
			System.out.println("Old Request Line: ");
			System.out.println("\t" + lines[i]);
			serverUrl = new URL(url);
			
			if(serverUrl.getQuery() != null)
				requestUrl = serverUrl.getPath() + "?" + serverUrl.getQuery();
			else
				requestUrl = serverUrl.getPath();
			
			requestLine = new String(verb + " " + requestUrl + " HTTP/" + version + "\r\n");
		}
		
		if(requestLine == null) // if the loop exited with a null requestLine then the header is only partially here or invalid
			return null;
		
		System.out.println("\t" + requestLine);
		
		System.out.println("Connecting to " + serverUrl.getHost() + " on port " + serverUrl.getPort());
		
		
		
		InetAddress hostAddress = InetAddress.getByName(hostname);
		
		return new Socket(hostAddress, port);
	}*/
	
	public static class RequestThread extends Thread{
		public final Socket clientSocket;
		
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
				final InputStream cis = clientSocket.getInputStream();
				final OutputStream cos = clientSocket.getOutputStream();
				
				InetAddress hostAddress = null;
				
				// keep looping until either the host is
				// determined as unknown or it has been determined
				StringBuilder requestBuilder = new StringBuilder();
				Tuple<String, Socket> result;
				String header = null;
				while(serverSocket == null)
				{
					if((bytesRead = cis.read(request)) != -1) // quit try to read bytes if -1 (means closed Socket)
					{
						requestBuilder.append(new String(request, 0, bytesRead));
						try {
							result = parseHeader(requestBuilder.toString());
							header = result.first;
							serverSocket = result.second;
						} catch (IOException e) {
							PrintWriter out = new PrintWriter(cos, true);
							out.println("Proxy server can't connect\n" + e);
							out.close();
							clientSocket.close();
							return;
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				
				System.out.println("Opening input and output streams on server socket");
				
				// get server streams
				final InputStream sis = serverSocket.getInputStream();
				final OutputStream sos = serverSocket.getOutputStream();
				
				// a thread to read the client's requests and pass them
				// to the server. This is a separate thread so that 
				// data from client to server can be processed at the same
				// time as data from server to the client
				Thread c2s = new Thread() {
					public void run() {
						int bytesRead;
						try {
							while((bytesRead = cis.read(request)) != -1)
							{
								sos.write(request, 0, bytesRead);
								sos.flush();
							}
						} catch (IOException e) {}
						
						// client must have closed the connection
						// so close the server connection
						try {
							sos.close();
						} catch (IOException e) {}
					}
				};
				
				System.out.println("Writing header to the server socket output");
				System.out.print(header);
				// before starting the thread write the header message out to the server
				try {
					sos.write(header.getBytes());
					sos.flush();
				} catch (IOException e) {
					sos.close();
					sis.close();
					cos.close();
					cis.close();
					throw e;
				}
				
				System.out.println("Now listening to client for more data"); // TODO: listen for more requests
				// start the client 2 server thread
				c2s.start();
				
				// read the server's responses and pass
				// them to the client
				try {
					while((bytesRead = sis.read(reply)) != -1)
					{
						cos.write(reply, 0, bytesRead);
						cos.flush();
					}
				} catch (IOException e) {}
				
				// the server closed its connection so close the connection
				// to the client
				cos.close();
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
}