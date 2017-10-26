public class ClientHelper 
{

	static byte[] appendByteArray(byte[] a, byte[] b ) 
	{
		
		byte[] res = new byte[a.length + b.length];
		
        System.arraycopy(a, 0, res, 0, a.length);
        
        System.arraycopy(b, 0, res, a.length, b.length);
        
        return res;
        
    }
	
	static byte[] append_byte_to_bytearray(byte[] a, byte b) 
	{
		
		byte[] res = new byte[a.length + 1];
		
        System.arraycopy(a, 0, res, 0, a.length);
        
        res[a.length] = b;
        
        return res;
        
	}
	
	static byte[] int_to_bytearray(int val) 
	{
		
		byte[] conv = new byte[4];
		
		conv[0] = (byte) ((val >> 24) & 0xFF);
		
		conv[1] = (byte) ((val >> 16) & 0xFF);
		
		conv[2] = (byte) ((val >> 8) & 0xFF);
		
		conv[3] = (byte) (val & 0xFF);
		
		return conv;
		
	}
	
}