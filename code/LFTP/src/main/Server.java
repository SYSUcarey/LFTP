package main;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;

import service.ReceiveThread;
import service.SendThread;
import tools.FileIO;

public class Server {
	private static final int BUFFER_SIZE = 1024;
	private int cPort;					//���ƶ˿�
	private volatile DatagramSocket socket;
	private DatagramPacket send_packet;
	private DatagramPacket rcv_packet;
	private byte[] buffer;
	
	public Server(int cPort) {
		this.cPort = cPort;
		buffer = new byte[BUFFER_SIZE];
		try {
			//��socket������ͻ���֮�佨����ϵ
			//�շ����ݴ���Ŀ�����Ϣ
			socket = new DatagramSocket(cPort);
			rcv_packet = new DatagramPacket(buffer, buffer.length);
		} catch (SocketException e) {
			System.err.println("[ERROR]��������������: " + e.getMessage());
			//e.printStackTrace();
		}
	}
	
	public void run() throws SocketException {
		while (true) {
			//�ȴ��������Կͻ��˵�����
			try {
				socket.receive(rcv_packet);
			} catch (IOException e) {
				System.err.println("[ERROR]�������ݰ�����: " + e.getMessage());
			}
			String message = new String(buffer, 0, rcv_packet.getLength());
			System.out.println("[cmd]" + message);
			String operation = message.substring(0, message.indexOf(" "));
			String fileName = message.substring(message.indexOf(" ") + 1);
			//��ÿͻ��˵ĵ�ַ�Ͷ˿�
			InetAddress address = rcv_packet.getAddress();
			int port = rcv_packet.getPort();
			int dataPort;
			switch (operation) {
				case "lget":
					//�������ݶ˿�
					dataPort = 20000 + (int)(Math.random() * 1000);
					int fileSize = FileIO.getBufferLength("server/" + fileName);
					message = "dataport:" + dataPort + "fileSize:" + fileSize;
					//��֪�ͻ������ݶ˿�
					send_packet = new DatagramPacket(message.getBytes(), message.getBytes().length, address, port);
					try {
						socket.send(send_packet);
					} catch (IOException e) {
						System.err.println("[ERROR]�������ݰ�����: " + e.getMessage());
					}
					
					DatagramSocket messSocket = new DatagramSocket(dataPort - 1);
					messSocket.setSoTimeout(5000);
					//��֪�ͻ��˽����ý����߳�
					try {
						messSocket.receive(rcv_packet);
					} catch (IOException e) {
						System.err.println("[ERROR]���ӿͻ��˳�ʱ: " + e.getMessage());
						//���½����������ӣ����������ʱ�б�Ҫ
						messSocket.close();
						break;
					}
					//û�������쳣ʱ���������߳�
					System.out.println("[INFO]��ʼ�����ļ� " + fileName);
					System.out.println("[INFO]���������ݶ˿� " + dataPort);
					messSocket.close();
					Thread send_thread = new Thread(new SendThread(address, dataPort, port + 1, "server/" + fileName, false));
					send_thread.start();
					break;
				case "lsend":
					//�������ݶ˿�
					dataPort = 30000 + (int)(Math.random() * 1000);
					message = "dataport:" + dataPort;
					
					//���������߳�
					File dir = new File("server/");
					if (!dir.exists()) {
						dir.mkdir();
					}
					System.out.println("[INFO]��ʼ�����ļ� " + fileName);
					System.out.println("[INFO]���������ݶ˿� " + dataPort);
					Thread recv_thread = new Thread(new ReceiveThread(dataPort, "server/" + fileName, address, port + 1, false, 0));
					recv_thread.start();
					//��֪�ͻ��˽����߳̿����Լ����ݶ˿�
					//��֪�ͻ������ݶ˿�
					send_packet = new DatagramPacket(message.getBytes(), message.getBytes().length, address, port);
					try {
						socket.send(send_packet);
					} catch (IOException e) {
						System.err.println("[ERROR]�������ݰ�����: " + e.getMessage());
					}
					break;
				case "listall":
					File file=new File("server/");
					String result = "";
					for(File temp:file.listFiles()){
			            if(!temp.isDirectory()){
			                result += "[INFO]" + temp.toString().substring(temp.toString().indexOf("/")) + "\n";
			            }
			        }
					if (result == "") result = "[INFO]��������û���ļ�.";
					send_packet = new DatagramPacket(result.getBytes(), result.getBytes().length, address, port);
					try {
						socket.send(send_packet);
					} catch (IOException e) {
						System.err.println("[ERROR]�������ݰ�����: " + e.getMessage());
					}
					break;
				default:
					break;
				}
		}
	}
}
