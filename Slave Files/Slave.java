import java.io.IOException;
import java.util.ArrayList;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Slave {

    public static void main(String[] args) throws IOException, InterruptedException {

        //input: "localhost" "30124" "A"
        //args contains IP address and port number that client will use to request connection with server

        if (args.length != 3) {
            System.err.println("Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        Type type = Type.A;
        if (args[2].equalsIgnoreCase("B"))
            type = Type.B;

        ArrayList waitingPackets = new ArrayList<Packet>();
        Object wait_LOCK = new Object();
        
        ArrayList finishedPackets = new ArrayList<Packet>();
        Object finished_LOCK = new Object();
                
        try (Socket slave = new Socket(hostName, portNumber); //slave requests connection to master
             ObjectInputStream requestReader = new ObjectInputStream(slave.getInputStream()); //reads job requests from master
             ObjectOutputStream updateWriter = new ObjectOutputStream(new ObjectOutputStream(slave.getOutputStream())); //sends updates to master
             PrintWriter pw = new PrintWriter(slave.getOutputStream());) 
            {
                System.out.println("Connected.");
                Thread t1 = new SlaveThread(type, pw, requestReader, waitingPackets, wait_LOCK); //will read job requests from master
                Thread t2 = new SlaveThread(type, updateWriter, finishedPackets, finished_LOCK, pw); //will send updates to master
                Thread t3 = new SlaveThread(type, waitingPackets, wait_LOCK, finishedPackets, finished_LOCK);
                
                t1.start();
                t2.start();
                t3.start();
                
                t1.join();
                t2.join();
                t3.join();
            }
            
        catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        }
    }
}