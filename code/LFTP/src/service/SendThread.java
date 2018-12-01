package service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.List;

import com.sun.xml.internal.bind.v2.model.core.ID;

import tools.ByteConverter;
import tools.Packet;

public class SendThread implements Runnable {
	private final static int BUFSIZE = 1024 * 1024;
	private final static boolean SS = false;				//������״̬
	private final static boolean CA = true;				//ӵ������״̬
	private int TTL = 300;							//��ʱ�����ڼ��
	private List<Packet> data;						//Ҫ���͵�����
	InetAddress address;							//Ŀ�ĵ�ַ
	int sourcePort;									//Դ�˿�
	int destPort;									//Ŀ�Ķ˿�
	private volatile int base = 0;					//�����
	private volatile int nextSeq = 0;				//��һ�������ͷ�������
	private double cwnd = 1.0;							//ӵ�����ƴ��ڴ�С(congWin)
	private volatile Date date;						//��¼������ʱ����ʱ��
	private DatagramSocket socket;					//���ڷ������ݰ�
	private volatile boolean retrans = false;		//��ǰ�Ƿ����ش�
	private volatile int lastAcked = -1;			//���һ����ȷ�ϵķ���ack
	private volatile int rwnd = 1024;				//���շ����л���ռ�
	private String fileName;						//�ļ���
	private volatile boolean status;					//ӵ������״̬
	private volatile double threshold = 50;			//ӵ�����ƴ�����ֵ
	private volatile int dupliACK = 0;					//����ACK��
	
	
	public SendThread(List<Packet> data, InetAddress address, int sourcePort, int destPort, String fileName) {
		this.data = data;
		this.address = address;
		this.sourcePort = sourcePort;
		this.destPort = destPort;
		this.date = new Date();
		this.fileName = fileName;
		this.status = SS;
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
		Thread time_out_threadThread;
		time_out_threadThread = new Thread(new TimeOut());
		time_out_threadThread.start();
		
		//�����������ݰ�
		try {
			while (nextSeq < data.size()) {
				//���շ�������
				if (rwnd <= 0) {
					System.out.println("���շ�����������ͣ����");
				}
				else if (nextSeq < base + cwnd && retrans == false) {
					byte[] buffer = ByteConverter.objectToBytes(data.get(nextSeq));
					DatagramPacket dp = new DatagramPacket(buffer, buffer.length, address, destPort);
					Packet packet = ByteConverter.bytesToObject(dp.getData());
					System.out.println("���͵ķ������: " + packet.getSeq());
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
		while (true) {
			if (lastAcked == data.size() - 1 && rwnd > 0) {
				try {
					System.out.println("������ֹpacket");
					byte[] buffer = ByteConverter.objectToBytes(new Packet(-1, -1, false, true, -1, null, fileName));
					DatagramPacket dp = new DatagramPacket(buffer, buffer.length, address, destPort);
					socket.send(dp);
					System.out.println("�������");
				} catch (IOException e) {
					System.out.println("SendThread: �������ݰ�����");
					e.printStackTrace();
				}
				break;
			}
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
					
					if (packet.isACK() == true && lastAcked + 1 == packet.getAck()) {
						//���յ���ȷ��ACK��
						if (status == SS) {
							cwnd++;
							if (cwnd > threshold) status = CA;
						}
						else {
							cwnd += (double)(1 / cwnd);
						}
						dupliACK = 0;
					}
					else if (packet.isACK() == true) {
						dupliACK++;
					}
					
					//��⵽3������ACK
					if (dupliACK == 3) {
						threshold = cwnd / 2;
						cwnd = threshold + 3;
						status = CA;
					}
					
					if (packet.isACK()) {
						base = packet.getAck() + 1;
						lastAcked = packet.getAck();
					}
					rwnd = packet.getRwwd();
					
					//���շ�������������������һ��ֻ��һ���ֽ����ݵı��Ķ�
					new Thread(new Runnable() {
						@Override
						public void run() {
							while (rwnd == 0) {
								byte[] tmp = ByteConverter.objectToBytes(new Packet(-1, -1, false, false, -1, null, fileName));
								DatagramPacket tmpPack = new DatagramPacket(tmp, tmp.length, address, destPort);
								try {
									socket.send(tmpPack);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					}).start();
					
					
					
					if (base != nextSeq) startTimer();
					
					//ȷ�Ͻ������һ������
					if (packet.isACK() && packet.getAck() == data.size() - 1) break;
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
				//����0.3��ʱ������ʱ
				if (curr_time - start_time > TTL) timeOut();
				
				//ȷ�Ͻ������һ������ʱֹͣ��ʱ
				if (lastAcked == data.size() - 1) break;
			}
		}
	}
	
	//��ʱ�����ش��¼�
	private void timeOut() {
		threshold = cwnd / 2;
		cwnd = threshold;
		status = SS;
		System.out.println("�����ش���");
		startTimer();
		try {
			//��¼baseֵ��nextSeqֵ����ֹ�����̶߳�����ɸı�
			int myBase = base, myNextSeq = nextSeq;
			retrans = true;
			for (int i = myBase; i < myNextSeq; ++i) {
				while (rwnd <= 0) System.out.println("���շ����治������ͣ�ش�"); 
				byte[] buffer = ByteConverter.objectToBytes(data.get(i));
				DatagramPacket dp = new DatagramPacket(buffer, buffer.length, address, destPort);
				System.out.println("���·���Ƭ�Σ�" + i);
				socket.send(dp);
			}
			retrans = false;
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
