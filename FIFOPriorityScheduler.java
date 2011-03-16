/*
 * Author: Arwid Bancewicz
 */

import java.io.*;
import java.net.*;
import java.util.LinkedList;

public class FIFOPriorityScheduler {
	public static void main(String[] args) throws IOException, InterruptedException {
		InetAddress addr = InetAddress.getByName(args[0]);
		DatagramSocket socketA = new DatagramSocket(4444);
		DatagramSocket socketB = new DatagramSocket();
		
		FileOutputStream fout =  new FileOutputStream("fifopriorityscheduler.txt");
		PrintStream pout = new PrintStream (fout);
		
		SenderThreadP sender = new SenderThreadP(socketB, addr, 4445);//.start();
		ArrivalThreadP arrival = new ArrivalThreadP(sender, socketA, pout);
		sender.start();
		arrival.start();
  }
}

class ArrivalThreadP extends Thread {
	private static final int BUFFER_SIZE = 14800;
	DatagramSocket socket;
	LinkedList<DatagramPacket> arrivalBuffer;
	PrintStream pout;
	SenderThreadP sender;
	long lastTime = -1;
	
	ArrivalThreadP(SenderThreadP sender, DatagramSocket socket, PrintStream pout) {
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
			} catch (IOException e) {
				e.printStackTrace();
				running = false;
			}
		}
	}
}

class SenderThreadP extends Thread {
	private final int MAX_SIZE = 100*1024; // 100 KB
	private int capacity1 = MAX_SIZE;
	private int capacity2 = MAX_SIZE;
	private double rate = 2971.0; // tokens per millisecond
	private double lastTx = Double.NEGATIVE_INFINITY;
	DatagramSocket socket;
	LinkedList<DatagramPacket> arrivalBufferHigh;
	LinkedList<DatagramPacket> arrivalBufferLow;
	InetAddress addr;
	int port;
	
	SenderThreadP(DatagramSocket socket, InetAddress addr, int port) {
		this.socket = socket;
		this.addr = addr;
		this.port = port;
		this.arrivalBufferHigh = new LinkedList<DatagramPacket>();
		this.arrivalBufferLow = new LinkedList<DatagramPacket>();
	}
	public synchronized void send(DatagramPacket packet) throws IOException {
		if (packet.getData()[0] == 0x01) {
			if (packet.getLength() <= capacity1) {
				arrivalBufferHigh.add(packet);
				capacity1 -= packet.getLength();
				System.out.println("Buffer1 - Capacity: "+capacity1 + " down "+packet.getLength());
				send();
			} else {
				System.out.println("Buffer1 - Packet is dropped.");
			}
		} else if (packet.getData()[0] == 0x02) {
			if (packet.getLength() <= capacity2) {
				arrivalBufferLow.add(packet);
				capacity2 -= packet.getLength();
				System.out.println("Buffer2 - Capacity: "+capacity1 + " down "+packet.getLength());
				send();
			} else {
				System.out.println("Buffer2 - Packet is dropped.");
			}
		}
	}
	public synchronized void send() throws IOException {
		if (arrivalBufferHigh.size() <= 0 && arrivalBufferLow.size() <= 0) return;
		if (arrivalBufferHigh.size() > 0) {
			DatagramPacket packet = arrivalBufferHigh.removeFirst();
			DatagramPacket newPacket =
	            new DatagramPacket(packet.getData(), packet.getLength(), addr, port);
			socket.send(newPacket);
			capacity1 += packet.getLength();
			System.out.println("Buffer1 - Back to "+capacity1);
			send();
		} else {
			DatagramPacket packet = arrivalBufferLow.removeFirst();
			DatagramPacket newPacket =
	            new DatagramPacket(packet.getData(), packet.getLength(), addr, port);
			socket.send(newPacket);
			capacity2 += packet.getLength();
			System.out.println("Buffer1 - Back to "+capacity2);
			send();
		}
	}
	public void run() {
		while(true) {
			try {
				double now = System.currentTimeMillis();
				double nextTx = lastTx + 1 / rate;
				if (nextTx - now > 0) {
					Thread.sleep((long) (nextTx - now));
					lastTx = nextTx;
				} else {
					lastTx = now;
				}
				//if (window < MAX_WINDOW) window++;
				send();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public int getBufferSize() {
		return arrivalBufferHigh.size();
	}
	public int getCapacity() {
		return capacity1;
	}
}