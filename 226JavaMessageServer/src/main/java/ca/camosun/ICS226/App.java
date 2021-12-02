package ca.camosun.ICS226;
import java.net.*; 
import java.io.*;
import java.nio.channels.IllegalBlockingModeException;
/**
 * Hello world!
 *
 */
public class App 
{
    public static void main(String[] args){
        while (true){
          Server serv = new Server(12345);
          serv.serve();
        }
      }
}
