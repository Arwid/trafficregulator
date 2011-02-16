/*
 * Author: Arwid Bancewicz
 */

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class TrafficGenerator {

	public static final int N = 9; // 1 to 9

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
			File fin = new File("poisson3.data");
			FileReader fis = new FileReader(fin);
			bis = new BufferedReader(fis);

			/*
			 * Open file for output
			 */
			FileOutputStream fout = new FileOutputStream("trace.txt");
			pout = new PrintStream(fout);

			long startTimeMillis = System.currentTimeMillis();

			/*
			 * Read file line-by-line until the end of the file
			 */
			while ((currentLine = bis.readLine()) != null) {

				count++;
				if (count > 10000)
					break; // limit data points

				/*
				 * Parse line and break up into elements
				 */
				StringTokenizer st = new StringTokenizer(currentLine);
				String col1 = st.nextToken();
				String col2 = st.nextToken();
				String col3 = st.nextToken();

				/*
				 * Convert each element to desired data type
				 */
				int SeqNo = Integer.parseInt(col1);
				float time = Float.parseFloat(col2);
				int size = Integer.parseInt(col3);

				/*
				 * Re-scale data
				 */
				time *= 10;
				size *= N;

				byte[] buf = new byte[size];
				buf[0] = 0x01;
				long timeMilli = (long) (time / (10 * 10 * 10));

				/*
				 * Write line to output file
				 */
				pout.println(SeqNo + "\t" + timeMilli + "\t" + size);

				/*
				 * Delay sending packet
				 */
				if (count > 1) // No delay for first packet
					while (timeMilli > System.currentTimeMillis()
							- startTimeMillis) {
						// Wait
					}
				else
					startTimeMillis = System.currentTimeMillis();

				/*
				 * Send packet
				 */
				DatagramPacket packet = new DatagramPacket(buf, buf.length,
						addr, 4444);
				socket.send(packet);
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
					System.out.println("IOException: " + e.getMessage());
				}
			}
		}
	}
}