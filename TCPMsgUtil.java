import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;


/*
 * This class contains functions that handle TCP messages
 * like constructing different messages, sending messages
 * to peers and receiving messages from peers
*/


public class TCPMsgUtil 
{

	public enum MessageType 
	{
		
		CHOKE((byte)0),
		
		UNCHOKE((byte)1),
		
		INTERESTED((byte)2),
		
		NOTINTERESTED((byte)3),
		
		HAVE((byte)4),
		
		BITFIELD((byte)5),
		
		REQUEST((byte)6),
		
		PIECE((byte)7);
		
		byte val = -1;
		
		MessageType(byte b) 
		{
			this.val = b;
		}
		
	}
	
	byte[] want;
	
	//Method to construct a handshake message
	static byte[] constructHandshakeMessage(String peerID) 
	{
		String header = "P2PFILESHARINGPROJ";
		
		byte[] zero_bits = {0,0,0,0,0,0,0,0,0,0};
		
		byte[] header_and_zerobits = ClientHelper.appendByteArray(header.getBytes(), zero_bits);
		
		byte[] handshakeMsg = ClientHelper.appendByteArray(header_and_zerobits, peerID.getBytes());
		
		return handshakeMsg;
		
	}
	
	//Method to construct a message to be sent
	static byte[] constructActualMessage(MessageType msgType, byte[] payload) 
	{
		Integer msgLength = payload.length + 1;
		
		//converting msg length to byte array
		byte[] len = ClientHelper.int_to_bytearray(msgLength);
		
		//appending message type to msg length and then the message payload
		byte[] res = ClientHelper.appendByteArray(ClientHelper.append_byte_to_bytearray(len, msgType.val), payload);
		
		return res;
		
	}
	
	//Method to receive a handshake message to a peer
	public int receiveHandshakeMessage(InputStream in, int expectedPeerId)
	{
		
		Integer pId = -1;
		
		try 
		{
            
			byte[] rcv = new byte[32];         
			in.read(rcv);			
			byte[] header = Arrays.copyOfRange(rcv, 0, 18);			
			byte[] pInd = Arrays.copyOfRange(rcv, 28, 32);
            pId= Integer.parseInt(new String(pInd));            
            if(header.toString() != "P2PFILESHARINGPROJ" || pId != expectedPeerId)
            {
				return -1;
            }
            
            /*if(isClient)
                if(peerHandShakeMap.containsKey(pId) && peerHandShakeMap.get(pId) == false)
                        peerHandShakeMap.put(pId, true);*/
        } 
		
		catch(IOException ioe) 
		{
			ioe.printStackTrace();
		}
		
		return pId;
	}

	//Method to send an bit field message to a peer
	public void sendBitFieldMessage(OutputStream out, byte[] bitField) 
	{
		
        byte[] msg = constructActualMessage(MessageType.BITFIELD, bitField);
        sendMessage(out, msg);
		
	}

	//Method to receive a bit field message to a peer
	public synchronized byte[] receiveBitFieldMessage(InputStream in, int len) {
		
		byte[] msgLength, msgType;
		byte[] rcv = new byte[0];
		
		try
		{
			
			//4 bytes for message length and 1 byte for message type
			msgLength = new byte[4];			
			in.read(msgLength);			
			msgType = new byte[1];			
			in.read(msgType);
			rcv = new byte[len];
			
			if(msgType[0] == MessageType.BITFIELD.val) 
			{				
				in.read(rcv);				
			}

		}
		catch (IOException ioe) 
		{			
			ioe.printStackTrace();		
		}		
		return rcv;
		
	}
	
	//Method to fetch the next piece index to request
	public int getPieceIndexToRequest(byte[] bitField, byte[] rcvBitField, AtomicBoolean[] reqBitField) {
		byte[] req = new byte[bitField.length];
		Arrays.fill(req, (byte)0);
		//convert reqBitField to byte array
		byte[] reqBitFieldByte = ClientHelper.atomicbooleanarray_to_bytearray(reqBitField, req);
		int pieceIndex = 0;
		byte[] reqAndCurr = new byte[bitField.length];
		List<Integer> nonEmpty = new ArrayList<Integer>();
		
		for(int i=0;i<bitField.length;i++) 
		{
			reqAndCurr[i] = (byte)(bitField[i] | reqBitFieldByte[i]);
			want[i] = (byte)((reqAndCurr[i] ^ rcvBitField[i]) & ~reqAndCurr[i]);
			
			if(want[i]!=0)
				nonEmpty.add(i);
		}
		
		if(nonEmpty.isEmpty())
			return -1;
		
		int byteInd = nonEmpty.get(ThreadLocalRandom.current().nextInt(0, nonEmpty.size()));
		byte rand = want[byteInd];
		
		int bitInd = ThreadLocalRandom.current().nextInt(0, 8);
		for(int i=0;i<8;i++) {
			if((rand & (1 << i)) !=0) {
				bitInd = i;
				break;
			}
		}
		
		pieceIndex = (byteInd*8) + (7-bitInd);
				
		return pieceIndex;
	}
		
		
	//Method to check if the peer is interested in any piece from the connected peer
	public boolean isInterested(byte[] bitField, byte[] rcvBitField) {
		
		boolean isInt = false;		
		byte[] missing = new byte[bitField.length];		
				
		for(int i=0;i<bitField.length;i++) 
		{			
			missing[i] = (byte)(bitField[i] ^ rcvBitField[i]);		
			want[i] = (byte) (missing[i] & ~bitField[i]);
			
			if(want[i]!=0)
				isInt = true;
				
		}
		
		return isInt;
	}
	
	//Method to send an interested message to a peer
	public void sendInterestedMessage(OutputStream out) {
		
		//converting msg length to byte array
		byte[] len = ClientHelper.int_to_bytearray(1);
			
		//appending message type to msg length, no payload for interested message
		byte[] res = ClientHelper.append_byte_to_bytearray(len, MessageType.INTERESTED.val);
			 
		sendMessage(out, res);
		
	}
	
	//Method to send an not interested message to a peer
	public void sendNotInterestedMessage(OutputStream out) {
		
		//converting msg length to byte array
		byte[] len = ClientHelper.int_to_bytearray(1);
			
		//appending message type to msg length, no payload for not interested message
		byte[] res = ClientHelper.append_byte_to_bytearray(len, MessageType.NOTINTERESTED.val);
			
		sendMessage(out, res);
		
	}
	
	//Method to send request message
	public void sendRequestMessage(OutputStream out, int pieceIndex) {
		
		byte[] msg = constructActualMessage(MessageType.REQUEST, ClientHelper.int_to_bytearray(pieceIndex));
			
		sendMessage(out, msg);
			
	}
	
	//Method to send the piece message
	public void sendPieceMessage(OutputStream out, int pieceIndex, byte[] data) {
		
		byte[] msg = constructActualMessage(MessageType.PIECE, ClientHelper.appendByteArray(ClientHelper.int_to_bytearray(pieceIndex), data));
		
		sendMessage(out, msg);
		
	}
	
	//Method to send a message to a peer
	public void sendMessage(OutputStream out, byte[] msg) 
	{
		
		try
		{
			//stream write the message
			out.write(msg);
			out.flush();
		}
		catch(IOException ioException)
		{
			ioException.printStackTrace();
		}
		
	}
	
	//This method handles messages received in segments
	public byte[] readCompleteMsg(InputStream input_stream, int len) 
    {
		byte[] b = new byte[0];
		
		int rem = len;
        
		try 
		{
			while(rem != 0) 
			{
				int avail = input_stream.available();          
				int read = (len <= avail) ? len : avail;           
				byte[] r = new byte[read];
            
				if(read != 0)
				{
					
					input_stream.read(r);               
					b = ClientHelper.appendByteArray(b, r);                
					rem -= read;
					
				}
				
			}
			
		}
		
		catch(IOException ioe) 
		{
			ioe.printStackTrace();
		}
        
        return b;
    }

}
