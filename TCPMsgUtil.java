import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class TCPMsgUtil {

	static byte[] appendByteArray(byte[] a, byte[] b ) {
		byte[] res = new byte[a.length + b.length];
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, b.length);
        return res;
    }

}
