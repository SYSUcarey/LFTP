package service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import tools.ByteConverter;
import tools.Packet;

public class SendThread implements Runnable {
	private final static int BUFSIZE = 1024 * 1024;
	private List<Packet> data;		//Ҫ���͵�����
	InetAddress address;			//Ŀ�ĵ�ַ
	int sourcePort;					//Դ�˿�
	int destPort;					//Ŀ�Ķ˿�
	private int base = 0;			//�����
	private int nextSeq = 0;		//��һ�������ͷ�������
	private int N = 10;				//δȷ�ϵ���������
	private Date date;				//��¼������ʱ����ʱ��
	private DatagramSocket socket;	//���ڷ������ݰ�
	
	
	
	public SendThread(List<Packet> data, InetAddress address, int sourcePort, int destPort) {
		this.data = data;
		this.address = address;
		this.sourcePort = sourcePort;
		this.destPort = destPort;
		this.date = new Date();
		try {
			this.socket = new DatagramSocket(sourcePort);
		} catch (SocketException e) {
			System.out.println("SendThread: ����socket����");
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		//System.out.println("size: " + data.size());
		
		//��������ACK���߳�
		Thread recv_ack_thread = new Thread(new RecvAck());
		recv_ack_thread.start();
		
		// ������ʱ�жϴ����߳�
		Thread time_out_threadThread = new Thread(new TimeOut());
		time_out_threadThread.start();
		
		//�����������ݰ�
		try {
			while (nextSeq < data.size()) {
				if (nextSeq < base + N) {
					byte[] buffer = ByteConverter.objectToBytes(data.get(nextSeq));
					DatagramPacket dp = new DatagramPacket(buffer, buffer.length, address, destPort);
					socket.send(dp);
					if (base == nextSeq) startTimer();
					nextSeq++;
				}
			}
		} catch (IOException e) {
			System.out.println("SendThread: �������ݰ�����");
			e.printStackTrace();
		}
		
		//�������ʱ������һ��FIN����֪���շ�
		try {
			System.out.print("������ֹpacket");
			byte[] buffer = ByteConverter.objectToBytes(new Packet(0, 0, false, true, 0, null));
			DatagramPacket dp = new DatagramPacket(buffer, buffer.length, address, destPort);
			socket.send(dp);
			System.out.println("�������");
		} catch (IOException e) {
			System.out.println("SendThread: �������ݰ�����");
			e.printStackTrace();
		}
	
	}
	
	//����ACK�����߳�
	class RecvAck implements Runnable {
		@Override
		public void run() {
			try {
				while (true) {
					byte[] buffer = new byte[BUFSIZE];
					DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
					socket.receive(dp);
					Packet packet = ByteConverter.bytesToObject(buffer);
					System.out.println("ȷ�Ϸ���: " + packet.getAck());
					base = packet.getAck() + 1;
					if (base != nextSeq) startTimer();
					
					//ȷ�Ͻ������һ������
					if (packet.getAck() == data.size()) break;
				}
			} catch (IOException e) {
				System.out.println("ReceiveThread: �������ݰ�����");
			}
		}
	}
	
	//�ж��Ƿ�ʱ���߳�
	class TimeOut implements Runnable {
		@Override
		public void run() {
			while (true) {
				long start_time = date.getTime();
				long curr_time = new Date().getTime();
				//����3��ʱ������ʱ
				if (curr_time - start_time > 3000) {
					System.out.println("�����ش���");
					timeOut();
				}
				
				//ȷ�Ͻ������һ������ʱֹͣ��ʱ
				if (base == nextSeq) break;
			}
			
		}
	}
	
	//��ʱ�����ش��¼�
	private void timeOut() {
		startTimer();
		try {
			for (int i = base; i < nextSeq; ++i) {
				byte[] buffer = ByteConverter.objectToBytes(data.get(i));
				DatagramPacket dp = new DatagramPacket(buffer, buffer.length, address, destPort);
				System.out.println("���·���Ƭ�Σ�" + i);
				socket.send(dp);
			}
		} catch (IOException e) {
			System.out.println("SendThread: �������ݰ�����");
			e.printStackTrace();
		}
	}
	
	//������ʱ��
	private void startTimer() {
		date = new Date();
	}
}
