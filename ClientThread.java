import java.net.*;
import java.awt.TrayIcon.MessageType;
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
                sendMessage(TCPMsgUtil.constructHandshakeMessage(peerID));
                receiveHandshakeMessage();
	        } else {
                Integer rcv = receiveHandshakeMessage();
                this.peerID = rcv.toString();
                sendMessage(TCPMsgUtil.constructHandshakeMessage(peerID));
	        }

	        if(peerID == "-1")
                this.setName("Peer : " + peerProcess.Id);
	        else
                this.setName("Peer : " + peerID);
	        
	        sendBitFieldMessage();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void run() {
		try {
			BufferedInputStream inp_stream = new BufferedInputStream(requestSocket.getInputStream());
			//while(!stoppingCondition) {
				
				//sendMessage(message);
				//Receive the upperCase sentence from the server
				//MESSAGE = (String)in.readObject();
				//show the message to the user
				//System.out.println("Receive message: " + MESSAGE);
			//}
		} catch(IOException ioException){
			ioException.printStackTrace();
		} finally{
			//Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
			} catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
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
        } catch (IOException ioe) {
        	ioe.printStackTrace();
        }
		return pId;
	}
	
	public void sendBitFieldMessage() {
		try {
            byte[] val = new byte[10]; 
            //getBitFieldValue();
            byte[] msg = TCPMsgUtil.constructActualMessage(TCPMsgUtil.MessageType.BITFIELD, val);
            out.write(msg);
            out.flush();
        } catch (IOException ioe) {
        	ioe.printStackTrace();
        }
	}
	
	void sendMessage(byte[] msg) {
		try {
			//stream write the message
			out.write(msg);
			out.flush();
		} catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	
	static {
		
	}
	
}