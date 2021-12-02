package ca.camosun.ICS226;
import java.net.*; 
import java.io.*;
import java.nio.channels.IllegalBlockingModeException;

public class Server { 
    protected int port;
        public Server(int port) { 
            this.port = port;
        }

        public void serve() {
            try (
                ServerSocket serverSocket = new ServerSocket(port); Socket clientSocket = serverSocket.accept();
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
            ) { while (true) {
                    String inputLine = in.readLine();
                    if (inputLine.equals("\n")) {
                        break;
                    }
                    System.out.println(inputLine);
                    out.println("Client said: " + inputLine);
                }
                } catch (IOException e) {
                    System.err.println(e);
                    System.exit(-2);
                } catch (SecurityException e) {
                    System.err.println(e);
                    System.exit(-3);
                } catch (IllegalArgumentException e) {
                    System.err.println(e);
                    System.exit(-4);
                } catch (IllegalBlockingModeException e) {
                    System.err.println(e);
                    System.exit(-6);
                }
        }

        // public static void main(String[] args){

        //     while (true){
        //       Server serv = new Server(12345);
        //       serv.serve();
        //     }
        //   }




}
