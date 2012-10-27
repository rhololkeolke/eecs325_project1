import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;


class InputWatcherRunner implements Runnable {

	private boolean quit;
	private ServerSocket server;
	
	private BufferedReader stdIn;
	
	public InputWatcherRunner(boolean quit, ServerSocket server)
	{
		this.quit = quit;
		this.server = server;
		
		stdIn = new BufferedReader(new InputStreamReader(System.in));
	}
	
	@Override
	public void run() {
		String userInput;
		try {
			while((userInput = stdIn.readLine()) != null)
			{
				try{
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			}
			quit = true;
			// double check if this is thread safe
			server.close(); // This ensures that the process won't block on server.accept()
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}