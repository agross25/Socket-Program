import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Client {

        
    public static void main(String[] args) throws IOException, InterruptedException {

        //input: "localhost" "30121" "A"
        if (args.length != 3) {
            System.err.println("Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        final String NAME = args[2]; //name of this specific client
        
        ArrayList newPackets = new ArrayList<Packet>();
        Object newP_LOCK = new Object();
        
        ArrayList sentPackets = new ArrayList<Packet>();
        Object sentP_LOCK = new Object();
        
        ArrayList finishedPackets = new ArrayList<Packet>();
        Object finishedP_LOCK = new Object();

        //requests connection to master
        try (Socket client = new Socket(hostName, portNumber); //requests connection to master
             ObjectOutputStream requestWriter = new ObjectOutputStream(client.getOutputStream()); //write job requests to master
             ObjectInputStream updateReader = new ObjectInputStream(client.getInputStream());) //read job updates from master
            {
                System.out.println("Connected.");
                Thread t1 = new Thread(new ClientThread(NAME, newPackets, newP_LOCK)); //will read user input
                Thread t2 = new Thread(new ClientThread(NAME, requestWriter, newPackets, newP_LOCK, sentPackets, sentP_LOCK)); //will send requests to master
                Thread t3 = new Thread(new ClientThread(NAME, updateReader, sentPackets, sentP_LOCK, finishedPackets, finishedP_LOCK));
                    
                t1.start();
                t2.start();
                t3.start();
                
                t1.join();
                
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        }
    }
    
}
