/*
 * Author: Arwid Bancewicz
 */

import java.io.*;
import java.net.*;

public class TrafficSink {
	public static void main(String[] args) throws IOException {
		DatagramSocket socket = new DatagramSocket(4445);
		byte[] buf = new byte[14800];
		DatagramPacket p = new DatagramPacket(buf, buf.length);
		PrintStream pout;

		/*
		 * Open file for output
		 */
		FileOutputStream fout = new FileOutputStream("output.txt");
		pout = new PrintStream(fout);

		long count = 0;
		long timestamp = System.currentTimeMillis();
		long startTime = 0;
		long totalSize = 0;

		while (true) {
			socket.receive(p);

			count++;
			if (count == 1)
				startTime = System.currentTimeMillis();

			long timeDiff = System.currentTimeMillis() - timestamp;
			timestamp = System.currentTimeMillis();
			long time = System.currentTimeMillis() - startTime;

			int size = p.getLength();

			if (count == 1) {
				time = 0; // The time is zero for the first packet
				timeDiff = 0;
			}

			totalSize += size;

			/*
			 * Write line to output file
			 */
			pout.println(timeDiff + "\t" + time + "\t" + size + "\t"
					+ totalSize);
		}
	}
}
