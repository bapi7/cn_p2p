import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.logging.Formatter;

import java.util.logging.*;
import java.io.File;

public class PeerLogger {
	
	static Logger log_info;
	
	public static Logger getLogger(Integer peerId) {
		log_info = Logger.getLogger(PeerLogger.class.getName());
		
		log_info.setLevel(Level.INFO);
		
		FileHandler fhand = null;
		try {
			fhand = new FileHandler("log_peer_" + peerId + ".log");
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		fhand.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        });
		
		log_info.addHandler(fhand);
		return log_info;
	}

}
