/*
 * Author: Arwid Bancewicz
 */

import java.io.*;
import java.net.*;
import java.util.LinkedList;

public class LeakyBucketImproved {
	public static void main(String[] args) throws IOException, InterruptedException {
		InetAddress addr = InetAddress.getByName(args[0]);
		DatagramSocket socketA = new DatagramSocket(4444);
		DatagramSocket socketB = new DatagramSocket();
		LinkedList<DatagramPacket> arrivalBuffer = new LinkedList<DatagramPacket>();
		
		FileOutputStream fout =  new FileOutputStream("leakybucket.txt");
		PrintStream pout = new PrintStream (fout);
		
		SenderThreadImproved sender = new SenderThreadImproved(socketB, addr, 4445, arrivalBuffer);
		ArrivalThreadImproved arrival = new ArrivalThreadImproved(sender, socketA, pout);
		sender.start();
		arrival.start();
  }
}

class ArrivalThreadImproved extends Thread {
	private static final int BUFFER_SIZE = 10000;
	DatagramSocket socket;
	LinkedList<DatagramPacket> arrivalBuffer;
	PrintStream pout;
	SenderThreadImproved sender;
	long lastTime = -1;
	ArrivalThreadImproved(SenderThreadImproved sender, DatagramSocket socket, PrintStream pout) {
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
			    pout.println(timeDiff + "\t" + p.getLength() + "\t" + sender.getBufferSize() + "\t" +  sender.getWindowSize());
			    sender.send(p);
			} catch (SocketException e) {
				e.printStackTrace();
				running = false;
			} catch (IOException e) {
				e.printStackTrace();
				running = false;
			}
		}
	}
}

class SenderThreadImproved extends Thread {
	private static final int MAX_WINDOW = 10000;
	private int window = MAX_WINDOW;
	private double rate = 2971.0; // tokens per millisecond
	private double rateTx = 1 / rate;
	private double lastTx = Double.NEGATIVE_INFINITY;
	DatagramSocket socket;
	LinkedList<DatagramPacket> arrivalBuffer;
	InetAddress addr;
	int port;
	SenderThreadImproved(DatagramSocket socket, InetAddress addr, int port, LinkedList<DatagramPacket> arrivalBuffer) {
		this.socket = socket;
		this.addr = addr;
		this.port = port;
		this.arrivalBuffer = arrivalBuffer;
	}
	public synchronized void send(DatagramPacket packet) throws IOException {
		arrivalBuffer.add(packet);
		send();
	}
	public synchronized boolean send(DatagramPacket packet, boolean addToBuffer) throws IOException {
		if (packet.getLength() <= window) {
		    DatagramPacket newPacket =
		                 new DatagramPacket(packet.getData(), packet.getLength(), addr, port);
			socket.send(newPacket);
			window -= packet.getLength();
			return true;
		} else if (addToBuffer) {
			arrivalBuffer.add(packet);
		}
		return false;
	}
	public synchronized void send() throws IOException {
		if (arrivalBuffer.size() <= 0) return;
		DatagramPacket packet = arrivalBuffer.removeFirst();
		if (send(packet, false) == false)
			arrivalBuffer.addFirst(packet);
		else
			send();
	}
	public void run() {
		while(true) {
			try {
				double now = System.currentTimeMillis();
				double nextTx = lastTx + rateTx;
				if (nextTx - now > 0) {
					Thread.sleep((long) (nextTx - now));
					lastTx = nextTx;
				} else {
					lastTx = now;
				}
				if (window < MAX_WINDOW) window++;
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
	public int getWindowSize() {
		return window;
	}
}