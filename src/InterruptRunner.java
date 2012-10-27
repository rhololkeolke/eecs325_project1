import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;


public class InterruptRunner implements Runnable {
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
	 * This class watches a thread and throws the InterruptedException
	 * If that thread is interrupted.
	 */

	private Thread watched;
	
	private Socket host;
	private Socket client;
	
	private BufferedReader bis;
	private OutputStream bos;
	private InputStream sis;
	private PrintWriter sos;

	public InterruptRunner(Thread watched, Socket client,
			Socket host, BufferedReader bis, OutputStream bos,
			InputStream sis, PrintWriter sos) {
		this.watched = watched;
		this.client = client;
		this.host = host;
		this.bis = bis;
		this.bos = bos;
		this.sis = sis;
		this.sos = sos;
	}

	@Override
	public void run() {

		try {
			// just wait
			while(watched.isAlive() && !watched.isInterrupted())
			{
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			}
			
			// I know this is bad, but I don't know how else
			// to guarantee the thread stops
			// watched.destroy();
			
			// keep closing the connections
			// this should cause the thread this is watching
			// to finish its run method
			while(watched.isAlive())
			{
				if(client != null)
					client.close();
				if(host != null)
					host.close();
				if(bis != null)
					bis.close();
				if(bos != null)
					bos.close();
				if(sis != null)
					sis.close();
				if(sos != null)
					sos.close();
			}
			
			
		} catch (IOException e) {	}
	}
	
}
