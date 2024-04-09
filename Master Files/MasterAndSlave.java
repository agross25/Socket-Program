import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MasterAndSlave extends Thread {
    
    private int id;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    private ArrayList<Packet> slaveAPackets;
    private final Object slaveA_LOCK;
    private ArrayList<Packet> slaveBPackets;
    private final Object slaveB_LOCK;
    private ArrayList<Packet> finishedPackets;
    private final Object finished_LOCK;
    private BufferedReader message;
    private Type slaveType;
    
    //constructor 1 - will send jobs to slaves
    public MasterAndSlave(ObjectOutputStream w, BufferedReader m, ArrayList<Packet> packetsA, Object lockA, ArrayList<Packet> packetsB, Object lockB) //writes requests to slaves
    {
        id = 1;
        writer = w;
        message = m;
        slaveAPackets = packetsA;
        slaveA_LOCK = lockA;
        slaveBPackets = packetsB;
        slaveB_LOCK = lockB;
        finished_LOCK = new Object(); //will not be used
    }
    
    //constructor 2 - will receive updates from slaves
    public MasterAndSlave(ObjectInputStream r, BufferedReader m, ArrayList<Packet> jobsA, Object jobLockA, ArrayList<Packet> jobsB, Object jobLockB, ArrayList<Packet> finished, Object finishedLock) //gets updates from slaves
    {
        id = 2;
        reader = r;
        message = m;
        slaveAPackets = jobsA;
        slaveA_LOCK = jobLockA;
        slaveBPackets = jobsB;
        slaveB_LOCK = jobLockB;
        finishedPackets = finished;
        finished_LOCK = finishedLock;
    }
    
     @Override
    public void run() {
        try {
            System.out.println("MasterAndSlave Thread " + id + " running.");
            switch (id) {
                case 1 -> sendRequestsToSlave();
                case 2 -> receiveUpdatesFromSlave();
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(MasterAndSlave.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @SuppressWarnings("empty-statement")
    private void sendRequestsToSlave() throws IOException
    {   
        //find out slave type from reader
        String s = message.readLine();
        if (s.equals("A"))
            slaveType = Type.A;
        else
            slaveType = Type.B;
        Packet sending;
        while(true) //infinite loop
        {
            if (slaveType.equals(Type.A))
            {
                while(slaveAPackets.isEmpty()); //spin
                synchronized(slaveA_LOCK)
                {
                    sending = (Packet)slaveAPackets.get(0);
                    slaveAPackets.remove(sending);
                }
                writer.writeObject(sending);
                System.out.println("Sending job "+ sending.getJobId() + " to slave A.");
            }
            else
            {
                while(slaveBPackets.isEmpty()); //spin
                synchronized(slaveB_LOCK)
                {
                    sending = (Packet)slaveBPackets.get(0);
                    slaveBPackets.remove(sending);
                }
                writer.writeObject(sending);
                System.out.println("Sending job "+ sending.getJobId() + " to slave B.");
            }
        }
    }
    
    private void receiveUpdatesFromSlave() throws IOException, ClassNotFoundException
    {
        //find out slave type from reader
        String s = message.readLine();
        if (s.equals("A"))
            slaveType = Type.A;
        else
            slaveType = Type.B;        
        while(true) //infinite loop
        {
            Packet completedJob = (Packet)reader.readObject();
            System.out.println("MasterAndSlave Thread " + id + ": Received completed job " + completedJob.getJobId() + " from Slave");
            synchronized (finished_LOCK) {
                finishedPackets.add(completedJob);
            }
            
            if (slaveType.equals(Type.A))
            {
                //remove job from appropriate list
                synchronized(slaveA_LOCK)
                {
                    slaveAPackets.remove(completedJob);
                }
            }
            else
            {
                synchronized(slaveB_LOCK)
                {
                    slaveBPackets.remove(completedJob);
                }
            }
        }

        }
    }
