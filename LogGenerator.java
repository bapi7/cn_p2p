import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.logging.Formatter;

import java.util.logging.*;
import java.io.File;

//Overwriting the formatter to customize and write into log files


class NewFormatterOverrideFormatAndDate extends Formatter 
{

	
	//Overwriting the format method  of formatter
	
	public String format(LogRecord log_record) 
	{
	
		StringBuilder      string_builder     =     new     StringBuilder    (1000)     ;
		
		string_builder		.		append		(		calcDate   (     log_record.getMillis()      )      );
		
		string_builder	.	append	(	" "		);
		
		string_builder	.		append		(		log_record .  getMessage	()	)	;
		
			string_builder		.		append	(			"\n"	)			;
		
		return string_builder		.	toString	()	;
	}
	
//Overwriting the calculate Date function to display  date in the format of "MMM dd,yyyy HH:mm:ss"
	
	public String calcDate(long inputTime) 
	{    
		//Giving the date format to be used in log files
		
		SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
		
		Date output_Time = new Date(inputTime);
		
		return date_format.format(output_Time);
	   
	}

   
}


public class LogGenerator 
{
	
	
	static Logger log_data;
	//return the log_data
	public static Logger getMyLogger() 
	{
		return log_data;

	}
	
	//Function to write the log files in the specified format directory of each peer
	
	static public void LoggerSet() 
	
	{

		log_data = Logger.getLogger(LogGenerator.class.getName());
		
		
		log_data.setUseParentHandlers(false);

		
		Handler[]     hdl 		=		 log_data 		.  	getHandlers   ()  ;
		
		
		if 		(hdl != null )
		{
				if(hdl.length > 0 )
		
				{
			
					if(hdl[0] instanceof ConsoleHandler) 
				
						log_data.removeHandler(hdl[0]);
			
					}
		}
			
		NewFormatterOverrideFormatAndDate formatterText;
		
		log_data.setLevel(Level.INFO);

        
        //Initializing a  new FileHandler
        
        FileHandler fh = null;
        
        //Creating a directory with peer_"peerid"
        
        try {
			fh = new FileHandler("log_peer_"
			        + 	peerProcess.Id + ".log"	)	;
		}
        //Catch any security exception occurred and print the message
        catch (SecurityException e) 
        {
			
			System.out.println(e.getMessage());
		} 
        //catch any IO Exception occured and print the message
        
        catch (IOException e) 
        {
			
			System.out.println(e.getMessage());
		}
		
        // Instantiating the class that overrides formatter
        formatterText = new NewFormatterOverrideFormatAndDate();
		
        fh.setFormatter(formatterText);
		
        log_data.addHandler(fh);
	}

	

}
