import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SlaveThread extends Thread {
    
    private final int threadID;
    private final Type slaveType;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    private ArrayList<Packet> packets;
    private final Object packet_LOCK;
    private ArrayList<Packet> finishedPackets;
    private final Object finished_LOCK;
    private PrintWriter pw;
     
    //constructor 1 - will receive job requests from master
    public SlaveThread(Type type, PrintWriter p, ObjectInputStream r, ArrayList<Packet> waiting, Object waitLock)
    {
        slaveType = type;
        threadID = 1;
        reader = r;
        packets = waiting;
        packet_LOCK = waitLock;
        finished_LOCK = new Object(); //will not be used
        pw = p;
    }
    
    //constructor 2 - will send updates to master
    public SlaveThread(Type type, ObjectOutputStream w, ArrayList<Packet> finished, Object finishedLock, PrintWriter p)
    {
        slaveType = type;
        threadID = 2;
        writer = w;
        packets = finished;
        packet_LOCK = finishedLock;
        finished_LOCK = new Object(); //will not be used
        pw = p;
    }
    
    //constructor 3 - will work on incoming jobs
    public SlaveThread(Type type, ArrayList<Packet> waiting, Object waitLock, ArrayList<Packet> finished, Object finishedLock)
    {
        slaveType = type;
        threadID = 3;
        packets = waiting;
        packet_LOCK = waitLock;
        finishedPackets = finished;
        finished_LOCK = finishedLock;
    }
    
    @Override
    public void run()
    {
        try {
            System.out.println("SlaveThread " + threadID + " running.");
            switch(threadID)
            {
                case 1 -> readRequests();
                case 2 -> sendUpdates();
                case 3 -> doJobs();
            }
        } catch (IOException | ClassNotFoundException | InterruptedException ex) {
            Logger.getLogger(SlaveThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void readRequests() throws IOException, ClassNotFoundException
    {
        //tell master what type of slave this is
        if (slaveType == Type.A)
            pw.write("A");
        else
            pw.write("B");
        Packet newPacket;
        while(true) //infinite loop
        {
            newPacket = (Packet)reader.readObject(); //read new request from master
            synchronized(packet_LOCK)
            {
                packets.add(newPacket); //add new request to list
            }
        }
    }
    
    @SuppressWarnings("empty-statement")
    private void sendUpdates() throws IOException
    {
        //tell master what type of slave this is
        if (slaveType == Type.A)
            pw.write("A");
        else
            pw.write("B");
        Packet complete;
        while(true) //infinite loop
        {
            while(packets.isEmpty()); //spin
            synchronized(packet_LOCK)
            {
                complete = packets.get(0);
                packets.remove(0);
            }
            //send to master
            writer.writeObject(complete);
            System.out.println("Message sent to Master: Job " + complete.getJobId() + " complete.");
        }
    }
    
    @SuppressWarnings("empty-statement")
    private void doJobs() throws InterruptedException
    {
        Packet currentJob;
        while(true) //infinite loop
        {
            while(packets.isEmpty()); //spin
            //claim a job to work on
            synchronized(packet_LOCK)
            {
                currentJob = packets.get(0);
                packets.remove(0);
            }
            //sleep for appropriate amount of time
            if (currentJob.getJobType().equals(slaveType))
                Thread.sleep(2000); //sleep for 2 seconds if we are the same type
            else
                Thread.sleep(10000); //sleep for 10 seconds if we are not the same type
            System.out.println("Job " + currentJob.getJobId() + " completed.");
            synchronized(finished_LOCK)
            {
                finishedPackets.add(currentJob);
            }
        }
    }
}
