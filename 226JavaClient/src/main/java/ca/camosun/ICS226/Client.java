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


    //Overload these functions, one takes the global currentKey, the other accepts the nextKey as a param. 
    //This prevents the two functions from modifying the same global at the same time.

    public String generateMessageToServer(String usrMessage){
        String randomKey = generateKey();

        return (PUT_CMD + currentKey + randomKey + usrMessage + "\n");
    }

    public String generateMessageToServer(String usrMessage, String nextKey){
        String randomKey = generateKey();

        return (PUT_CMD + nextKey + randomKey + usrMessage + "\n");

    }


    public String startConnection(String message_to_send){

        try (
			Socket socket = new Socket(serverName, serverPort);
			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);  //this is our reader / writer 
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
		)  {
            writer.println(message_to_send);
            String data = reader.readLine();
            if (data.length() > 2)
                System.out.println("Received: " + data.substring((CMD_LENGTH + KEY_LENGTH)));  //print prettier plz

            else
                System.out.println("Received: " + data);
            return data;

           // return data;

        }catch (Exception e) {
			System.err.println(e);
			System.exit(-5); //exit with different code so I can see it's from this function 
		}    

        return "";
    }


//this function throws indexOutOfBounds while checking... I just wrapped it up in a try catch block so it doesn't bother us :))
    public void receiveMesage(){ 

        while (true){   
            try{
                String data = startConnection( GET_CMD + currentKey + '\n');
        
                while(data.substring(0, CMD_LENGTH).equals(MESSAGE_EXISTS)){ //if NO message was recieved, that means there is a message at that key! DO WHIKLE?
                    synchronized(this) {                                    //should prevent a race condition with the CcurrKey
                        currentKey = data.substring(CMD_LENGTH, (CMD_LENGTH + KEY_LENGTH)); // gets key located after the NO message that is KEY_LENGTH (8) long. If we changed the message then this shouldn't break 
                    }

                    data = startConnection( GET_CMD + currentKey + '\n');
                    //System.out.println(data);
                }
                
            }catch(Exception e) {
                //System.err.println(e);
            }

            try{
                Thread.sleep(PING_IN_MILLISECONDS);           //try to sleep in this funct as I want recMes to be in control of the pinging?
                                            

            }catch(InterruptedException e) {
                System.err.println(e);
                System.exit(-666); //exit with different code so I can see it's from this function 
            } 
            
        }
    }



    

//Our writeMessage thread. This function will call promptForMessage to accquire a message from user, then goes through the process to sending it through to the server.
//If a message exists at currentKey (another client or process beat us to that key) continue to loop "down the list" until an empty node is found.
//this function throws indexOutOfBounds while checking... I just wrapped it up in a try catch block so it doesn't bother us :))

    public void writeMessage(){
        Scanner scanny = new Scanner(System.in);
        while (true){
            try{
                String data; 
                String nextMessage = promptForMessage(scanny);
                System.out.println(nextMessage);

                synchronized(this) {
                    data = startConnection(generateMessageToServer(nextMessage, currentKey)); //block while accessing current key. allows message to generate in peace before send
                }                                  
                
                while(data.substring(0, CMD_LENGTH).equals(MESSAGE_EXISTS)){  //message exist at key, so loop thru until finding empty node
                    String nextKey = data.substring(CMD_LENGTH, CMD_LENGTH + KEY_LENGTH );
                    synchronized(this) {
                        data = startConnection(generateMessageToServer(nextMessage,nextKey)); //block while accessing current key. allows message to generate in peace before send. 
                    }                                                               //nextKey allows to loop through without interfearing with the getMessage operations.
                }
            }catch (Exception e){
            }
            try{
                Thread.sleep(PING_IN_MILLISECONDS);

            }catch(InterruptedException e) {
                System.err.println(e);
                System.exit(-666); //exit with different code so I can see it's from this function 
            } 
    }

    }

    // while (true){
    //     Runnable runnable = () -> {
    //         String data =     startConnection( GET_CMD + currentKey + '\n'); //can't get data out of the lambada functioon?
    //     };
    //     try{
    //         Thread.sleep(2000);

    //     }catch(InterruptedException e) {
    //         System.err.println(e);
    //         System.exit(-666); //exit with different code so I can see it's from this function 
    //     } 
    //     Thread t = new Thread(runnable);
    //     t.start();

	public void connect() {
		String reply;

		try (
			Socket socket = new Socket(serverName, serverPort);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);  //this is our reader / writer 
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
		) {
            
            generateKey();
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
            //System.out.println(reply);
            //Thread.sleep(1000);
    
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
        //System.out.println(args[3] + " third arg");
		Client c = new Client(args[0], Integer.valueOf(args[1]), args[2]);
		c.connect();
	}

}
