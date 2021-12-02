package ca.camosun.ICS226;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.HashMap;

public class AsyncServer {
    private final int CMD_LENGTH = 3;
    private final int KEY_LENGTH = 8;
    private final int MIN_MESSAGE_LENGTH = 12;  //cmd of 3,8 length key + not empy message
    protected final int MAX_MESSAGE_LENGTH = 160;
    protected final String HOST = "";
    protected final String GET_CMD = "GET";
    protected final String PUT_CMD = "PUT";
    protected final String BAD_REPLY = "NO";
    protected int port;
    protected HashMap<String,String> Dict;

    public AsyncServer(int port) {
        this.port = port;

        Dict = new HashMap<String, String>(); //init an empty hash map 
    }

    //String getMessage()

    void putMessage(PrintWriter out, BufferedReader in, String fullInput){
        try{
            String key = fullInput.substring(CMD_LENGTH, (KEY_LENGTH + CMD_LENGTH)); //key length + cmd length should be 11 at time of writing 
            String message = fullInput.substring((KEY_LENGTH + CMD_LENGTH)); 

            System.out.println(key + " is key");
            System.out.println(message + " is the message");
            
        }catch(Exception e){
            System.err.println(e);
        }
    }



    //FIX ME : usrKey comes from the COMMAND LINE and should be input checked!
    void delegate(Socket clientSocket) {
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            while (true) {
                String inputLine = in.readLine();
                if (inputLine == null) {
                    break;
                }
                else if ( (inputLine.length() < MIN_MESSAGE_LENGTH) || (inputLine.length() > MAX_MESSAGE_LENGTH ) ){  //don't bother checking the commmands if the message is too short or too long
                    out.println(BAD_REPLY);
                    break;
                }
                String cmd = inputLine.substring(0, CMD_LENGTH);
                System.out.println(cmd);
                try{
                    if (cmd.equals(GET_CMD)){

                    }
                    else if (cmd.equals(PUT_CMD)){
                        putMessage(out, in, inputLine);
                    }
                    else{
                        out.println(BAD_REPLY);
                        throw new NoSuchFieldException("invalid command");
                    }
                }catch(Exception e){
                    System.err.println(e);
                }

                synchronized(this) { //this is our lock! 
                    System.out.println("Client " + Thread.currentThread() + " says: " + inputLine);
                }
                out.println(Thread.currentThread() + inputLine);// client gets this
                break;                                            //added in this break so the client quit's itself after sending a message. That means each new message ends up as a new thread. Maybe need to change?
            }
        } catch (Exception e) {
            System.err.println(e);
            System.exit(-1);
        }
    }

    //we need to have a get command, put command, and the link list setup. Client sends one message and quits. 
    //GET should come first. Empty? Prompt for new message. Loop through the list until at empty node 
    
    public void serve() {
        try (
            ServerSocket serverSocket = new ServerSocket(port);
        ) {
            while(true) {
                Socket clientSocketCopy = null;
                try {
                    Socket clientSocket = serverSocket.accept();

                    Runnable runnable = () -> this.delegate(clientSocket);
                    Thread t = new Thread(runnable);
                    t.start();
                } catch (Exception e) {
                    System.err.println(e);
                    if (clientSocketCopy != null) {
                        clientSocketCopy.close();
                    }
                    System.exit(-2);
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            System.exit(-3);
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Need <port>");
            System.exit(-99);
        }
        AsyncServer s = new AsyncServer(Integer.valueOf(args[0]));
        s.serve();
    }
}