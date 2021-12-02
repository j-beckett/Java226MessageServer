package ca.camosun.ICS226;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.HashMap;

public class AsyncServer {
    private final int CMD_LENGTH = 3;
    private final int KEY_LENGTH = 8;
    private final int KEY_COMMAND_LENGTH = (KEY_LENGTH + CMD_LENGTH);
    private final int MIN_MESSAGE_LENGTH = 12;  //cmd of 3,8 length key + not empy message
    protected final int MAX_MESSAGE_LENGTH = 160;
    protected final String HOST = "";
    protected final String GET_CMD = "GET";
    protected final String PUT_CMD = "PUT";
    protected final String BAD_REPLY = "NO";
    protected final String BAD_GET_REPLY = "\n";
    protected final String GOOD_PUT_REPLY = "OK";

    protected int port;
    protected HashMap<String,String> Dict;

    public AsyncServer(int port) {
        this.port = port;

        Dict = new HashMap<String, String>(); //init an empty hash map 
    }

    String getKey(String fullInput){
        String key = fullInput.substring(CMD_LENGTH, KEY_COMMAND_LENGTH);
        return key;
    }

    String getMessage(String fullInput){

        if (fullInput.length() < KEY_COMMAND_LENGTH){
            return BAD_GET_REPLY;
        }
        String key = getKey(fullInput);
        String message;

        synchronized(this) { //this is our lock! both put and get comands have to be done in there 
            message = Dict.get(key);
        }

        if (message == null)
            return BAD_GET_REPLY;

        return message;
        

    }

    void putMessage(PrintWriter out, String fullInput){
        try{
            if (fullInput.length() < MIN_MESSAGE_LENGTH){
                out.println(BAD_REPLY);
                return;
            } 

            String key = getKey(fullInput); //key length + cmd length should be 11 at time of writing 
            String message = fullInput.substring(KEY_COMMAND_LENGTH); 
            System.out.println(message.length());

            if((message.length() == 0) || (message.equals(" ")) || (message.length() > MAX_MESSAGE_LENGTH )){
                out.println(BAD_REPLY);
                return;
            } 

            if (!getMessage(fullInput).equals(BAD_GET_REPLY)){
                out.println(BAD_REPLY);
                return;
            }

            synchronized(this) { //this is our lock! both put and get comands have to be done in there 
                Dict.put(key, message);
            }
            out.println(GOOD_PUT_REPLY);
            
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
            //while (true) {
                String inputLine = in.readLine();
                if (inputLine == null) {
                    return;
                }
                // else if ( inputLine.length() > MAX_MESSAGE_LENGTH  ){  //don't bother checking the commmands if the message is too long
                //     out.println(BAD_REPLY);
                //     break;
                // }
                String cmd = inputLine.substring(0, CMD_LENGTH);
                System.out.println(cmd);
                try{
                    if (cmd.equals(GET_CMD)){
                        out.println(getMessage(inputLine));
                    }
                    else if (cmd.equals(PUT_CMD)){
                        putMessage(out, inputLine);
                    }
                    else{
                        out.println(BAD_REPLY);
                        throw new NoSuchFieldException("invalid command");
                    }
                }catch(Exception e){
                    System.err.println(e);
                }

                //synchronized(this) { //this is our lock! both put and get comands have to be done in there 
                    //System.out.println("Client " + Thread.currentThread() + " says: " + inputLine);
                //}
                //out.println(Thread.currentThread() + inputLine);// client gets this
                //break;                                            //added in this break so the client quit's itself after sending a message. That means each new message ends up as a new thread. Maybe need to change?
           // }
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