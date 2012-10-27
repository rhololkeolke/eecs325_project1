import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;


public class SimpleProxy {
	
	public static void main(String[] args) {
		
		Socket client = null;
		Socket server = null;
		ServerSocket listener = null;

		try {
			listener = new ServerSocket(9090);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			client = listener.accept();
			
			BufferedReader clientIn = new BufferedReader(new InputStreamReader(client.getInputStream()));
			PrintWriter clientOut = new PrintWriter(client.getOutputStream(), true);
			
			List<String> data = new LinkedList<String>(); 
					
			System.out.println("\n\nStarting to print client data\n\n");
			String clientLine;
			while((clientLine = clientIn.readLine()) != null && !clientLine.equals(""))
			{
				data.add(clientLine);
				System.out.println(clientLine + "ZzZzZ");
			}
			
			System.out.println("\n\nOpening new socket\n\n");
			server = new Socket("www.google.com", 80);
			
			BufferedReader serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));
			PrintWriter serverOut = new PrintWriter(server.getOutputStream(), true);
			
			System.out.println("\n\nPrinting data to server socket\n\n");
			for(int i=0; i<data.size(); i++)
			{
				serverOut.print(data.get(i) + "\r");
			}
			
			data = new LinkedList<String>();
			
			System.out.println("\n\nReceving data from server\n\n");
			String serverLine;
			while((serverLine = serverIn.readLine()) != null && !serverLine.equals(""))
			{
				data.add(serverLine);
				System.out.println(serverLine);
			}
			
			/*
			serverOut.println("GET http://www.google.com/ HTTP/1.1\r");
			serverOut.println("Host: google.com\r");
			serverOut.println("\r");
			
			String serverLine;
			while((serverLine = serverIn.readLine()) != null)
			{
				System.out.println(serverLine);
			}*/
			
			clientIn.close();
			clientOut.close();
			client.close();
			
			serverIn.close();
			serverOut.close();
			server.close();
			
			
			listener.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
