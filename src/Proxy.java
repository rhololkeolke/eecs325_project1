import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Proxy {
	
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
		
		InetAddress hostAddress = InetAddress.getByName(hostname);
		
		return new Socket(hostAddress, port);
	}
	
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
				while(serverSocket == null)
				{
					if((bytesRead = cis.read(request)) != -1) // quit try to read bytes if -1 (means closed Socket)
					{
						try {
							serverSocket = parseHeader(new String(request, 0, bytesRead));
						} catch (IOException e) {
							PrintWriter out = new PrintWriter(cos, true);
							out.println("Proxy server can't connect\n" + e);
							out.close();
							clientSocket.close();
							return;
						}
					}
				}
				
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
				
				// before starting the thread write the header message out to the server
				try {
					sos.write(request, 0, bytesRead);
					sos.flush();
				} catch (IOException e) {
					sos.close();
					sis.close();
					cos.close();
					cis.close();
					throw e;
				}
				
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