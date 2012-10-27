import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;


public class ForwardingRunner implements Runnable {
	/*
	 * Class: ForwardingThread
	 * 
	 * Author: Devin Schwab
	 * 
	 * Case ID: dts34
	 * 
	 * Created: 10/26/12
	 * 
	 * Description
	 * ------------
	 * This class is used to forward data from
	 * one socket to another.
	 */

	// The data from the reader is echoes to the writer
	private InputStream is;
	private OutputStream os;
	
	private String name;
	
	public ForwardingRunner(String name, InputStream is, OutputStream os)
	{
		this.name = name;
		this.is = is;
		this.os = os;
	}

	@Override
	public void run() {
		
		byte[] buffer = new byte[10000];
		int n = 0;

		try {
			do {
				n = is.read(buffer);
				System.out.println(name + ": Receiving " + n + " bytes");
				if (n > 0) {
					os.write(buffer, 0, n);
				}
			} while( n > 0);
			
			os.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
