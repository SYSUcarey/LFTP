package service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

import tools.ByteConverter;
import tools.Packet;

public class SendThread implements Runnable {
	private List<Packet> data;		//Ҫ���͵�����
	InetAddress address;			//Ŀ�ĵ�ַ
	int port;						//Ŀ�Ķ˿�
	
	public SendThread(List<Packet> data, InetAddress address, int port) {
		this.data = data;
		this.address = address;
		this.port = port;
	}

	@Override
	public void run() {
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
			for (int i = 0; i < data.size(); ++i) {
				byte[] buffer = ByteConverter.objectToBytes(data.get(i));
				DatagramPacket dp = new DatagramPacket(buffer, buffer.length, address, port);
				socket.send(dp);
			}
		} catch (SocketException e) {
			System.out.println("SendThread: ����socket����");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("SendThread: �������ݰ�����");
			e.printStackTrace();
		}
	}
}
