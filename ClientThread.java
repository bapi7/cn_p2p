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
	byte[] fileData;
	boolean clientInterested;
	Float downloadRate;
	Long avgPieceDownloadTime;
	boolean choked;
	List<Integer> preferredNeighbors = new ArrayList<Integer>();
	boolean stoppingCondition = false;

	public ClientThread(Socket s, boolean isClient, String peerID, config cfg, byte[] fileData) 
	{
		
		this.cfg = cfg;
		this.requestSocket = s;
		this.isClient = isClient;
		util = new TCPMsgUtil();
		
		try
		{
			
			this.fileData = fileData;
			
			//initialize inputStream and outputStream
			out = new BufferedOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new BufferedInputStream(requestSocket.getInputStream());

	        if(isClient)
	        {
	        	//sending handshake message to the peer
	        	this.peerID = peerID;
                util.sendMessage(out, TCPMsgUtil.constructHandshakeMessage(peerID));
                util.receiveHandshakeMessage(in, Integer.parseInt(peerID));
                
	        } 	        
	        else 
	        {
                Integer rcv = util.receiveHandshakeMessage(in, Integer.parseInt(peerID));
	        	this.peerID = rcv.toString();
	        	util.sendMessage(out, TCPMsgUtil.constructHandshakeMessage(peerID));
	        	
	        }
	        
	        peerProcess.lock.lock();
	        try{
	        	util.sendBitFieldMessage(out, peerProcess.bitField);	                
		        
		        peerBitField = util.receiveBitFieldMessage(in, cfg.noOfBytes);

		        if(util.isInterested(peerProcess.bitField, peerBitField))
		        {
		        	util.sendInterestedMessage(out);
		        }
		        else 
		        { 
		            util.sendNotInterestedMessage(out);
		        }
	        } finally {
	        	peerProcess.lock.unlock();
	        }
	        
	        
		} 		
		catch(IOException ioe) 
		{			
			ioe.printStackTrace();			
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
		    	switch(new String(msgType)) {
		    		case "CHOKE":
		    			byte indByte = peerProcess.bitField[requestedIndex/8];
		    			
		    			if(((1 << (7 - (requestedIndex%8))) & indByte) == 0) {
		    				peerProcess.requested[requestedIndex].set(false);
		    			}
		    			
		    			break;
		    			
		    		case "UNCHOKE":
		    			//Fetch the piece index to request
		    			int pIndex = util.getPieceIndexToRequest(peerProcess.bitField, peerBitField, peerProcess.requested);
		    			
		    			if(pIndex>=0) {
		    				requestedIndex = pIndex;
		    				peerProcess.requested[pIndex].set(true);
		    				util.sendRequestMessage(out, pIndex);
		    				req_time = System.currentTimeMillis();
		    			} else {
		    				util.sendNotInterestedMessage(out);
		    			}
		    			
		    			break;
		    			
		    		case "INTERESTED":
		    			if(!clientInterested)
		    				clientInterested = true;
		    			break;
		    			
		    		case "NOTINTERESTED": 
		    			clientInterested = false;
		    			choked = true;
		    			//sendChokeMessage();
		    			break;
		    			
		    		case "HAVE":
		    			//Have message contains 4 byte piece index payload
		    			byte[] pieceIndexbytes = util.readCompleteMsg(in, 4);
		    			int pieceIndex = ClientHelper.bytearray_to_int(pieceIndexbytes);
		    			
		    			byte indexByte = peerProcess.bitField[pieceIndex/8];
		    			//Checking if it has the piece, if not send interested message
		    			if(((1 << (7 - (pieceIndex%8))) & indexByte) == 0) {
		    				util.sendInterestedMessage(out);
		    				peerBitField[pieceIndex/8] |=  (1 << (7 - (pieceIndex%8)));
		    			}
		    			
		    			else 
		    	        { 
		    	            util.sendNotInterestedMessage(out);
		    	        }
		    			break;
		    			
		    		case "REQUEST":
		    			payload = new byte[4];
		    			in.read(payload);
		    			int pieceInd = ClientHelper.bytearray_to_int(payload);
		    			
		    			int startInd = pieceInd*cfg.PieceSize;
		    			
		    			byte[] data;
		    			if((cfg.FileSize-startInd) < cfg.PieceSize) {
		    				data = Arrays.copyOfRange(peerProcess.fileData, startInd, cfg.FileSize);
		    			} 
		    			
		    			else {
		    				data = Arrays.copyOfRange(peerProcess.fileData, startInd, startInd+cfg.PieceSize);
		    			}
		    			 
		    			if(!choked)
		    				util.sendPieceMessage(out, pieceInd, data);
		    			break;
		    			
		    		case "PIECE":
		    			byte[] pInd = new byte[4];
		    			in.read(pInd);
		    			
		    			int pcInd = ClientHelper.bytearray_to_int(pInd);
		    			
		    			byte[] pdata = util.readCompleteMsg(in, cfg.PieceSize);
		    			
		    			peerProcess.bitField[pcInd/8] |= 1 << (7-(pcInd%8));
		    			
		    			piecesRcvd++;
		    			total_time += (System.currentTimeMillis() - req_time)/1000;
		    			downloadRate = (float) ((piecesRcvd*cfg.PieceSize)/total_time);
		    			
		    			
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
		try {
			//converting msg length to byte array
			byte[] len = ClientHelper.int_to_bytearray(1);
			
			//appending message type to msg length, no payload for unchoke message
			byte[] res = ClientHelper.append_byte_to_bytearray(len, TCPMsgUtil.MessageType.UNCHOKE.val);
			 
			out.write(res);
			out.flush();
		} 
		catch(IOException ioe) 
		{
			ioe.printStackTrace();
		}
	}
	
	//Method to send choke message
	public void sendChokeMessage() {
		try {
			//converting msg length to byte array
			byte[] len = ClientHelper.int_to_bytearray(1);
			
			//appending message type to msg length, no payload for choke message
			byte[] res = ClientHelper.append_byte_to_bytearray(len, TCPMsgUtil.MessageType.CHOKE.val);
			 
			out.write(res);
			out.flush();
		} 
		catch(IOException ioe) 
		{
			ioe.printStackTrace();
		}
	}
	
    //Update the stopping condition to true when the method is called. This results in closing the socket connection
    public void closeClientThread(boolean end) 
    {
        
    	stoppingCondition = end;
        
    	if(end == true)
    	{
        
    		try
    		{
                if(!requestSocket.isClosed()) {
                	if(!requestSocket.isInputShutdown())
                		in.close();
                	if(!requestSocket.isOutputShutdown())
                		out.close();
    				requestSocket.close();
                }
            }
    		
    		catch (IOException e) 
    		{    
               System.out.println(e.getMessage());
    		}
        		
    	}
            
    }
}