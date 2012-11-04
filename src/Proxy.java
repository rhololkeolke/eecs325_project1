import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Proxy {
	
	public final static int PORT = 5030;
		
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
			final byte[] request = new byte[4096];
			
			Socket serverSocket = null;
			try {
				final OutputStream cos = clientSocket.getOutputStream(); 
				
				/*
				 * This section will parse the header and create a new one to send to the server
				 */

				// the header will be in standard ASCII so its fine to use a BufferedReader
				BufferedReader cbr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				
				String headerLine;
				// keep looping until the 
				while(true) {
					if((headerLine = cbr.readLine()) == null) // null if stream is closed. NOTE: This will block until something is read or stream is closed
					{
						return;
					}
					

					
					// parse the first line of the request
					Matcher headerMatcher = requestPattern.matcher(headerLine);
					headerMatcher.matches();
					String verb = headerMatcher.group(1);
					String url = headerMatcher.group(2);
					String version = headerMatcher.group(3);
					
					
					if(verb == null || url == null || version == null)
						throw new IOException("Error reading request header");
	
					
					// parse out the path from the url
					URL serverUrl = new URL(url);
					
					String requestUrl;
					if(serverUrl.getQuery() != null)
						requestUrl = serverUrl.getPath() + "?" + serverUrl.getQuery();
					else
						requestUrl = serverUrl.getPath();
					
					// build the header
					String newHeaderLine = new String(verb + " " + requestUrl + " HTTP/" + version + "\r\n");

					// Now that the new header is created open a socket to the host in the url on the port specified
					
					// set the port number
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
					OutputStream sos = serverSocket.getOutputStream();
					
					System.out.print(newHeaderLine);
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
						
						// see if this is the content-length line
						Matcher contentLengthMatcher = contentLengthPattern.matcher(headerLine);
						if(contentLengthMatcher.matches())
						{
							// convert the content length number from a string to an integer
							contentLen = Integer.parseInt(contentLengthMatcher.group(1));
						}
						
						// according to wikipedia the proxy-connection header isn't actually
						// a valid header. However, many browsers will use it
						// since it isn't supposed to be valid I have removed it when present.
						if(headerLine.matches("^Proxy-Connection:.*"))
						{
							continue;
						}
						else if(headerLine.equals(""))
						{
							System.out.print("Connection: close\r\n\r\n");
							sos.write("Connection: close\r\n\r\n".getBytes());
							//sos.write("\r\n".getBytes());
							sos.flush();
							
							// would get the next n chars where n is the number in content-length
							// this should handle POST requests
							char[] cbuf = new char[4096];
							int charsRead = 0;
							String postdata;
							while(contentLen > 0)
							{
								if((charsRead = cbr.read(cbuf, 0, request.length)) != -1)
								{
									postdata = new String(Arrays.copyOfRange(cbuf, 0, charsRead));
									System.out.print(postdata);
									sos.write(postdata.getBytes());
									sos.flush();
									contentLen -= charsRead;
								}
							}
							
							// all done reading the request
							System.out.println("\n\nFinished reading header");
							System.out.println();
							break;
						}
						else
						{
							// if it isn't a special line then
							// simply copy what was received to the server
							System.out.print(headerLine + "\r\n");
							sos.write((headerLine + "\r\n").getBytes());
							sos.flush();
						}
					}
	
					// this thread will listen for the server's response and send it 
					// back to the client
					Server2ClientThread s2c = new Server2ClientThread(serverSocket, cos);
					s2c.start();
				}
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
	
	// This class handles listening for data from the server
	// and passing it back to the client
	public static class Server2ClientThread extends Thread
	{
		private final Socket serverSocket;
		private final OutputStream cos;
		
		public Server2ClientThread(Socket s, OutputStream os)
		{
			serverSocket = s;
			cos = os;
		}
		
		public void run() {
			try {
				InputStream sis = serverSocket.getInputStream();
				int bytesRead;
				byte[] reply = new byte[4096];
				while((bytesRead = sis.read(reply)) != -1)
				{
					cos.write(reply, 0, bytesRead);
					cos.flush();
				}
			} catch (IOException e) {
				System.err.println(e);
			}
			
			try {
				serverSocket.close();
			} catch (IOException e) {}
		}
	}
}