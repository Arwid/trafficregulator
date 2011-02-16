/*
 * Author: Arwid Bancewicz
 */

import java.io.*;
import java.net.*;

public class TrafficGeneratorFixed {

  public static void main(String[] args) throws IOException, InterruptedException {
    
    InetAddress addr = InetAddress.getByName(args[0]);
    DatagramSocket socket = new DatagramSocket();
	
	int PacketSize = 100;
	long spacing = 8;
	long timeMilli = 0;
	long startTimeMillis = System.currentTimeMillis();
	
	int count = 0;
	
	try {  
		byte[] buf = new byte[PacketSize];
		DatagramPacket packet =
            new DatagramPacket(buf, buf.length, addr, 4444);
		
		startTimeMillis =  System.currentTimeMillis();
		while ( count++ <= 1000) { 
			count ++;
			
			socket.send(packet);
			
			/*
			 * Delay sending packet
			 */
			timeMilli += spacing;
			while (timeMilli > System.currentTimeMillis() - startTimeMillis) {
				// Wait
			}
			//Thread.sleep(Spacing);
		} 
		//pout.println("max:" +byteMax);
		
	} catch (IOException e) {  
		// catch io errors from FileInputStream or readLine()  
		System.out.println("IOException: " + e.getMessage());
	} finally {  
	} 
  }
}

