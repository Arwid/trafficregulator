/*
 * Author: Arwid Bancewicz
 */

import java.io.*;
import java.net.*;
import java.util.LinkedList;

public class DualLeakyBucket {
	public static void main(String[] args) throws IOException, InterruptedException {
		InetAddress addr = InetAddress.getByName(args[0]);
		DatagramSocket socketA = new DatagramSocket(4444);
		DatagramSocket socketB = new DatagramSocket();
		LinkedList<DatagramPacket> arrivalBuffer = new LinkedList<DatagramPacket>();
		
		FileOutputStream fout =  new FileOutputStream("leakybucket.txt");
		PrintStream pout = new PrintStream (fout);
		
		DualSender sender = new DualSender(socketB, addr, 4445, arrivalBuffer);
		DualArrivalThread arrival = new DualArrivalThread(sender, socketA, pout);
		sender.start();
		arrival.start();
  }
}

class DualArrivalThread extends Thread {
	private static final int BUFFER_SIZE = 10000;
	DatagramSocket socket;
	LinkedList<DatagramPacket> arrivalBuffer;
	PrintStream pout;
	DualSender sender;
	long lastTime = -1;
	DualArrivalThread(DualSender sender, DatagramSocket socket, PrintStream pout) {
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
			    pout.println(timeDiff + "\t" + p.getLength() + "\t" + sender.getBufferSize() + "\t" +  sender.getWindow1Size() + "\t" + sender.getWindow2Size());
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

class WindowThread extends Thread {
	private int MAX_WINDOW = 10000;
	private int window = MAX_WINDOW;
	private double rate = 2971.0; // tokens per millisecond
	private double lastTx = Double.NEGATIVE_INFINITY;
	private DualSender sender;
	WindowThread(int window, double rate, DualSender sender) {
		MAX_WINDOW = window;
		this.window = MAX_WINDOW;
		this.rate = rate;
		this.sender = sender;
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
				//System.out.println(Thread.currentThread().getId()+":"+(nextTx - now) + " " + now + "-"+nextTx + " window is " + window);
				if (window < MAX_WINDOW) window++;
				sender.send();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public int getWindow() {
		return window;
	}
	
	public void reduceWindow(int num) {
		window -= num;
	}
}

class DualSender {
	DatagramSocket socket;
	LinkedList<DatagramPacket> arrivalBuffer;
	InetAddress addr;
	int port;
	WindowThread window1;
	WindowThread window2;
	DualSender(DatagramSocket socket, InetAddress addr, int port, LinkedList<DatagramPacket> arrivalBuffer) {
		this.socket = socket;
		this.addr = addr;
		this.port = port;
		this.arrivalBuffer = arrivalBuffer;
		this.window1 = new WindowThread(800, 100, this);
		this.window2 = new WindowThread(2000, 18, this);
	}
	public void start() {
		this.window1.start();
		this.window2.start();
	}
	public synchronized void send(DatagramPacket packet) throws IOException {
		arrivalBuffer.add(packet);
		send();
	}
	public synchronized boolean send(DatagramPacket packet, boolean addToBuffer) throws IOException {
		if (packet.getLength() <= Math.min(window1.getWindow(), window2.getWindow())) {
		    DatagramPacket newPacket =
		                 new DatagramPacket(packet.getData(), packet.getLength(), addr, port);
			socket.send(newPacket);
			window1.reduceWindow(packet.getLength());
			window2.reduceWindow(packet.getLength());
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
	public int getBufferSize() {
		return arrivalBuffer.size();
	}
	public int getWindow1Size() {
		return window1.getWindow();
	}
	public int getWindow2Size() {
		return window2.getWindow();
	}
}