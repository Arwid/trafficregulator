/*
 * Author: Arwid Bancewicz
 */

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class TrafficGeneratorVBR {

  public static void main(String[] args) throws IOException {
    
    InetAddress addr = InetAddress.getByName(args[0]);
    DatagramSocket socket = new DatagramSocket();
    
    BufferedReader bis = null; 
	String currentLine = null; 
	PrintStream pout = null;
	
	int count = 0;
	
	try {  
		
		/*
		 * Open input file as a BufferedReader
		 */ 
		File fin = new File("movietrace.data"); 
		FileReader fis = new FileReader(fin);  
		bis = new BufferedReader(fis);  
		
		/*
		 * Open file for output 
		 */
		FileOutputStream fout =  new FileOutputStream("trace.txt");
		pout = new PrintStream (fout);
		
		long startTimeMillis = System.currentTimeMillis();
		int byteMax = 0;
		long totalSize = 0;
		
		/*
		 *  Read file line-by-line until the end of the file 
		 */
		while ( (currentLine = bis.readLine()) != null) { 
			
			count++;
			if (count > 10000) break; // limit data points
			
			/*
			 *  Parse line and break up into elements 
			 */
			StringTokenizer st = new StringTokenizer(currentLine); 
			String col1 = st.nextToken(); 
			String col2 = st.nextToken(); 
			String col3  = st.nextToken(); 
			String col4 = st.nextToken(); 
			
			/*
			 *  Convert each element to desired data type 
			 */
			int SeqNo 	= Integer.parseInt(col1);
			float time 	= Float.parseFloat(col2);
			String Ftype 	= col3;
			int size 	= Integer.parseInt(col4);
			
			if (size > byteMax)
				byteMax = size;
			
			long timeMilli = (long)(time * (10*10*10));
			
			/*
			 * Send datagram
			 */
			long now = System.currentTimeMillis();
			if (count > 1) // No delay for first packet
				while (3.333333 > System.currentTimeMillis() - now) {
					// Wait
				}
			else
				startTimeMillis =  System.currentTimeMillis();
			
			
			int i = 1;
			while (size > 0) {
				int thisSize = size > 1480 ? 1480 : size;
				byte[] buf  = new byte[thisSize];
				buf[0] = 0x02;
				DatagramPacket packet =
					new DatagramPacket(buf, buf.length, addr, 4444);
				
				timeMilli = System.currentTimeMillis() - startTimeMillis;
				/*
				 *  Write line to output file 
				 */
				totalSize += thisSize;
				pout.println(timeMilli + "\t" + thisSize + "\t" + totalSize); 
				
				if (i-- == 0)
					break;
				socket.send(packet);
				
				size -= 1480;
			}
		}
		
	} catch (IOException e) {  
		// catch io errors from FileInputStream or readLine()  
		System.out.println("IOException: " + e.getMessage());
	} finally {  
		// Close files   
		if (bis != null) { 
			try { 
				bis.close(); 
				pout.close();
			} catch (IOException e) {
				System.out.println("IOException: " +  e.getMessage());  
			}
		}
	} 
  }
}

