import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Master {

    @SuppressWarnings("empty-statement")
    public static void main(String[] args) throws IOException, InterruptedException {

        //input: "30121" //port number for clients
        //input: "30124" //port number for slaves

        if (args.length != 2) { 
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }

        int clientPort = Integer.parseInt(args[0]);
        int slavePort = Integer.parseInt(args[1]);

        try (ServerSocket serverC = new ServerSocket(clientPort);
                ServerSocket serverS = new ServerSocket(slavePort);
                
                Socket client1 = serverC.accept(); 
                Socket client2 = serverC.accept();
                
                Socket slave1 = serverS.accept();
                Socket slave2 = serverS.accept();
                 
                ObjectInputStream requestReader1 = new ObjectInputStream(client1.getInputStream());
                ObjectOutputStream updateWriter1 = new ObjectOutputStream(client1.getOutputStream());
                ObjectInputStream requestReader2 = new ObjectInputStream(client2.getInputStream());
                ObjectOutputStream updateWriter2 = new ObjectOutputStream(client2.getOutputStream());
                
                ObjectInputStream updateReader1 = new ObjectInputStream(slave1.getInputStream());
                ObjectOutputStream requestWriter1 = new ObjectOutputStream(slave1.getOutputStream());
                ObjectInputStream updateReader2 = new ObjectInputStream(slave2.getInputStream());
                ObjectOutputStream requestWriter2 = new ObjectOutputStream(slave2.getOutputStream());
            
                BufferedReader typeReader1 = new BufferedReader(new InputStreamReader(slave1.getInputStream())); //will read slave type
                BufferedReader typeReader2 = new BufferedReader(new InputStreamReader(slave2.getInputStream()));)
               {
                   System.out.println("Server: Client connections successful.");
                   System.out.println("Server: Slave connections successful.");
                   
                   ArrayList<Packet> waitingPackets = new ArrayList<Packet>(); //waiting to be assigned to a slave
                   Object wait_LOCK = new Object();
                   
                   ArrayList<Packet> slaveAPackets = new ArrayList<Packet>(); //jobs handed over to slave a
                   Object slaveA_LOCK = new Object();
                   
                   ArrayList<Packet> slaveBPackets = new ArrayList<Packet>(); //jobs handed over to slave b
                   Object slaveB_LOCK = new Object();
                   
                   ArrayList<Packet> finishedPackets = new ArrayList<Packet>(); //complete jobs
                   Object finished_LOCK = new Object();
                   
                   //create 2 threads per client
                   Thread mcReader1 = new MasterAndClient(requestReader1, waitingPackets, wait_LOCK); //read requests from client1
                   Thread mcWriter1 = new MasterAndClient(updateWriter1, finishedPackets, finished_LOCK); //sends updates to client1
                   
                   Thread mcReader2 = new MasterAndClient(requestReader2, waitingPackets, wait_LOCK); //read requests from client2
                   Thread mcWriter2 = new MasterAndClient(updateWriter2, finishedPackets, finished_LOCK); //sends updates to client2
                   
                   //create 2 threads per slave
                   Thread msWriter1 = new MasterAndSlave(requestWriter1, typeReader1, slaveAPackets, slaveA_LOCK, slaveBPackets, slaveB_LOCK);
                   Thread msReader1 = new MasterAndSlave(updateReader1, typeReader1, slaveAPackets, slaveA_LOCK, slaveBPackets, slaveB_LOCK, finishedPackets, finished_LOCK);
                   
                   Thread msWriter2 = new MasterAndSlave(requestWriter2, typeReader2, slaveAPackets, slaveA_LOCK, slaveBPackets, slaveB_LOCK);
                   Thread msReader2 = new MasterAndSlave(updateReader2, typeReader2, slaveAPackets, slaveA_LOCK, slaveBPackets, slaveB_LOCK, finishedPackets, finished_LOCK);
                   
                   //start all threads
                   mcReader1.start();
                   mcWriter1.start();
                   mcReader2.start();
                   mcWriter2.start();
                   msWriter1.start();
                   msReader1.start();
                   msWriter2.start();
                   msReader2.start();
                   
                   //perform load balancing algorithm on all incoming jobs
                   Packet newPacket;
                   Type jobType;
                   while(true) //infinite loop
                   {
                       while(waitingPackets.isEmpty()); //spin
                       newPacket = (Packet)waitingPackets.get(0);
                       jobType = newPacket.getJobType();
                       int slaveADelay = 0;
                       int slaveBDelay = 0;
                       
                       //calculate slave A's Delay
                       synchronized(slaveA_LOCK)
                       {
                           for (Packet p : slaveAPackets)
                           {
                               if (p.getJobType().equals(Type.A))
                                   slaveADelay += 2;
                               else
                                   slaveADelay += 10;
                           }
                       }
                       //calculate slave B's delay
                       synchronized(slaveB_LOCK)
                       {
                           for (Packet p : slaveBPackets)
                           {
                               if (p.getJobType().equals(Type.B))
                                   slaveBDelay += 2;
                               else
                                   slaveBDelay += 10;
                           }
                       }
                       
                       //find best slave to send to
                       if (jobType.equals(Type.A))
                       {
                           if (slaveADelay - slaveBDelay > 8) //if slave A's delay is 9+ seconds longer than slave B's delay, send to B
                           {
                                //send to slave B
                               synchronized(slaveB_LOCK)
                               {
                                   slaveBPackets.add(newPacket);
                               }
                           }
                           else
                           {
                               //send to slave A
                               synchronized(slaveA_LOCK)
                               {
                                   slaveAPackets.add(newPacket);
                               }
                           } 
                       }
                       else //if this job is type B
                       {
                           if (slaveBDelay - slaveADelay > 8) //if slave B's delay is 9+ seconds longer than slave A's delay, send to A
                           {
                                //send to slave A
                               synchronized(slaveA_LOCK)
                               {
                                   slaveAPackets.add(newPacket);
                               }
                           }
                           else
                           {
                               //send to slave B
                               synchronized(slaveB_LOCK)
                               {
                                   slaveBPackets.add(newPacket);
                               }
                           } 
                       }
                           
                       //remove from waiting list
                       synchronized(wait_LOCK)
                       {
                            waitingPackets.remove(newPacket);
                       }
                   }

        }
        catch (IOException e) {
            System.out.println(
                    "Exception caught when trying to listen on port or listening for a connection");
            System.out.println(e.getMessage());
        }

    }
}