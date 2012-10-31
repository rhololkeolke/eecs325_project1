import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Proxy2 {
  public static void main(String[] args) throws IOException {
    try {
      int localport = 9090;
      // Print a start-up message
      System.out.println("Starting proxy for on port " + localport);
      // And start running the server
      runServer(localport); // never returns
    } catch (Exception e) {
      System.err.println(e);
    }
  }

  /**
   * runs a single-threaded proxy server on
   * the specified local port. It never returns.
   */
  public static void runServer(int localport)
      throws IOException {
    // Create a ServerSocket to listen for connections with
    ServerSocket ss = new ServerSocket(localport);

    

    while (true) {
      Socket client = null;
      try {
        // Wait for a connection on the local port
        client = ss.accept();

        Thread requestThread = new RequestThread(client);
        requestThread.start();
	    } catch(IOException e) {
	    	
	    }
    }
      
  }
 
  private static InetAddress parseHeader(String header) throws UnknownHostException
  {
	  int start, end;
	  if((start = header.indexOf("Host: ") + 6) < 0)
		  return null;
	  
	  if((end = header.indexOf('\n', start)) < 0)
		  return null;
	  
	  String hostName = header.substring(start, end-1);
	  
	  System.out.println("Connecting to host " + hostName);
	  
	  return InetAddress.getByName(hostName);
  }
  
  public static class RequestThread extends Thread {
	  public final Socket client;
	  
	  public RequestThread(Socket c)
	  {
		  client = c;
	  }
	  
	  public void run() {
		  int bytesRead = 0;
		  final byte[] request = new byte[1024];
		  byte[] reply = new byte[4096];
		  Socket server = null;
		  try {
			  final InputStream streamFromClient = client.getInputStream();
		      final OutputStream streamToClient = client.getOutputStream();
		              
		      InetAddress hostAddress = null;
		      while(hostAddress == null)
		      {
		    	  if((bytesRead = streamFromClient.read(request)) != -1)
		    	  {
		    		  hostAddress = parseHeader(new String(request, 0, bytesRead));
		    	  }
		      }
		      		      
		      // Make a connection to the real server.
		      // If we cannot connect to the server, send an error to the
		      // client, disconnect, and continue waiting for connections.
		      try {
		        server = new Socket(hostAddress, 80);
		      } catch (IOException e) {
		        PrintWriter out = new PrintWriter(streamToClient);
		        out.print("Proxy server cannot connect to " + hostAddress.getHostName() + ":"
		            + 80 + ":\n" + e + "\n");
		        out.flush();
		        client.close();
		        return;
		      }
		
		      // Get server streams.
		      final InputStream streamFromServer = server.getInputStream();
		      final OutputStream streamToServer = server.getOutputStream();
		
		      // a thread to read the client's requests and pass them
		      // to the server. A separate thread for asynchronous.
		      Thread t = new Thread() {
		        public void run() {
		          int bytesRead;
		          try {
		            while ((bytesRead = streamFromClient.read(request)) != -1) {
		          	//System.out.println(new String(request, 0, bytesRead));
		              streamToServer.write(request, 0, bytesRead);
		              streamToServer.flush();
		            }
		          } catch (IOException e) {
		          }
		
		          // the client closed the connection to us, so close our
		          // connection to the server.
		          try {
		            streamToServer.close();
		          } catch (IOException e) {
		          }
		        }
		      };
		
		      // before starting write the header message out to the server
		      try{
		      	streamToServer.write(request, 0, bytesRead);
		      	streamToServer.flush();
		      } catch (IOException e) {
		      	streamToServer.close();
		      	streamFromServer.close();
		      	streamToClient.close();
		      	streamFromClient.close();
		      	throw e;
		      }
		  	// Start the client-to-server request thread running
		  	t.start();
		
		      // Read the server's responses
		      // and pass them back to the client.
		      try {
		        while ((bytesRead = streamFromServer.read(reply)) != -1) {
		      	streamToClient.write(reply, 0, bytesRead);
		          streamToClient.flush();
		        }
		      } catch (IOException e) {
		      }
		
		      // The server closed its connection to us, so we close our
		      // connection to our client.
		      streamToClient.close();
	    } catch (IOException e) {
	      System.err.println(e);
	    } finally {
	      try {
	        if (server != null)
	          server.close();
	        if (client != null)
	          client.close();
	      } catch (IOException e) {
	      }
	    }
	  }
	  
  }
  
}