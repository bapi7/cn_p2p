import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class ClientThread extends Thread
{
	
	Socket requestSocket;           //socket connect to the server
	BufferedOutputStream out;         //stream write to the socket
	BufferedInputStream in;          //stream read from the socket
	
	boolean isClient;
	String peerID;
	byte[] peerBitField;
	config cfg;
	Runnable intThread;
	TCPMsgUtil util;
	boolean clientInterested = true;
	Float downloadRate = 1.0f;
	Long avgPieceDownloadTime;
	boolean choked = true;
	List<Integer> preferredNeighbors = new ArrayList<Integer>();
	boolean stoppingCondition = false;

	public ClientThread(Socket s, boolean isClient, String peerID, config cfg) {
		
		this.cfg = cfg;
		this.requestSocket = s;
		this.isClient = isClient;
		util = new TCPMsgUtil();
		
		try {
			
			//initialize inputStream and outputStream
			out = new BufferedOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new BufferedInputStream(requestSocket.getInputStream());

	        if(isClient)
	        {
	        	//sending handshake message to the peer
	        	this.peerID = peerID;
                util.sendMessage(out, TCPMsgUtil.constructHandshakeMessage(String.valueOf(peerProcess.Id)));
                util.receiveHandshakeMessage(in);
                
	        } 	        
	        else 
	        {
                Integer rcv = util.receiveHandshakeMessage(in);
	        	this.peerID = rcv.toString();
	        	util.sendMessage(out, TCPMsgUtil.constructHandshakeMessage(String.valueOf(peerProcess.Id)));
	        	
	        }
	        
	        
	        util.sendBitFieldMessage(out, peerProcess.bitField);	                
		        
	        peerBitField = util.receiveBitFieldMessage(in);

		    if(util.isInterested(peerProcess.bitField, peerBitField))
		    	util.sendInterestedMessage(out);
		    else
		    	util.sendNotInterestedMessage(out);
	        
		} 		
		catch(IOException ioe) 
		{	
			ioe.printStackTrace();
			peerProcess.LOGGER.info("IOException: " + ioe.toString());
		}
		
	}

	public void run() 
	{		
		try 
		{
			long req_time = 0l, total_time = 0l;
			int piecesRcvd = 0;
			byte[] msgLength, msgType, payload;
			msgType = new byte[1];
			msgLength = new byte[4];
			int requestedIndex = 0;
			
		    while(!stoppingCondition) 
			{
		    	
		    	in.read(msgLength);
		    	in.read(msgType);
		    	
		    	TCPMsgUtil.MessageType inpMsg = TCPMsgUtil.MessageType.fromValue(msgType[0]);
		    	
		    	switch(inpMsg) {
		    		case CHOKE:
		    			peerProcess.LOGGER.info("Peer " + peerProcess.Id + " is choked by " + peerID + ".");
		    			byte indByte = peerProcess.bitField[requestedIndex/8];
		    			
		    			if(((1 << (7 - (requestedIndex%8))) & indByte) == 0) {
		    				peerProcess.requested[requestedIndex].set(false);
		    			}
		    			
		    			break;
		    			
		    		case UNCHOKE:
		    			peerProcess.LOGGER.info("Peer " + peerProcess.Id + " is unchoked by " + peerID + ".");
		    			
		    			//Fetch the piece index to request
		    			int pIndex = util.getPieceIndexToRequest(peerProcess.bitField, peerBitField, peerProcess.requested);
		    			
		    			if(pIndex>=0) {
		    				requestedIndex = pIndex;
		    				peerProcess.requested[pIndex].set(true);
		    				util.sendRequestMessage(out, pIndex);
		    				req_time = System.nanoTime();
		    			}
		    			
		    			break;
		    			
		    		case INTERESTED:
		    			peerProcess.LOGGER.info("Peer " + peerProcess.Id + " received the 'interested' message from " + peerID + ".");
		    			if(!clientInterested)
		    				clientInterested = true;
		    			break;
		    			
		    		case NOTINTERESTED:
		    			peerProcess.LOGGER.info("Peer " + peerProcess.Id + " received the 'not interested' message from " + peerID + ".");
		    			clientInterested = false;
		    			choked = true;
		    			break;
		    			
		    		case HAVE:
		    			
		    			//Have message contains 4 byte piece index payload
		    			byte[] pieceIndexbytes = util.readCompleteMsg(in, 4);
		    			int pieceIndex = ClientHelper.bytearray_to_int(pieceIndexbytes);
		    			
		    			peerProcess.LOGGER.info("Peer " + peerProcess.Id + " received the 'have' message from " + peerID + " for the piece " + pieceIndex + ".");
		    			
		    			//Update the peer's bitfield
		    			peerBitField[pieceIndex/8] |=  (1 << (7 - (pieceIndex%8)));
		    			
		    			byte indexByte = peerProcess.bitField[pieceIndex/8];
		    			//Checking if it has the piece, if not send interested message
		    			if(((1 << (7 - (pieceIndex%8))) & indexByte) == 0)
		    				util.sendInterestedMessage(out);
		    			
		    			else
		    				util.sendNotInterestedMessage(out);
		    			
		    			break;
		    			
		    		case REQUEST:
		    			payload = util.readCompleteMsg(in, 4);
		    			
		    			int pieceInd = ClientHelper.bytearray_to_int(payload);
		    			
		    			peerProcess.LOGGER.info("Peer " + peerProcess.Id + " received the 'request' message from " + peerID + " for the piece " + pieceInd + ".");
		    			int startInd = pieceInd*cfg.PieceSize;
		    			
		    			try {
		    				byte[] data;
		    			
		    				if((cfg.FileSize-startInd) < cfg.PieceSize) {
		    					data = Arrays.copyOfRange(peerProcess.fileData, startInd, cfg.FileSize);
		    				} 
		    			
		    				else {
		    					data = Arrays.copyOfRange(peerProcess.fileData, startInd, startInd+cfg.PieceSize);
		    				}
		    			 
		    				if(!choked)
		    					util.sendPieceMessage(out, pieceInd, data);
		    			} catch(Exception e) {
		    				e.printStackTrace();
		    				System.out.println(e.toString());
		    			}
		    			break;
		    			
		    		case PIECE:
		    			byte[] pInd = new byte[4];
		    			in.read(pInd);
		    			
		    			int pcInd = ClientHelper.bytearray_to_int(pInd);
		    			
		    			int mLen = ClientHelper.bytearray_to_int(msgLength);
		    			
		    			byte[] pdata = util.readCompleteMsg(in, mLen-5);
		    			
		    			//Update bitField after receiving the piece
		    			peerProcess.bitField[pcInd/8] |= 1 << (7-(pcInd%8));
		    			
		    			//Store the received piece data at the appropriate location
		    			int start = pcInd*cfg.PieceSize;
		    			for(int i=0;i<pdata.length;i++) {
		    				peerProcess.fileData[start+i] = pdata[i];
		    			}
		    			
		    			piecesRcvd++;
		    			peerProcess.LOGGER.info("Peer " + peerProcess.Id + " has downloaded the piece " + pcInd + " from " + 
		    					peerID + ". Now the number of pieces it has is " + piecesRcvd + ".");
		    			
		    			
		    			//Update the download rate from this peer
		    			total_time += System.nanoTime() - req_time;
		    			downloadRate = (float) ((piecesRcvd*cfg.PieceSize)/total_time);
		    			
		    			//Send have messages to all the peers after receiving this piece
		    			for(ClientThread ct : peerProcess.ClientThreads) {
		    				ct.sendHaveMessage(pInd);
		    			}
		    			
		    			//Check if this peer has any useful pieces and send another request message
		    			pcInd = util.getPieceIndexToRequest(peerProcess.bitField, peerBitField, peerProcess.requested);
		    			
		    			if(pcInd>=0) {
		    				requestedIndex = pcInd;
		    				peerProcess.requested[pcInd].set(true);
		    				util.sendRequestMessage(out, pcInd);
		    				req_time = System.currentTimeMillis();
		    			} else {
		    				util.sendNotInterestedMessage(out);
		    				
		    				//Check if the peer has the complete file
		    				if(Arrays.equals(peerProcess.bitField, peerProcess.completeFile)) {
		    					
		    					//Send not interested messages to all the peers
				    			for(ClientThread ct : peerProcess.ClientThreads) {
				    				ct.util.sendNotInterestedMessage(out);
				    			}
				    			
				    			new File("peer_" + peerProcess.Id).mkdir();
				    			File file = new File("peer_" + peerProcess.Id + "/" + cfg.FileName);
				    			
				    			FileOutputStream fdata = new FileOutputStream(file);			
								fdata.write(peerProcess.fileData);
								fdata.close();
								peerProcess.LOGGER.info("Peer " + peerProcess.Id + " has downloaded the complete file.");
		    				}
		    			}
		    			
		    			break;
		    			
		    		default:
		    			break;
		    	}
			
			}
		} 
		
		catch(IOException ioException)
		{
				ioException.printStackTrace();				
		} 
		
		finally
		{
			//Close connections
			try
			{				
				in.close();
				out.close();
				requestSocket.close();				
			} 
			catch(IOException ioException)
			{				
				ioException.printStackTrace();				
			}
		}
	}
	
	//Method to send Unchoke message
	public void sendUnchokeMessage() {
	
		//converting msg length to byte array
		byte[] len = ClientHelper.int_to_bytearray(1);
			
		//appending message type to msg length, no payload for unchoke message
		byte[] res = ClientHelper.append_byte_to_bytearray(len, TCPMsgUtil.MessageType.UNCHOKE.val);
		 
		util.sendMessage(out, res);
	}
	
	//Method to send choke message
	public void sendChokeMessage() {
		
		//converting msg length to byte array
		byte[] len = ClientHelper.int_to_bytearray(1);
		
		//appending message type to msg length, no payload for choke message
		byte[] res = ClientHelper.append_byte_to_bytearray(len, TCPMsgUtil.MessageType.CHOKE.val);
		 
		util.sendMessage(out, res);
	}
	
	//Method to send have message
	public void sendHaveMessage(byte[] pieceIndex) {
		
		//converting msg length to byte array
		byte[] len = ClientHelper.int_to_bytearray(5);
				
		//appending message type to msg length and then the payload for have message
		byte[] res = ClientHelper.appendByteArray(ClientHelper.append_byte_to_bytearray(len, TCPMsgUtil.MessageType.HAVE.val), pieceIndex);
				 
		util.sendMessage(out, res);
	}
	
}