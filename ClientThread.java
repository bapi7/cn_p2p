import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class ClientThread extends Thread
{
	
	Socket requestSocket;           //socket connect to the server
	BufferedOutputStream out;         //stream write to the socket
	BufferedInputStream in;          //stream read from the socket
	
	boolean isClient;
	String peerID;
	byte[] bitField;
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

	public ClientThread(Socket s, boolean isClient, String peerID, config cfg, byte[] bitField, byte[] fileData) 
	{
		
		this.cfg = cfg;
		this.requestSocket = s;
		this.isClient = isClient;
		util = new TCPMsgUtil();
		
		try
		{
			
			this.bitField = bitField;
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
	        
	        util.sendBitFieldMessage(out, bitField);	                
	        
	        peerBitField = util.receiveBitFieldMessage(in, cfg.noOfBytes);

	        if(util.isInterested(bitField, peerBitField))
	        {
	        	util.sendInterestedMessage(out);
	        }
	        else 
	        { 
	            util.sendNotInterestedMessage(out);
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
			long req_time, total_time;
			int piecesRcvd = 0;
			byte[] msgLength, msgType;
			msgType = new byte[1];
			msgLength = new byte[4];
		    while(!stoppingCondition) 
			{
		    	in.read(msgLength);
		    	in.read(msgType);
		    	switch(new String(msgType)) {
		    		case "CHOKE":
		    			break;
		    			
		    		case "UNCHOKE": 
		    			break;
		    			
		    		case "INTERESTED":
		    			break;
		    			
		    		case "NOTINTERESTED": 
		    			break;
		    			
		    		case "HAVE":
		    			//Have message contains 4 byte piece index payload
		    			byte[] pieceIndexbytes = util.readCompleteMsg(in, 4);
		    			int pieceIndex = ClientHelper.bytearray_to_int(pieceIndexbytes);
		    			
		    			byte indexByte = bitField[pieceIndex/8];
		    			//Checking if it has the piece, if not send interested message
		    			if(((1 << (pieceIndex%8)) & indexByte) == 0) {
		    				util.sendInterestedMessage(out);
		    				peerBitField[pieceIndex/8] |=  (1 << (pieceIndex%8));
		    			}
		    			
		    			else 
		    	        { 
		    	            util.sendNotInterestedMessage(out);
		    	        }
		    			break;
		    			
		    		case "REQUEST": 
		    			
		    			break;
		    			
		    		case "PIECE":
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