/*
 * Author: Arwid Bancewicz
 */

import java.io.*;
import java.net.*;
import java.util.LinkedList;

public class FIFOScheduler {
	public static void main(String[] args) throws IOException, InterruptedException {
		InetAddress addr = InetAddress.getByName(args[0]);
		DatagramSocket socketA = new DatagramSocket(4444);
		DatagramSocket socketB = new DatagramSocket();
		LinkedList<DatagramPacket> arrivalBuffer = new LinkedList<DatagramPacket>();
		
		FileOutputStream fout =  new FileOutputStream("fifoscheduler.txt");
		PrintStream pout = new PrintStream (fout);
		
		SenderThread sender = new SenderThread(socketB, addr, 4445, arrivalBuffer);//.start();
		ArrivalThread arrival = new ArrivalThread(sender, socketA, pout);
		sender.start();
		arrival.start();
  }
}

class ArrivalThread extends Thread {
	private static final int BUFFER_SIZE = 9000;
	DatagramSocket socket;
	LinkedList<DatagramPacket> arrivalBuffer;
	PrintStream pout;
	SenderThread sender;
	long lastTime = -1;
	ArrivalThread(SenderThread sender, DatagramSocket socket, PrintStream pout) {
		this.socket = socket;
		this.pout = pout;
		this.sender = sender;
	}
	public void run() {
		boolean running = true;
		while (running) {
			try {
				byte[] buf = new byte[BUFFER_SIZE];
			    DatagramPacket p = new DatagramPacket(buf, buf.length);
			    socket.receive(p);
			    long timeStamp = System.currentTimeMillis();
			    long timeDiff = lastTime == -1 ? 0 : timeStamp - lastTime;
			    lastTime = timeStamp;
			    pout.println(timeDiff + "\t" + p.getLength() + "\t" + sender.getBufferSize() + "\t" +  sender.getCapacity());
			    sender.send(p);
			} catch (SocketException e) {
				e.printStackTrace();
				running = false;
			} catch (InterruptedException e) {
				e.printStackTrace();
				running = false;
			} catch (IOException e) {
				e.printStackTrace();
				running = false;
			}
		}
	}
}

class SenderThread extends Thread {
	private final int MAX_SIZE = 100*1024; // 100 KB
	private int capacity = MAX_SIZE;
	private double rate = 125;// bytes per millisecond
	private double lastTx = Double.NEGATIVE_INFINITY;
	private long timeMilli = 0;
	private long startTimeMillis = System.currentTimeMillis();
	DatagramSocket socket;
	LinkedList<DatagramPacket> arrivalBuffer;
	InetAddress addr;
	int port;
	SenderThread(DatagramSocket socket, InetAddress addr, int port, LinkedList<DatagramPacket> arrivalBuffer) {
		this.socket = socket;
		this.addr = addr;
		this.port = port;
		this.arrivalBuffer = arrivalBuffer;
	}
	public synchronized void send(DatagramPacket packet) throws IOException, InterruptedException {
		if (packet.getLength() <= capacity) {
			arrivalBuffer.add(packet);
			capacity -= packet.getLength();
		} else {
			System.out.println("Packet is dropped.");
		}
	}
	public synchronized void send() throws IOException, InterruptedException {
		if (arrivalBuffer.size() <= 0) return;
		DatagramPacket packet = arrivalBuffer.removeFirst();
		DatagramPacket newPacket =
            new DatagramPacket(packet.getData(), packet.getLength(), addr, port);
		double spacing = packet.getLength() / rate;
		/*
		 * Delay sending packet
		 */
		while (spacing > System.currentTimeMillis() - lastTx) {
			// Wait
		}
		lastTx = System.currentTimeMillis();
		socket.send(newPacket);
		capacity += packet.getLength();
		send();
	}
	public void run() {
		while(true) {
			try {
				/*
				double now = System.currentTimeMillis();
				double nextTx = lastTx + 1 / rate;
				if (nextTx - now > 0) {
					Thread.sleep((long) (nextTx - now));
					lastTx = nextTx;
				} else {
					lastTx = now;
				}
				if (window < MAX_WINDOW) window++;
				*/
				send();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public int getBufferSize() {
		return arrivalBuffer.size();
	}
	public int getCapacity() {
		return capacity;
	}
}