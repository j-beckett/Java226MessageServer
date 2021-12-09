package ca.camosun.ICS226;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.nio.charset.*;

public class Client{
    private final int CMD_LENGTH = 3;
    private final int KEY_LENGTH = 8;

    protected final int MAX_MESSAGE_LENGTH = 160;
    protected final int PING_IN_MILLISECONDS = 5000;
    protected final String HOST = "";
    protected final String GET_CMD = "GET";
    protected final String PUT_CMD = "PUT";
    protected final String MESSAGE_EXISTS = "NO ";
	protected String serverName;
	protected int serverPort;
	protected String message;

    public static String currentKey;

	public Client(String serverName, int serverPort, String message) {
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.message = message;

        currentKey = message; //assigns currentKey from the parm on the command line. We should do some error checking here!
	}


    //prompts user for message. Passes in the scanner object, which is created in the writeMessage function.
    //do this because if we close the scanner once, we close it forever?
    //returns the user entered input. Could do some error checking here.
    public String promptForMessage(Scanner scan){
        System.out.print("Input message >> ");
        return scan.nextLine();
    }


    //takes no params, generates a random key for the next message in the linked list
    // get random key of length KEY_LENGTH ( 8 ) with letters(upperCase & lowercase) + digits
    //this is "psudeorandom"
    private String generateKey(){
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(KEY_LENGTH);

        for (int i = 0; i < KEY_LENGTH; i++) {

            // generate a random number between
            // 0 to AlphaNumericString length.
            int index= (int)(AlphaNumericString.length() * Math.random());
            // add Character one by one in end of sb
            sb.append(AlphaNumericString.charAt(index));
        }
        //System.out.println(sb); 
        return sb.toString();
    }


//generates the PUT message to the server. Calls on generateKey to make us a nice new key for the next node.
//concatenates the key with the current key, and user inputted message to make a complete string.
// Note: currKey could be EITHER the global currentKey, OR the nextKey from write message. I did this to prevent the two threads messing up a shared resource.

    public String generateMessageToServer(String usrMessage, String currKey){
        String randomKey = generateKey();
        return (PUT_CMD + currKey + randomKey + usrMessage + "\n");
    }



//this function throws indexOutOfBounds while checking... I just wrapped it up in a try catch block so it doesn't bother us :))
//this continues to ping the server checking for new messages. 
//cycle through all nodes with messages to display everything to the client. ping the empty node until a message comes in!
//this function runs on it's own thread ( like write message)
    public void receiveMesage(){ 
        while (true){   
            try{

                String data = startConnection( GET_CMD + currentKey + '\n');
        
                while(data.substring(0, CMD_LENGTH).equals(MESSAGE_EXISTS)){ //if NO message was recieved, that means there is a message at that key!
                    synchronized(this) {                                    //should prevent a race condition with the CcurrKey
                        currentKey = data.substring(CMD_LENGTH, (CMD_LENGTH + KEY_LENGTH)); // gets key located after the NO message that is KEY_LENGTH (8) long. If we changed the message then this shouldn't break 
                    }

                    data = startConnection( GET_CMD + currentKey + '\n');
                    //System.out.println(data);
                }
                
            }catch(Exception e) {}  //do nothing with this exception.... just pretend it's not there :) 
                
            try{
                Thread.sleep(PING_IN_MILLISECONDS);  //sleep for the time listed above
                                            
            }catch(InterruptedException e) {
                System.err.println(e);
                System.exit(-123); //exit with different code so I can see it's from this function 
            } 
        }
    }

    

//Our writeMessage thread. This function will call promptForMessage to accquire a message from user, then goes through the process to sending it through to the server.
//If a message exists at currentKey (another client or process beat us to that key) continue to loop "down the list" until an empty node is found.
//this function throws indexOutOfBounds while checking... I just wrapped it up in a try catch block so it doesn't bother us :))
//this function runs on it's own thread ( like Recievemessage)

    public void writeMessage(){
        Scanner scanny = new Scanner(System.in);
        while (true){
            try{

                String data; 
                String nextMessage = promptForMessage(scanny);
                //System.out.println(nextMessage);

                synchronized(this) {
                    data = startConnection(generateMessageToServer(nextMessage, currentKey)); //block while accessing current key. allows message to generate in peace before send
                }                                  
                
                while(data.substring(0, CMD_LENGTH).equals(MESSAGE_EXISTS)){  //message exist at key, so loop thru until finding empty node
                    String nextKey = data.substring(CMD_LENGTH, CMD_LENGTH + KEY_LENGTH );
                    synchronized(this) {
                        data = startConnection(generateMessageToServer(nextMessage,nextKey)); //block while accessing current key. allows message to generate in peace before send. 
                    }                                                               //nextKey allows to loop through without interfearing with the getMessage operations.
                }
            }catch (Exception e){} //do nothing with the exception .. could prob do this nicer

            try{
                Thread.sleep(PING_IN_MILLISECONDS);

            }catch(InterruptedException e) {
                System.err.println(e);
                System.exit(-666); //exit with different code so I can see it's from this function 
            } 
        }

    }


    
    //Creates one instance of a socket connection to write a message, then read the reply recieved from the server.
    //Only function that prints the recieved messages to console. 
    //returns the data response from the server in String format to the calling function. 
    //shouldn't throw an exception but wrapped in a try catch block to play it safe
    public String startConnection(String message_to_send){

        try ( //put reader / writer in the parenth so they auto - close
			Socket socket = new Socket(serverName, serverPort);
			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);  //this is our reader / writer 
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
		)  {
            writer.println(message_to_send);
            String data = reader.readLine();
            if (data.length() > CMD_LENGTH)
                System.out.println('\n' + "Received: " + data.substring((CMD_LENGTH + KEY_LENGTH))); //prints nicely the user entered message without nextKey or any commands
                                                                                            //newlines at the start so it displays a lil nicer
            else
                System.out.println('\n' + "Received: " + data);
            return data;

        }catch (Exception e) {
			System.err.println(e);
			System.exit(-5); //exit with different code so I can see it's from this function 
		}    

        return "-1"; //should never reach this; if it does... we have a problem!
    }

//first stop after our client boots up. No params, no return type. 
//This function creates two threads for our client to use. One for receiveMessage, one for writeMessage.
//these threads should run forever (each have their own infinate loop)
	public void connect() {
		String reply;

		try (
			Socket socket = new Socket(serverName, serverPort);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);  //this is our reader / writer 
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
		) {
            Runnable runnable = () -> {
                receiveMesage();
            };
            Thread t = new Thread(runnable);
            t.start();
        
            Runnable runny = () -> {
                writeMessage();
            };
            Thread thready = new Thread(runny); //these must be named differently to start diff threads?
            thready.start();
            
            if ((reply = in.readLine()) == null) { //if inturrpted and line read in is null, return
                return;
            }

		} catch (Exception e) {
			System.err.println(e);
			System.exit(-1);
		} 
	}

	public static void main(String[] args){
		if (args.length < 3) {
			System.err.println("Need <host> <port> <message>");
			System.exit(-2);
		}
        
		Client c = new Client(args[0], Integer.valueOf(args[1]), args[2]);
		c.connect();
	}

}
