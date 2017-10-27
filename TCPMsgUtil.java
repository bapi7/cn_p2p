import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

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
	
	static byte[] constructHandshakeMessage(String peerID) 
	{
		String header = "P2PFILESHARINGPROJ";
		
		byte[] zero_bits = {0,0,0,0,0,0,0,0,0,0};
		
		byte[] header_and_zerobits = ClientHelper.appendByteArray(header.getBytes(), zero_bits);
		
		byte[] handshakeMsg = ClientHelper.appendByteArray(header_and_zerobits, peerID.getBytes());
		
		return handshakeMsg;
		
	}
	
	static byte[] constructActualMessage(MessageType msgType, byte[] payload) 
	{
		Integer msgLength = payload.length + 1;
		
		//converting msg length to byte array
		byte[] len = ClientHelper.int_to_bytearray(msgLength);
		
		//appending message type to msg length and then the message payload
		byte[] res = ClientHelper.appendByteArray(ClientHelper.append_byte_to_bytearray(len, msgType.val), payload);
		
		return res;
		
	}
	
	public synchronized int receiveHandshakeMessage(InputStream in, int expectedPeerId)
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

	public synchronized void sendBitFieldMessage(OutputStream out, byte[] bitField) 
	{
		
		try 
		{
            
            byte[] msg = TCPMsgUtil.constructActualMessage(TCPMsgUtil.MessageType.BITFIELD, bitField);
            out.write(msg);
            out.flush();
        } 
		
		catch(IOException ioe) 
		{
			ioe.printStackTrace();
        }
		
	}

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
			
			if(msgType[0] == TCPMsgUtil.MessageType.BITFIELD.val) 
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
	
	public synchronized boolean isInterested(byte[] bitField, byte[] rcvBitField) {
		
		boolean isInt = false;		
		byte[] missing = new byte[bitField.length];		
		int q;		
		for(int i=0;i<bitField.length;i++) 
		{			
			missing[i] = (byte)(bitField[i] ^ rcvBitField[i]);		
			q = (byte) (missing[i] & ~bitField[i]);
			
			if(q!=0) 
			{
				isInt = true;
				break;
			}
				
		}
		
		return isInt;
	}
	
	public synchronized void sendInterestedMessage(OutputStream out) {
		try {
			//converting msg length to byte array
			byte[] len = ClientHelper.int_to_bytearray(5);
			
			//appending message type to msg length, no payload for interested message
			byte[] res = ClientHelper.append_byte_to_bytearray(len, MessageType.INTERESTED.val);
			 
			out.write(res);
			out.flush();
		} 
		catch(IOException ioe) 
		{
			ioe.printStackTrace();
		}
	}
	
	public synchronized void sendNotInterestedMessage(OutputStream out) {
		try {
			//converting msg length to byte array
			byte[] len = ClientHelper.int_to_bytearray(5);
			
			//appending message type to msg length, no payload for not interested message
			byte[] res = ClientHelper.append_byte_to_bytearray(len, MessageType.NOTINTERESTED.val);
			
			out.write(res);
			out.flush();
			
		} 
		catch(IOException ioe) 
		{
			ioe.printStackTrace();
		}
	}
	
	public synchronized void sendMessage(OutputStream out, byte[] msg) 
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
	
	public synchronized byte[] readCompleteMsg(InputStream input_stream, int len) 
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
