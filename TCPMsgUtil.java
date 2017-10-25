import java.awt.TrayIcon.MessageType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class TCPMsgUtil {

	public enum MessageType {
		CHOKE((byte)0),
		UNCHOKE((byte)1),
		INTERESTED((byte)2),
		NOTINTERESTED((byte)3),
		HAVE((byte)4),
		BITFIELD((byte)5),
		REQUEST((byte)6),
		PIECE((byte)7);
		
		byte val = -1;
		
		MessageType(byte b) {
			this.val = b;
		}
	}
	static byte[] appendByteArray(byte[] a, byte[] b ) {
		byte[] res = new byte[a.length + b.length];
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, b.length);
        return res;
    }
	
	static byte[] append_byte_to_bytearray(byte[] a, byte b) {
		byte[] res = new byte[a.length + 1];
        System.arraycopy(a, 0, res, 0, a.length);
        res[a.length] = b;
        return res;
	}
	
	static byte[] int_to_bytearray(int val) {
		byte[] conv = new byte[4];
		conv[0] = (byte) ((val >> 24) & 0xFF);
		conv[1] = (byte) ((val >> 16) & 0xFF);
		conv[2] = (byte) ((val >> 8) & 0xFF);
		conv[3] = (byte) (val & 0xFF);
		return conv;
	}
	
	static byte[] constructHandshakeMessage(String peerID) {
		String header = "P2PFILESHARINGPROJ";
		byte[] zero_bits = {0,0,0,0,0,0,0,0,0,0};
		byte[] header_and_zerobits = appendByteArray(header.getBytes(), zero_bits);
		byte[] handshakeMsg = appendByteArray(header_and_zerobits, peerID.getBytes());
		return handshakeMsg;
	}
	
	static byte[] constructActualMessage(MessageType msgType, byte[] payload) {
		Integer msgLength = payload.length + 1;
		//converting msg length to byte array
		byte[] len = int_to_bytearray(msgLength);
		//appending message type to msg length and then the message payload
		byte[] res = appendByteArray(TCPMsgUtil.append_byte_to_bytearray(len, msgType.val), payload);
		
		return res;
	}

}
