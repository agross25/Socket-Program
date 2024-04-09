import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class MasterAndClient extends Thread {
    
    private int id;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    ArrayList<Packet> packets;
    Object packet_LOCK;
    
    //constructor 1 - will get job requests from client
    public MasterAndClient(ObjectInputStream r, ArrayList<Packet> waiting, Object waitLock)
    {
        id = 1;
        reader = r;
        packets = waiting;
        packet_LOCK = waitLock;
    }
    
    //constructor 2 - will send updates to client
    public MasterAndClient(ObjectOutputStream w, ArrayList<Packet> finished, Object finishedLock)
    {
        id = 2;
        writer = w;
        packets = finished;
        packet_LOCK = finishedLock;
    }
    
    @Override
   public void run() throws RuntimeException {
       System.out.println("Master/Client thread " + id + " running.");
       try {
           if (id == 1)
               readRequests();
           else //id = 2
               writeUpdates();
       }
       catch (Exception e)
       {
           System.err.println("Error occured.");
       }
   }


   private void readRequests() throws IOException, ClassNotFoundException {
       while (true) //infinite loop
       {
           Packet newPacket = (Packet)reader.readObject();
           //add new request to waiting packets list
           synchronized (packet_LOCK)
           {
               packets.add(newPacket);
           }
           System.out.println("Job request " + newPacket.getJobType() + newPacket.getJobId() + " received.");
       }
   }


    @SuppressWarnings("empty-statement")
   private void writeUpdates() throws IOException {
       Packet finishedPacket;
       while (true) //infinite loop
       {
           while(packets.isEmpty()); //spin
           //notify client of finished job and remove from list
           synchronized(packet_LOCK)
           {
               finishedPacket = packets.get(0);
               packets.remove(finishedPacket);
           }
           writer.writeObject(finishedPacket);
           System.out.println("Message sent to client: Job " + finishedPacket.getJobType() + finishedPacket.getJobId() + " complete.");
       }
   }
}
