import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class SimpleProxy {
	
	public static void main(String[] args) {

		try {
			System.out.println("Starting the SimpleProxy");
			
			ServerSocket serverSocket = new ServerSocket(9090);
			
			while(true) {
				Socket clientSocket = serverSocket.accept();
				
				BufferedReader bis = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				
				// read the request
				System.out.println("Reading the request");
				StringBuilder browserRequestBuilder = new StringBuilder();
				String bLine;
				while((bLine = bis.readLine()) != null)
				{
					browserRequestBuilder.append(bLine + "\r\n");
					System.out.println(bLine + "\r");
					
					if(bLine.equals("")) // HTTP headers end with a blank line
						break;
				}
				
				String browserRequest = browserRequestBuilder.toString();
				
				// extract the host to connect to
				System.out.println("Extracting the host");
				int start = browserRequest.indexOf("Host: ") + 6;
				int end = browserRequest.indexOf("\n", start);
				String host = browserRequest.substring(start, end-1);
				System.out.println("Connecting to host " + host);
				
				// forward the response from the proxy to the server
				System.out.println("Forwarding the response to the server");
				Socket hostSocket = new Socket(host, 80);
				
				System.out.println("ip.dst == " + hostSocket.getInetAddress().getHostAddress() + " and tcp.srcport == " + hostSocket.getLocalPort());
				
				PrintWriter sos = new PrintWriter(hostSocket.getOutputStream(), true);
				sos.print(browserRequest);
				sos.flush();
				
				// forward the response from the server to the browser
				System.out.println("Forwarding the response from the server to the browser");
				
				InputStream sis = hostSocket.getInputStream();
				OutputStream bos = clientSocket.getOutputStream();
				

				int n = 0;
				byte[] buffer = new byte[10000];
				do {
					n = sis.read(buffer);
					System.out.println("Receving " + n + " bytes");
					if(n > 0)
					{
						bos.write(buffer, 0, n);
					}
				} while (n > 0);
				
				
				
				bis.close();
				bos.close();
				clientSocket.close();
			
				sis.close();
				sos.close();
				hostSocket.close();
			}

				
			//serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
