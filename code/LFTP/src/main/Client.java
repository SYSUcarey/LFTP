package main;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import service.ReceiveThread;
import service.SendThread;
import tools.*;



public class Client {
	private static final int BUFFER_SIZE = 1024;
	private int port;
	private String fileName;
	private String operation;
	private InetAddress  serverAddress;				//��������ַ
	private int serverCPort;						//���������ƶ˿�
	private int serverDPort;						//���������ݶ˿�
	private DatagramSocket socket;
	private DatagramPacket send_packet;
	private volatile DatagramPacket rcv_packet;
	private byte[] buffer;
	private volatile Date start_date;						//���ڼ�¼�Ƿ�ʱ
	private volatile boolean isRecved = false;
	
	public Client(int port, String operation, String fileName, InetAddress severAddress, int serverCPort) {
		this.port = port;
		this.fileName = fileName;
		this.operation = operation;
		this.serverAddress = severAddress;
		this.serverCPort = serverCPort;
		try {
			//��socket�����������֮�佨����ϵ
			//��������ϵ�����ڴ�������
			buffer = new byte[BUFFER_SIZE];
			socket = new DatagramSocket(port);
			rcv_packet = new DatagramPacket(buffer, buffer.length);
		} catch (SocketException e) {
			System.err.println("[ERROR]�����ͻ��˳���: " + e.getMessage());
		}
	}
	
	public void run() {
		String message = operation + " " + fileName;
		send_packet = new DatagramPacket(message.getBytes(), message.getBytes().length, serverAddress, serverCPort);
		//���Ͱ�����������֪�ͻ�������
		try {
			socket.send(send_packet);
		} catch (IOException e) {
			System.err.println("[ERROR]�ͻ��˷������ݰ�����: " + e.getMessage());
		}
		start_date = new Date();
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (isRecved == false) {
					if (new Date().getTime() - start_date.getTime() > 5000) {
						System.err.println("[ERROR]������ϣ�������");
						System.exit(0);
					}
				}
			}
		}).start();
		
		try {
			//���յ���������Ӧ�İ�
			socket.receive(rcv_packet);
			isRecved = true;
			switch (operation) {
				case "listall":
					String result = new String(buffer, 0, rcv_packet.getLength());
					System.out.println("[INFO]�������д��ڵ��ļ�: \n" + result);
					break;
				case "lget":
					//���������߳�,��ʱ�������˷����̻߳�δ����
					message = new String(buffer, 0, rcv_packet.getLength());
					InetAddress sendInetAddress = rcv_packet.getAddress();
					String dataPort = message.substring(message.indexOf(":")+1, message.indexOf("fileSize"));
					String fileSize = message.substring(message.lastIndexOf(":")+1);
					File dir = new File("download/");
					if (!dir.exists()) {
						dir.mkdir();
					}
					Thread rcv_thread = new Thread(new ReceiveThread(port + 1, "download/" + fileName, sendInetAddress, Integer.parseInt(dataPort), true, Integer.parseInt(fileSize)));
					rcv_thread.start();
					//����һ���������������ݶ˿ڣ���֪�����߳̿���
					socket.send(new DatagramPacket(new byte[1], 1, sendInetAddress, Integer.parseInt(dataPort) - 1));
					break;
				case "lsend":
					//���������߳�,��ʱ�������˽����߳��ѿ���
					message = new String(buffer, 0, rcv_packet.getLength());
					InetAddress serverAddress = rcv_packet.getAddress();
					dataPort = message.substring(message.indexOf(":")+1);
					Thread send_thread = new Thread(new SendThread(serverAddress, port + 1, Integer.parseInt(dataPort), fileName, true));
					send_thread.start();
					break;
				default:
					System.err.println("[ERROR]��Ч����");
					break;
			}
		} catch (IOException e) {
			System.err.println("[ERROR]�ͻ��˽������ݰ�����: " + e.getMessage());
		}
	}
	
	/*public static void main(String[] args) {
	String filePath = "test.mp4";
	String address = "127.0.0.1";
	int sourcePort = 3777;
	int dstPort = 3888;
	System.out.println("������ " + address + ":" + dstPort + " �����ļ�: " + filePath);
	try {
		System.out.println("׼�����䣺");
    	InetAddress ia = InetAddress.getByName(address);
    	Thread send_thread = new Thread(new SendThread(ia, sourcePort, dstPort, filePath));
    	send_thread.start();
    	send_thread.join();
    	System.exit(0);
	} catch (Exception e) {
		// TODO: handle exception
		e.printStackTrace();
	}*/
	
    
}
