import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class peerProcess {

	static List<ClientThread> ClientThreads = Collections.synchronizedList(new ArrayList<ClientThread>());
	static ClientThread optUnchokedNeighbor;
	static byte[] bitField, fileData, completeFile;
	static AtomicBoolean[] requested;
	static Logger logger;
	
	ScheduledExecutorService scheduler =
		     Executors.newScheduledThreadPool(3);
	
	Integer port = 8000;
	static Integer Id;
	static ServerSocket ss;
	
	public static void main(String[] args) throws Exception 
	{
		
		peerProcess peer = new peerProcess();
		
		//Storing the peer Id of this peer
		Id = Integer.parseInt(args[0]);
		
		//Initializing a config instance to use across all client threads
		config cfg = new config();
		
		logger = PeerLogger.getLogger(Id);
		
		//This list maintains all peers that have already started
		List<RemotePeerInfo> connectedPeers = new ArrayList<RemotePeerInfo>();
		
		//This list maintains all peers that have yet to be started
		List<RemotePeerInfo> futurePeers = new ArrayList<RemotePeerInfo>();
		
		int has_file = 0;
		
		//Gather all the peer processes that have already started
		for(RemotePeerInfo rpi : cfg.peerInfoVector) 
		{
			
			if(Integer.parseInt(rpi.peerId) < Integer.parseInt(args[0])) 
				connectedPeers.add(rpi);
			
			else if(Integer.parseInt(rpi.peerId) == Integer.parseInt(args[0])) 
			{
				
				peer.port = Integer.parseInt(rpi.peerPort);
				
				if(rpi.peerHasFile.equals("1"))
					has_file = 1;
				
			}
			
			else
				futurePeers.add(rpi);
			
		}
		

		int size = cfg.noOfBytes;
		int pieces = cfg.noOfPieces;
		
		bitField = new byte[size];
		
		//Array to monitor if a piece has been already requested
		requested = new AtomicBoolean[pieces];
		Arrays.fill(requested, new AtomicBoolean(false));
		
		
		fileData = new byte[cfg.FileSize];
		completeFile = new byte[size];
		
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
            	Arrays.fill(completeFile, (byte)255);
            } 
			
            else 
            {
                int last = (int) pieces % 8;
                
                Arrays.fill(bitField, (byte) 255);
                Arrays.fill(completeFile, (byte)255);
                
                bitField[bitField.length - 1] = 0;
                completeFile[bitField.length - 1] = 0;
                	                
                while (last != 0) 
                {
                	
                	//setting the bits in the last byte of the bitfield
                	bitField[bitField.length - 1] |= (1 << (8 - last));
                	completeFile[bitField.length - 1] |= (1 << (8 - last));
                    last--;
                }
                
            }
			
		}
		else
		{
			if (pieces % 8 == 0) 
               	Arrays.fill(completeFile, (byte)255);
            			
            else 
            {
                int last = (int) pieces % 8;
                
                Arrays.fill(completeFile, (byte)255);
                completeFile[bitField.length - 1] = 0;
                	                
                while (last != 0) 
                {
                	completeFile[bitField.length - 1] |= (1 << (8 - last));
                    last--;
                }
                
            }
		}
		
		//Connect to the peers that have already started
		for(RemotePeerInfo pInfo : connectedPeers) 
		{			
			try 
			{
				ClientThread client = new ClientThread(new Socket(pInfo.peerAddress, 
						Integer.parseInt(pInfo.peerPort)), true, pInfo.peerId,cfg);
				
				client.start();
				ClientThreads.add(client);
				
				logger.info("Peer " + Id + " makes a connection to Peer " + pInfo.peerId + ".");
			} 			
			catch(Exception ex) 
			{
				ex.printStackTrace();
				logger.info(ex.toString());
			}
			
		}
		
		
		ss = new ServerSocket(peer.port);
		
		
		//Sockets listeners waiting for connection request from future peers in PeerInfo.cfg
		try 
		{
			
			for(RemotePeerInfo pInfo : futurePeers) 
			{
				logger.info("Listening at port: " + peer.port);
				
				Runnable listener = () -> {
					
					try {
						ClientThread nc = new ClientThread(ss.accept(), false, pInfo.peerId, cfg);
						logger.info("Peer " + Id + " is connected from Peer " + pInfo.peerId + ".");
						
						ClientThreads.add(nc);
						nc.start();
					} catch (IOException e) {
						logger.info(e.getMessage());
					}
					
					
				};
				
				new Thread(listener).start();
				
            }
			
			
        } 
		catch (Exception ex) 
		{			
			ex.printStackTrace();
		}
		
		/*List<ClientThread> cts = ClientThreads; 
		if(cts.size() > cfg.NumberOfPreferredNeighbors)
			cts = ClientThreads.subList(0, cfg.NumberOfPreferredNeighbors);
		
		//Initializing the preferred neighbors
		cts.stream().map(ct -> ct.choked = false);
		*/
		//Initializing the optimistically unchoked neighbor
		List<ClientThread> chokedInterestedNeighbors = ClientThreads.stream().filter(ct -> ct.choked && ct.clientInterested).collect(Collectors.toList());

		if(chokedInterestedNeighbors.isEmpty())
			optUnchokedNeighbor = null;
		else
			optUnchokedNeighbor = chokedInterestedNeighbors.get(new Random().nextInt(chokedInterestedNeighbors.size()));
		
		
		peer.updatePreferredNeighbors(cfg.NumberOfPreferredNeighbors, cfg.UnchokingInterval);
		peer.assignOptimisticallyUnchokedNeighbor(cfg.OptimisticUnchokingInterval);
		
		peer.monitorFileSharingStatus();
    }
	
	public void updatePreferredNeighbors(int k, int p) {
		
		Runnable updatepn = () -> {
			
			try {
				//Order the peers by download rate
				
				Collections.sort(ClientThreads, (ct1, ct2) -> ct2.downloadRate.compareTo(ct1.downloadRate));
			
				int neighbors = 0;
				List<String> neighborList = new ArrayList<String>();
				
				//Iterate the client threads and update choke/unchoke status of each thread using download rates and interested status 
				for(int i=0;i<ClientThreads.size();i++) {
				
					ClientThread ct = ClientThreads.get(i);
				
					if(ct.clientInterested)
					{
					
						//First k interested peers are unchoked
						if(neighbors<k) 
						{	
							if(ct.choked) 
							{
								ct.choked = false;
								ct.sendUnchokeMessage();
							}
							neighborList.add(ct.peerID);
						} 
						//Remaining peers are choked
						else 
						{
						
							if(!ct.choked && ct != optUnchokedNeighbor) 
							{
								ct.choked = true;
								ct.sendChokeMessage();
							}
						
						}
					
						neighbors++;
					}
				}
				logger.info("Peer " + Id + " has the preferred neighbors " + String.join(",", neighborList) + ".");
			} catch(Exception e) {
				e.printStackTrace();
				logger.info(e.toString());
			}
			
				
			
		};
		scheduler.scheduleAtFixedRate(updatepn, p, p, TimeUnit.SECONDS);
	}
	
	public void assignOptimisticallyUnchokedNeighbor(int m) {
		
		Runnable assignOUN = () -> {
			
			try {
				List<ClientThread> chokedInterestedNeighbors = ClientThreads.stream().filter(ct -> ct.choked && ct.clientInterested).collect(Collectors.toList());
			
			
				if(chokedInterestedNeighbors.isEmpty()) 
				{
					
					if(optUnchokedNeighbor!=null) {
					
						if(!optUnchokedNeighbor.choked)	{
						
							optUnchokedNeighbor.choked = true;
							optUnchokedNeighbor.sendChokeMessage();
						
						}
					
						optUnchokedNeighbor = null;
					}
				} 
			
				else 
				{
				
					if(optUnchokedNeighbor!=null)	
					{
						optUnchokedNeighbor.choked = true;
						optUnchokedNeighbor.sendChokeMessage();
					}
				
					optUnchokedNeighbor = chokedInterestedNeighbors.get(ThreadLocalRandom.current().nextInt(ClientThreads.size()));
				
					optUnchokedNeighbor.choked = false;
					optUnchokedNeighbor.sendUnchokeMessage();
				
				}
				
				if(optUnchokedNeighbor!=null)
					logger.info("Peer " + Id + " has the optimistically unchoked neighbor " + optUnchokedNeighbor.peerID + ".");
			} catch(Exception e) {
				e.printStackTrace();
				System.out.println("Error: " + e.toString());
			}
		};
		
		scheduler.scheduleAtFixedRate(assignOUN, m, m, TimeUnit.SECONDS);
	}
	
	
	public void monitorFileSharingStatus() {
		Runnable checkPeerFile = () -> {
			
			boolean complete = true;
			
			for(ClientThread ct : ClientThreads) {
				//Check if each peer has received the complete file or not
				if(!Arrays.equals(ct.peerBitField, completeFile)) {
					logger.info("Peer " + ct.peerID + " yet to receive the full file.");
					complete = false;
					break;
				}
			 
			}
			
			logger.info("Complete file: " + complete);
			if(complete && Arrays.equals(bitField, completeFile)) {
				
				//Setting the stopping condition to true for all client threads
				for(ClientThread ct : ClientThreads)
					ct.setStoppingCondition(true);
			 	
				//Ending the scheduled processes
				scheduler.shutdown();
				try {
					//Closing the server socket connection
					ss.close();
				} catch (IOException e) {
					e.printStackTrace();
					logger.info("Error while closing server socket");
				}
				logger.info("Ending the peer process: " + Id);
				
				System.exit(0);
			}
			
		};
		
		scheduler.scheduleAtFixedRate(checkPeerFile, 5, 5, TimeUnit.SECONDS);
	}

}

