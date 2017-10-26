import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

public class config {

	Integer NumberOfPreferredNeighbors;
	Integer UnchokingInterval;
	Integer OptimisticUnchokingInterval;
	String FileName;
	Integer FileSize;
	Integer PieceSize;
	Integer port = 8000;
	public Vector<RemotePeerInfo> peerInfoVector;
	Integer noOfPieces;
	Integer noOfBytes;

	public config(){
		String st;
		int i1;
		peerInfoVector = new Vector<RemotePeerInfo>();
		try {
			BufferedReader in = new BufferedReader(new FileReader("Common.cfg"));
			while((st = in.readLine()) != null) {
				String[] tokens = st.split("\\s+");
				switch(tokens[0]) {
				case "NumberOfPreferredNeighbors":
					NumberOfPreferredNeighbors = Integer.parseInt(tokens[1]);
					break;
				case "UnchokingInterval":
					UnchokingInterval = Integer.parseInt(tokens[1]);
					break;
				case "OptimisticUnchokingInterval":
					OptimisticUnchokingInterval = Integer.parseInt(tokens[1]);
					break;
				case "FileName":
					FileName = tokens[1];
					break;
				case "FileSize":
					FileSize = Integer.parseInt(tokens[1]);
					break;
				case "PieceSize":
					PieceSize = Integer.parseInt(tokens[1]);
					break;
				}
			}

			if(FileSize%PieceSize == 0 )
			{
				noOfPieces = FileSize/PieceSize;
			}
			else
			{
				noOfPieces = FileSize/PieceSize+1;
			}

			if(noOfPieces/8==0)
			{
				noOfBytes = noOfPieces/8;
			}
			else
			{
				noOfBytes = noOfPieces/8+1;
			}

			in = new BufferedReader(new FileReader("PeerInfo.cfg"));
			while((st = in.readLine()) != null) {
				String[] tokens = st.split("\\s+");
		    	peerInfoVector.addElement(new RemotePeerInfo(tokens[0], tokens[1], tokens[2],tokens[3]));
			}



			in.close();
		}
		catch (Exception ex) {
			System.out.println(ex.toString());
		}

	}




}
