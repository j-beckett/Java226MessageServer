package ca.camosun.ICS226;
import java.io.*;
import java.net.*;
import java.nio.channels.*;

public class Client{

	protected String serverName;
	protected int serverPort;
	protected String message;

	public Client(String serverName, int serverPort, String message) {
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.message = message;
	}

	public void connect() {
		String reply;

		try (
			Socket socket = new Socket(serverName, serverPort);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));

		) {
			while (true) {
				out.println(this.message);
				if ((reply = in.readLine()) == null) {
					break;
				}
				System.out.println(reply);
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			System.err.println(e);
			System.exit(-1);
		} 
	}

	public static void main(String[] args){
		if (args.length != 3) {
			System.err.println("Need <host> <port> <message>");
			System.exit(-2);
		}
		Client c = new Client(args[0], Integer.valueOf(args[1]), args[2]);
		c.connect();
	}

}
