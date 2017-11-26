import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class peerProcess {

	static List<ClientThread> ClientThreads = Collections.synchronizedList(new ArrayList<ClientThread>());
	static List<Integer> preferredNeighbors = new ArrayList<Integer>();
	ScheduledExecutorService scheduler =
		     Executors.newScheduledThreadPool(3);
	Integer port = 8000;
	static Integer Id;
	
	public static void main(String[] args) throws Exception 
	{
		
		System.out.println("The peer process " + args[0] + " is running.");
		
		peerProcess peer = new peerProcess();
		
		//Storing the peer Id of this peer
		Id = Integer.parseInt(args[0]);
		
		//Initializing a config instance to use across all client threads
		config cfg = new config();
		
		//This list maintains all peers that have already started
		List<RemotePeerInfo> connectedPeers = new ArrayList<RemotePeerInfo>();
		
		//This list maintains all peers that have yet to be started
		List<RemotePeerInfo> futurePeers = new ArrayList<RemotePeerInfo>();
		
		int has_file = 0;
		
		//Gather all the peer processes that have already started
		for(RemotePeerInfo rpi : cfg.peerInfoVector) 
		{
			
			if(Integer.parseInt(rpi.peerId) < Integer.parseInt(args[0])) 
			{
				connectedPeers.add(rpi);
			}
			
			else if(Integer.parseInt(rpi.peerId) == Integer.parseInt(args[0])) 
			{
				peer.port = Integer.parseInt(rpi.peerPort);
				if(rpi.peerHasFile == "1")
				{
					has_file = 1;
				}
				
			}
			
			else 
			{
				
				futurePeers.add(rpi);
				
			}
		}

		int size = cfg.noOfBytes;
		int pieces = cfg.noOfPieces;
		
		byte[] bitField = new byte[size];
		byte[] fileData = new byte[cfg.FileSize];
		//Check if the peer has the file and then set the all the bits to 1 if it has the file and 0 if doesn't
		if(has_file == 1)
		{
			
			try 
			{
				File file = new File("peer_"+ peerProcess.Id + "/" + cfg.FileName);			
				FileInputStream fdata = new FileInputStream(file);			
				fdata.read(fileData);
				fdata.close();
			
			} 
			catch(FileNotFoundException fnfe) 
			{
				fnfe.printStackTrace();
			}
			
			if (pieces % 8 == 0) 
            {
            	Arrays.fill(bitField, (byte) 255);
            } 
			
            else 
            {
                int last = (int) pieces % 8;
                Arrays.fill(bitField, (byte) 255); 
                bitField[bitField.length - 1] = 0; 
                	                
                while (last != 0) 
                {
                	
                	//setting the bits in the last byte of the bitfield
                	bitField[bitField.length - 1] |= (1 << (8 - last));
                    last--;
                }
                
            }
			
		}
		else
		{
			Arrays.fill(bitField, (byte)0);
		}
	
		//Connect to the peers that have already started
		for(RemotePeerInfo pInfo : connectedPeers) 
		{			
			try 
			{
				
				ClientThread client = new ClientThread(new Socket(pInfo.peerAddress, 
						Integer.parseInt(pInfo.peerPort)), true, pInfo.peerId,cfg, bitField, fileData);
				
				client.start();
				ClientThreads.add(client);
			} 			
			catch(Exception ex) 
			{
				ex.printStackTrace();
			}
			
		}
		
		if(connectedPeers.size() > cfg.NumberOfPreferredNeighbors)
			connectedPeers = connectedPeers.subList(0, cfg.NumberOfPreferredNeighbors);
		
		preferredNeighbors = connectedPeers.stream().map(cp -> Integer.parseInt(cp.peerId)).collect(Collectors.toList());	
		
		//Sockets listeners waiting for connection request from future peers in PeerInfo.cfg
		try 
		{			
			ServerSocket ss = new ServerSocket(peer.port);			
			for(RemotePeerInfo pInfo : futurePeers) 
			{
				Socket socket = ss.accept();			
				if(socket != null) 
				{
					ClientThread nc= new ClientThread(socket, false, pInfo.peerId, cfg, 
							bitField, fileData);
                	nc.start();
                	ClientThreads.add(nc);               	
                }
				
            }
			
        } 
		catch (Exception ex) 
		{			
			ex.printStackTrace();       
		}
		
		peer.updatePreferredNeighbors(cfg.UnchokingInterval);

    }
	
	public void updatePreferredNeighbors(int p) {
		
		Runnable managepc = () -> {
			for(Integer curr: preferredNeighbors) {
				ClientThreads.stream().filter(c -> (Integer.parseInt(c.peerID)==curr)).collect(Collectors.toList());
			}
			//preferredNeighbors.stream().map(c -> ClientThreads.)
		};
		//final ScheduledFuture<?> peerCon =
		scheduler.scheduleAtFixedRate(managepc, p, p, TimeUnit.SECONDS);
	}

}

