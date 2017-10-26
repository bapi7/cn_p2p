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
	byte[] bitField;
	config cfg;


	public ClientThread(Socket s, boolean isClient, String peerID, config cfg) {
		this.cfg = cfg;
		try {
			Vector<RemotePeerInfo> pv = cfg.peerInfoVector;
			String has_file = "0";
			for(RemotePeerInfo rpi : pv){
				int rpi_id = Integer.parseInt(rpi.peerId);
				if(rpi_id == peerProcess.Id){
					has_file = rpi.peerBitField;
				}
			}

			int setBit = Integer.parseInt(has_file);
			int size = cfg.noOfBytes;
			bitField = new byte[size];
			if(setBit == 0){

				Arrays.fill(bitField, (byte)0);
			}
			else{
				Arrays.fill(bitField, (byte)1);
			}

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
                receiveHandshakeMessage(); //Add logic to check header and return the header
	        } else {
                Integer rcv = receiveHandshakeMessage();
                this.peerID = rcv.toString();
                sendMessage(TCPMsgUtil.constructHandshakeMessage(peerID));
	        }

	        if(peerID == "-1")
                this.setName("Peer : " + peerProcess.Id);
	        else
                this.setName("Peer : " + peerID);

	        //Check whether handshake header is right and peer id is correct one


		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void run() {
		try {
			BufferedInputStream inp_stream = new BufferedInputStream(requestSocket.getInputStream());

			sendBitFieldMessage();
		    //This client is waiting to receive bitfieldmessage from peer.

		    //Incase if bitfield message is received check for interested and not interested fields
		    //based on the bit field messages received from peer B, Peer A sends a message with interested or not interested.



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
            //byte[] val = new byte[10];
            //getBitFieldValue();
            byte[] msg = TCPMsgUtil.constructActualMessage(TCPMsgUtil.MessageType.BITFIELD, bitField);
            out.write(msg);
            out.flush();
        } catch (IOException ioe) {
        	ioe.printStackTrace();
        }
	}

	public void receiveBitFieldMessage(){
		Integer pId = -1;
		try{
			int n = cfg.noOfBytes;
			//4 bytes for message length and 1 byte for message type
			n = n+5;
			// Take care of extra bits in the last byte
			byte[] rcv = new byte[n];
			in.read(rcv);
            byte[] pInd = Arrays.copyOfRange(rcv, 28, 32);

            pId= Integer.parseInt(new String(pInd));


		}catch (IOException ie) {

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