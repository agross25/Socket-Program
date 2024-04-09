import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientThread extends Thread {
    
    private ObjectOutputStream writer;
    private ObjectInputStream reader;
    private final int threadID; //what type of thread
    private final String CLIENT_NAME;
    private ArrayList<Packet> newPackets;
    private final Object newP_LOCK;
    private ArrayList<Packet> sentPackets;
    private final Object sentP_LOCK;   
    private ArrayList<Packet> finishedPackets;
    private final Object finishedP_LOCK;
    private static int jobID = 1; //will be used to assign job IDs
    private static final Object jobID_LOCK = new Object(); //will be used in synchronized block to update jobID
    
    
    //constructor 1 - will read user's job requests and add to newPackets list
    public ClientThread(String name, ArrayList<Packet> newP, Object lock)
    {
        CLIENT_NAME = name;
        threadID = 1;
        newPackets = newP;
        newP_LOCK = lock;
        sentP_LOCK = new Object(); //will not be used
        finishedP_LOCK = new Object(); //will not be used
    }
    
    //constructor 2 - will send new job requests to master
    public ClientThread(String name, ObjectOutputStream w, ArrayList<Packet> newP, Object newLock, ArrayList<Packet> sentP, Object sentLock)
    {
        CLIENT_NAME = name;
        threadID = 2;
        writer = w;
        newPackets = newP;
        newP_LOCK = newLock;
        sentPackets = sentP;
        sentP_LOCK = sentLock;
        finishedP_LOCK = new Object(); //will not be used
    }
    
    //constructor 3 - reads updates from master
    public ClientThread(String name, ObjectInputStream r, ArrayList<Packet> sentP, Object sentLock, ArrayList<Packet> finishedP, Object finishedLock)
    {
        CLIENT_NAME = name;
        threadID = 3;
        reader = r;
        sentPackets = sentP;
        sentP_LOCK = sentLock;
        finishedPackets = finishedP;
        finishedP_LOCK = finishedLock;
        newP_LOCK = new Object(); //will not be used
    }
 
    @Override
    public void run()
    {
        try {
            System.out.println("ClientThread " + threadID + " running.");
            switch(threadID)
            {
                case 1 -> readInput();
                case 2 -> sendRequests();
                case 3 -> readUpdates();
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
    private void readInput()
    {
        Scanner in = new Scanner(System.in);
        System.out.println("You have 100 job requests available.");
        int maxRequests = 100;
        Packet newJob;
        while (maxRequests > 0)
        {
            System.out.println("Enter job type for the next job (A or B):");
            String input = in.nextLine();
            while (!(input.equalsIgnoreCase("A") || input.equalsIgnoreCase("B")))
            {
                System.out.println("Invalid type. Please re-enter A or B:");
                input = in.nextLine();
            }
            
            //creates new packet and increments jobID
            synchronized(jobID_LOCK) //critical section
            {
                if (input.equalsIgnoreCase("A"))
                    newJob = new Packet(CLIENT_NAME, jobID, Type.A);
                else
                    newJob = new Packet(CLIENT_NAME, jobID, Type.B);
                jobID++;
            }
            
            //adds new packet to list
            synchronized(newP_LOCK)
            {
                newPackets.add(newJob);
            }
            
            maxRequests--;
        }
    }
    
    @SuppressWarnings("empty-statement")
    private void sendRequests() throws IOException
    {
        Packet sending;
        while(true) //infinite loop
        {
            while (newPackets.isEmpty()); //spin
            //remove first packet from new packets
            synchronized(newP_LOCK)
            {
                sending = newPackets.get(0);
                newPackets.remove(0);
            }
            //send to master
            writer.writeObject(sending);
            
            //add to sent packets
            synchronized(sentP_LOCK)
            {
                sentPackets.add(sending);
            }
            System.out.println("Job request " + sending.getJobId() + " sent to master.");
        }
    }
    
    private void readUpdates() throws IOException, ClassNotFoundException
    {
        Packet complete;
        while(true) //infinite loop
        {
            //receive complete packet from master
            complete = (Packet)reader.readObject();
            
            //add to finished jobs list
            synchronized(finishedP_LOCK)
            {
                finishedPackets.add(complete);
            }
            
            //remove from sent packets list
            synchronized(sentP_LOCK)
            {
                sentPackets.remove(complete);
            }
            
            System.out.println("Job " + complete.getJobId() + " complete.");
        }
    }
    
}