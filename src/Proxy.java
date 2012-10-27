import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
			
			PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			
			String clientInput;
			while((clientInput = in.readLine()) != null)
			{
				System.out.println(clientInput);
			}
			out.close();
			in.close();
			client.close(); // TODO spawn a request thread here
		} catch (IOException e1) {
			System.err.println("ERROR: Problem accepting client request");
			System.exit(1);
		}
		
		// Clean up
		try {
			System.out.println("Shutting down Proxy Server on port " + PORT);
			server.close();
		} catch (IOException e) {
			System.err.println("ERROR: Problem closing socket on port " + PORT);
			System.exit(1);
		}
	}

}
