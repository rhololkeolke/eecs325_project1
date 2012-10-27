import java.io.BufferedReader;
import java.io.IOException;
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
	private BufferedReader in;
	private PrintWriter out;
	
	private String name;
	
	public ForwardingRunner(String name, BufferedReader in, PrintWriter out)
	{
		this.name = name;
		this.in = in;
		this.out = out;
	}

	@Override
	public void run() {
		
		String inputLine;
		try {
			while((inputLine = in.readLine()) != null)
			{
				out.print(inputLine + "\r\n");
				System.out.print(this.name + ": " + inputLine + "\r\n");
			}
		} catch (IOException e) {
			System.err.println(this.name + " buffer closed. Ending thread");
		}

		
	}
	
}
