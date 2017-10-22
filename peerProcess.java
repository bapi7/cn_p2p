import java.io.*;
import java.net.*;

public class peerProcess {
	
	//private static final int sPort = 8000;   //The server will be listening on this port number

	public static void main(String[] args) throws Exception {
		System.out.println("The server is running.");
		ServerSocket listener = new ServerSocket(Integer.parseInt(args[0]));
		Client client = new Client();
		client.start();
		int clientNum = 1;
        	try {
            		while(true) {
                		new Handler(listener.accept(),clientNum).start();
                		System.out.println("Client "  + clientNum + " is connected!");
                		clientNum++;
            		}
        	} finally {
            		listener.close();
        	} 
 
    }
	
	private static class Client extends Thread{
		Socket requestSocket;           //socket connect to the server
		ObjectOutputStream out;         //stream write to the socket
		ObjectInputStream in;          //stream read from the socket
		String message;                //message send to the server
		String MESSAGE;                //capitalized message read from the server

		public void run()
		{
			try{
				//create a socket to connect to the server
				requestSocket = new Socket("localhost", 8000);
				System.out.println("Connected to localhost in port 8000");
				//initialize inputStream and outputStream
				out = new ObjectOutputStream(requestSocket.getOutputStream());
				out.flush();
				in = new ObjectInputStream(requestSocket.getInputStream());
			
				//get Input from standard input
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
				while(true)
				{
					System.out.print("Hello, please input a sentence: ");
					//read a sentence from the standard input
					message = bufferedReader.readLine();
					//Send the sentence to the server
					sendMessage(message);
					//Receive the upperCase sentence from the server
					MESSAGE = (String)in.readObject();
					//show the message to the user
					System.out.println("Receive message: " + MESSAGE);
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
			}
		}
		//send a message to the output stream
		void sendMessage(String msg)
		{
			try{
				//stream write the message
				out.writeObject(msg);
				out.flush();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	
	
	/**
 	* A handler thread class.  Handlers are spawned from the listening
 	* loop and are responsible for dealing with a single client's requests.
 	*/
	private static class Handler extends Thread {
    	private String message;    //message received from the client
    	private String MESSAGE;    //upper case message send to the client
    	private Socket connection;
    	private ObjectInputStream in;	//stream read from the socket
    	private ObjectOutputStream out;    //stream write to the socket
    	private int no;		//The index number of the client

    	public Handler(Socket connection, int no) {
        	this.connection = connection;
    		this.no = no;
    	}

    	public void run() {
    		try{
    			//initialize Input and Output streams
    			out = new ObjectOutputStream(connection.getOutputStream());
    			out.flush();
    			in = new ObjectInputStream(connection.getInputStream());
    			try{
    				while(true)
    				{
    					//receive the message sent from the client
    					message = (String)in.readObject();
    					//show the message to the user
    					System.out.println("Receive message: " + message + " from client " + no);
    					//Capitalize all letters in the message
    					MESSAGE = message.toUpperCase();
    					//send MESSAGE back to the client
    					sendMessage(MESSAGE);
    				}
    			}
    			catch(ClassNotFoundException classnot){
    				System.err.println("Data received in unknown format");
    			}
    		}
    		catch(IOException ioException){
    			System.out.println("Disconnect with Client " + no);
    		}
    		finally{
    			//Close connections
    			try{
    				in.close();
    				out.close();
    				connection.close();
    			}
    			catch(IOException ioException){
    				System.out.println("Disconnect with Client " + no);
    			}
    		}
    	}

    	//send a message to the output stream
    	public void sendMessage(String msg)
    	{
    		try{
    			out.writeObject(msg);
    			out.flush();
    			System.out.println("Send message: " + msg + " to Client " + no);
    		}
    		catch(IOException ioException){
    			ioException.printStackTrace();
    		}
    	}

	}
	
}

