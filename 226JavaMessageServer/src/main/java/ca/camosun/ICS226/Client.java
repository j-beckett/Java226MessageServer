
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.zip.*;

public class Client {

	protected String serverName;
	protected int serverPort;
	protected String message;

	public Client(String serverName, int serverPort, String message) {
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.message = message;
	}

	public void connect() {
		try (
			Socket socket = new Socket(serverName, serverPort);
			PrintWriter out = new PrintWriter(new GZIPOutputStream(socket.getOutputStream()) );
			BufferedReader in = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));

		) {
			//outMsg.write(message.getBytes(),0, message.length());
			out.println(message);
			System.out.println(in.readLine());
		} catch (UnknownHostException e) {
			System.err.println(e);
			System.exit(-1);
		} catch (IOException e) {
			System.err.println(e);
			System.exit(-2);
		} catch (SecurityException e) {
			System.err.println(e);
			System.exit(-3);
		} catch (IllegalArgumentException e) {
			System.err.println(e);
			System.exit(-4);
		}
	}

	public static void main(String[] args){

		Client cli = new Client("localhost", 12345, "This is a test message!!!!");
	
		cli.connect();
	}

}
