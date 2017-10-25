import java.io.*;
import java.net.*;
import java.util.*;

public class peerProcess {
	
	public static List<ClientThread> ClientThreads = Collections.synchronizedList(new ArrayList<ClientThread>());
	
	public Vector<RemotePeerInfo> peerInfoVector;
	Integer NumberOfPreferredNeighbors;
	Integer UnchokingInterval;
	Integer OptimisticUnchokingInterval;
	String FileName;
	Integer FileSize;
	Integer PieceSize;
	Integer port = 8000;
	static Integer Id;
	
	public void readConfiguration()
	{
		String st;
		int i1;
		peerInfoVector = new Vector<RemotePeerInfo>();
		
		try {
			BufferedReader in = new BufferedReader(new FileReader("Common.cfg"));
			while((st = in.readLine()) != null) {
				String[] tokens = st.split("\\s+");
				switch(tokens[0]) {
				case "NumberOfPreferredNeighbors":
					NumberOfPreferredNeighbors = Integer.parseInt(tokens[1]);
					break;
				case "UnchokingInterval":
					UnchokingInterval = Integer.parseInt(tokens[1]);
					break;
				case "OptimisticUnchokingInterval":
					OptimisticUnchokingInterval = Integer.parseInt(tokens[1]);
					break;
				case "FileName":
					FileName = tokens[1];
					break;
				case "FileSize":
					FileSize = Integer.parseInt(tokens[1]);
					break;
				case "PieceSize":
					PieceSize = Integer.parseInt(tokens[1]);
					break;
				}
			}
			
			in = new BufferedReader(new FileReader("PeerInfo.cfg"));
			while((st = in.readLine()) != null) {
				String[] tokens = st.split("\\s+");
		    	peerInfoVector.addElement(new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));
			}
			
			in.close();
		}
		catch (Exception ex) {
			System.out.println(ex.toString());
		}
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println("The peer process " + args[0] + " is running.");
		peerProcess peer = new peerProcess();
		peer.readConfiguration();
		
		Id = Integer.parseInt(args[0]);
		List<RemotePeerInfo> connectedPeers = new ArrayList<RemotePeerInfo>();
		List<RemotePeerInfo> futurePeers = new ArrayList<RemotePeerInfo>();
		//Gather all the peer processes that have already started
		for(RemotePeerInfo rpi : peer.peerInfoVector) {
			if(Integer.parseInt(rpi.peerId) < Integer.parseInt(args[0]))
				connectedPeers.add(rpi);
			else if(Integer.parseInt(rpi.peerId) == Integer.parseInt(args[0]))
				peer.port = Integer.parseInt(rpi.peerPort);
			else
				futurePeers.add(rpi);
			
		}
		
		//Connect to the peers that have already started
		for(RemotePeerInfo pInfo : connectedPeers) {
			try {
				ClientThread client = new ClientThread(new Socket(pInfo.peerAddress, Integer.parseInt(pInfo.peerPort)), true, pInfo.peerId);
				client.start();
				ClientThreads.add(client);
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		//Sockets listeners waiting for connection request from future peers in PeerInfo.cfg
		try {
			ServerSocket ss = new ServerSocket(peer.port);
            for(RemotePeerInfo pInfo : futurePeers) {
            	Socket socket = ss.accept();
                if(socket != null) {
                	ClientThread nc= new ClientThread(socket, false, "-1");
                    nc.start();
                    ClientThreads.add(nc);
                }
            }
        } catch (Exception ex) {
			ex.printStackTrace();
        }
 
    }
	
}

