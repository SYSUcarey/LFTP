package service;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tools.Percentage;
import tools.ByteConverter;
import tools.FileIO;
import tools.Packet;

public class ReceiveThread implements Runnable {
	private final static int BUFSIZE = 1024 * 1024;
	private final static int MAX_RWND = 1024 * 10;	// ���շ����մ���Ϊ10Mb��������������
	private DatagramSocket socket;					// UDP����DatagramSocket
	private int recvPort;							// ���շ����ն˿�
	private int expectedseqnum;						// �����յ������к�
	InetAddress sendInetAddress;					// ���ͷ�IP��ַ
	int sendPort;									// ���ͷ��˿�
	int rwnd = MAX_RWND;						
	String downloadDir;								// �洢λ��
	private boolean isClient;						// �ý����߳��Ƿ��ǿͻ��˴���
	private int fileSize;							// ���յ��ļ���С,�ͻ��˽���ʱ��Ҫ��֪
	
	public ReceiveThread(int recvPort, String dir, InetAddress sendInetAddress, int sendPort, boolean isClient, int fileSize) {
		this.recvPort = recvPort;
		this.downloadDir = dir;
		this.sendInetAddress = sendInetAddress;
		this.sendPort = sendPort;
		this.isClient = isClient;
		this.fileSize = fileSize;
		expectedseqnum = 0;
	}
	
	public InetAddress getSendInetAddress() {
		return sendInetAddress;
	}
	public void setSendInetAddress(InetAddress ia) {
		this.sendInetAddress = ia;
	}
	public int getSendPort() {
		return sendPort;
	}
	public void setSendPort(int port) {
		this.sendPort = port;
	}
	
	@Override
	public void run() {
		try {
			socket = new DatagramSocket(recvPort);
			byte[] buffer = new byte[BUFSIZE];
			List<byte[]> data = new ArrayList<>();
			DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
			// �����ȴ���һ�����ݰ�
			socket.receive(dp);
			/*// ��ȡ�ͻ���IP�ͷ��Ͷ˿�
			setClientInetAddress(dp.getAddress());
			setClientPort(dp.getPort());*/
			System.out.println("[INFO]���ͷ���ַ---" + sendInetAddress.toString().substring(1) + ":" + sendPort);
			String[] fileStringList = downloadDir.split("/");
			String fileName = fileStringList[fileStringList.length - 1];
			
			final Date startTime = new Date();
			//����ǿͻ��˽����ļ�����ʾ��������Ϣ
			Thread percentageThread = new Thread(new Runnable() {
				@Override
				public void run() {
					while(expectedseqnum < fileSize) {
						Percentage.showPercentage(fileSize, startTime, expectedseqnum-1);
					}
					Percentage.showPercentage(fileSize, startTime, expectedseqnum-1);
				}
			});
			if(isClient){
				percentageThread.start();
			}
			
			
			while (true) {		
				// ���յ������ݰ�ת���ɷ�װ��Packet
				Packet packet = ByteConverter.bytesToObject(buffer);
				// �ӵ�һ�����ݰ��л�ȡ���͵��ļ���,����շ���˵��ļ�����
				if(expectedseqnum == 0) {
			        File file=new File(downloadDir);
			         if(file.exists()&&file.isFile()) {
			             file.delete();
			         }
				}
				// ���յ���ɷ��͵�FIN�ź����ݰ�������ѭ��
				if (packet.isFIN() == true) break;
				// �жϵ�ǰrwnd�����Ƿ����������˽����ļ�д��
				if(rwnd == 0) {
					FileIO.byte2file(downloadDir, data);
					//System.out.println("�������ˣ�д�� " + MAX_RWND / 1024 + "Mb����.");
					// ���List,�����ڴ棬���ý��մ��ڿ��пռ�
					data = null;
					System.gc();
					data = new ArrayList<>();
					rwnd = MAX_RWND;
					Packet ackPacket = new Packet(expectedseqnum-1, -1, false, false, rwnd, null);
					byte[] ackBuffer = ByteConverter.objectToBytes(ackPacket);
					DatagramPacket ackdp = new DatagramPacket(ackBuffer, ackBuffer.length, sendInetAddress, sendPort);
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
					Packet ackPacket = new Packet(expectedseqnum-1, -1, true, false, rwnd, null);
					byte[] ackBuffer = ByteConverter.objectToBytes(ackPacket);
					DatagramPacket ackdp = new DatagramPacket(ackBuffer, ackBuffer.length, sendInetAddress, sendPort);
					socket.send(ackdp);
					//System.out.println("ACK(right): " + (expectedseqnum-1) + "������������expect: " + expectedseqnum + "������������get: " + packet.getSeq());
				}
				// ���ܵ����������ݰ�
				else {
					// ����һ��������ܵ�ACK��
					Packet ackPacket = new Packet(expectedseqnum-1, -1, true, false, rwnd, null);
					byte[] ackBuffer = ByteConverter.objectToBytes(ackPacket);
					DatagramPacket ackdp = new DatagramPacket(ackBuffer, ackBuffer.length, sendInetAddress, sendPort);
					socket.send(ackdp);
					//System.out.println("ACK(wrong): " + (expectedseqnum-1) + "������������expect: " + expectedseqnum + "������������get: " + packet.getSeq());
				}
				// �����ȴ���һ�����ݰ�
				socket.receive(dp);
			}
			FileIO.byte2file(downloadDir, data);
			if(isClient) {
				percentageThread.join();
			}
			System.out.println("[INFO]�ɹ������ļ�" + fileName);
		}
		catch (SocketException e) {
			System.out.println("ReceiveThread: ����socket����");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("ReceiveThread: �������ݰ�����");
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("ReceiveThread: �����̳߳���");
			e.printStackTrace();
		}
	}
	

}
