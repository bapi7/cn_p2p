import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class ClientThread extends Thread{
	Socket requestSocket;           //socket connect to the server
	BufferedOutputStream out;         //stream write to the socket
	BufferedInputStream in;          //stream read from the socket
	boolean isClient;
	String peerID;

	public ClientThread(Socket s, boolean isClient, String peerID) {
		try {
			this.requestSocket = s;
			this.isClient = isClient;
			//initialize inputStream and outputStream
			out = new BufferedOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new BufferedInputStream(requestSocket.getInputStream());
			
	        if(isClient) {
	        	//sending handshake message to the peer
	        	this.peerID = peerID;
                sendMessage(constructHandshakeMessage(peerID));
                receiveHandshakeMessage();
	        } else {
                Integer z = receiveHandshakeMessage();
                this.peerID = z.toString();
                sendMessage(constructHandshakeMessage(peerID));
	        }

	        if(peerID == "-1")
                this.setName("Peer : " + peerProcess.Id);
	        else
                this.setName("Peer : " + peerID);
	        
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void run()
	{
		/*try{
		
			while(true)
			{
				//sendMessage(message);
				//Receive the upperCase sentence from the server
				//MESSAGE = (String)in.readObject();
				//show the message to the user
				//System.out.println("Receive message: " + MESSAGE);
			}
		}
		catch (ConnectException e) {
			System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch ( ClassNotFoundException e ) {
        	System.err.println("Class not found");
    	}
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}*/
	}
	
	byte[] constructHandshakeMessage(String peerID) {
		String header = "P2PFILESHARINGPROJ";
		byte[] zero_bits = {0,0,0,0,0,0,0,0,0,0};
		byte[] header_and_zerobits = TCPMsgUtil.appendByteArray(header.getBytes(), zero_bits);
		byte[] handshakeMsg = TCPMsgUtil.appendByteArray(header_and_zerobits, peerID.getBytes());
		return handshakeMsg;
	}
	
	public int receiveHandshakeMessage() {
		Integer pId = -1;
		try {
            byte[] rcv = new byte[32];
            in.read(rcv);
            byte[] pInd = Arrays.copyOfRange(rcv, 28, 32);
            
            pId= Integer.parseInt(new String(pInd));
            
            /*if(isClient)
                if(peerHandShakeMap.containsKey(pId) && peerHandShakeMap.get(pId) == false)
                        peerHandShakeMap.put(pId, true);*/
        }
        catch (IOException ioe) {
        	ioe.printStackTrace();
        }
		return pId;
	}
	
	void sendMessage(byte[] msg)
	{
		try{
			//stream write the message
			out.write(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	
}