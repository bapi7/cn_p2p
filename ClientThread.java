import java.net.*;
import java.io.*;
import java.io.BufferedReader;
import java.io.FileReader;
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
	Thread intThread;
	TCPMsgUtil util;
	byte[] fileData;

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
	        //Thread to handle bit field and interested/not interested messages
	        intThread = new Thread() 
	        {
	            	
	        	public void run() 
	        	{	                
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
	                
	        		notify();
	            }
	        };
	        
	        intThread.start();
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
			BufferedInputStream inp_stream = new BufferedInputStream(requestSocket.getInputStream());
			
		    //while(!stoppingCondition) 
			 //{

				//sendMessage(message);
			
				//Receive the upperCase sentence from the server
			
				//MESSAGE = (String)in.readObject();
			
				//show the message to the user
			    // if any messages are 
				//System.out.println("Receive message: " + MESSAGE);
			
			//}
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
}