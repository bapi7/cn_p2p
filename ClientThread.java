import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class ClientThread extends Thread
{
	
	Socket requestSocket;           //socket connect to the server
	BufferedOutputStream out;         //stream write to the socket
	BufferedInputStream in;          //stream read from the socket
	
	boolean isClient;
	String peerID;
	byte[] bitField;
	config cfg;
	Runnable intThread;
	TCPMsgUtil util;
	byte[] fileData;
	boolean clientInterested;
	Float downloadRate;
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
	        
	        byte[] rcv = util.receiveBitFieldMessage(in, cfg.noOfBytes);

	        if(util.isInterested(bitField, rcv))
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
		    			break;
		    			
		    		case "REQUEST": 
		    			break;
		    			
		    		case "PIECE":
		    			break;
		    			
		    		default:
		    			break;
		    	}
		    	//if(cfg.NumberOfPreferredNeighbors)
				//sendMessage(message);
			
				//Receive the upperCase sentence from the server
			
				//MESSAGE = (String)in.readObject();
			
				//show the message to the user
			    // if any messages are 
				//System.out.println("Receive message: " + MESSAGE);
			
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