package service;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import tools.ByteConverter;
import tools.FileIO;
import tools.Packet;

public class ReceiveThread implements Runnable {
	private final static int BUFSIZE = 1024 * 1024;
	private final static int MAX_RWND = 1024 * 10;	// ���������ܴ���Ϊ10Mb��������������
	private DatagramSocket socket;					// UDP����DatagramSocket
	private int serverPort;							// ����˽��ն˿�
	private int expectedseqnum;						// �����յ������к�
	InetAddress clientInetAddress;					// �ͻ��˷���IP��ַ
	int clientPort;									// �ͻ��˷��Ͷ˿�
	String fileName;								// �ͻ��˷��͵��ļ���
	int rwnd = MAX_RWND;						
	String dir = "server/"	;						// ����˴洢λ��
	
	public ReceiveThread(int port) {
		this.serverPort = port;
		expectedseqnum = 0;
	}
	
	public InetAddress getClientInetAddress() {
		return clientInetAddress;
	}
	public void setClientInetAddress(InetAddress ia) {
		this.clientInetAddress = ia;
	}
	public int getClientPort() {
		return clientPort;
	}
	public void setClientPort(int port) {
		this.clientPort = port;
	}
	
	@Override
	public void run() {
		try {
			socket = new DatagramSocket(serverPort);
			byte[] buffer = new byte[BUFSIZE];
			List<byte[]> data = new ArrayList<>();
			DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
			// �����ȴ���һ�����ݰ�
			socket.receive(dp);
			// ��ȡ�ͻ���IP�ͷ��Ͷ˿�
			setClientInetAddress(dp.getAddress());
			setClientPort(dp.getPort());
			System.out.println("���ͷ���ַ---" + clientInetAddress.toString().substring(1) + ":" + clientPort);
			
			while (true) {		
				// ���յ������ݰ�ת���ɷ�װ��Packet
				Packet packet = ByteConverter.bytesToObject(buffer);
				// �ӵ�һ�����ݰ��л�ȡ���͵��ļ���,����շ���˵��ļ�����
				if(expectedseqnum == 0) {
					fileName = packet.getFileName();
					System.out.println("�ļ���---" + fileName);
			        File file=new File(dir + fileName);
			         if(file.exists()&&file.isFile()) {
			             file.delete();
			         }
				}
				// ���յ���ɷ��͵�FIN�ź����ݰ�������ѭ��
				if (packet.isFIN() == true) break;
				// �жϵ�ǰrwnd�����Ƿ����������˽����ļ�д��
				if(rwnd == 0) {
					String dirString = "server/" + fileName;
					FileIO.byte2file(dirString, data);
					System.out.println("�������ˣ�д�� " + MAX_RWND / 1024 + "Mb����.");
					// ���List�����ý��մ��ڿ��пռ�
					data.clear();
					rwnd = MAX_RWND;
					Packet ackPacket = new Packet(expectedseqnum-1, -1, false, false, rwnd, null, fileName);
					byte[] ackBuffer = ByteConverter.objectToBytes(ackPacket);
					DatagramPacket ackdp = new DatagramPacket(ackBuffer, ackBuffer.length, clientInetAddress, clientPort);
					socket.send(ackdp);
					//System.out.println("ACK(rwnd): " + (expectedseqnum-1) + "������������expect: " + expectedseqnum);
				}
				// ���յ������յ������ݰ�
				else if(packet.getSeq() == expectedseqnum) {
					// ��ȡ���ݰ����ݽ�����
					data.add(packet.getData());
					// �ڴ���һ�����кŵ����ݰ�
					expectedseqnum++;
					// ���ܿ��д��ڼ���1
					rwnd--;
					//System.out.println("����Ƭ�Σ�" + packet.getSeq());
					// ����һ����ȷ���ܵ�ACK��
					Packet ackPacket = new Packet(expectedseqnum, -1, true, false, rwnd, null, fileName);
					byte[] ackBuffer = ByteConverter.objectToBytes(ackPacket);
					DatagramPacket ackdp = new DatagramPacket(ackBuffer, ackBuffer.length, clientInetAddress, clientPort);
					socket.send(ackdp);
					//System.out.println("ACK(right): " + (expectedseqnum-1) + "������������expect: " + expectedseqnum + "������������get: " + packet.getSeq());
				}
				// ���ܵ����������ݰ�
				else {
					// ����һ��������ܵ�ACK��
					Packet ackPacket = new Packet(expectedseqnum-1, -1, true, false, rwnd, null, fileName);
					byte[] ackBuffer = ByteConverter.objectToBytes(ackPacket);
					DatagramPacket ackdp = new DatagramPacket(ackBuffer, ackBuffer.length, clientInetAddress, clientPort);
					socket.send(ackdp);
					//System.out.println("ACK(wrong): " + (expectedseqnum-1) + "������������expect: " + expectedseqnum + "������������get: " + packet.getSeq());
				}
				// �����ȴ���һ�����ݰ�
				socket.receive(dp);
			}
			String dirString = dir + fileName;
			FileIO.byte2file(dirString, data);
			System.out.println("���ղ�д����ϣ�");
		}
		catch (SocketException e) {
			System.out.println("ReceiveThread: ����socket����");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("ReceiveThread: �������ݰ�����");
			e.printStackTrace();
		}
	}
	

}
