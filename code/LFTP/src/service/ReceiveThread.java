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
	private final static int MAX_RWND = 1024 * 10;	// 服务器接受窗口为10Mb，用于流量控制
	private DatagramSocket socket;					// UDP连接DatagramSocket
	private int serverPort;							// 服务端接收端口
	private int expectedseqnum;						// 期望收到的序列号
	InetAddress clientInetAddress;					// 客户端发送IP地址
	int clientPort;									// 客户端发送端口
	String fileName;								// 客户端发送的文件名
	int rwnd = MAX_RWND;						
	String dir = "server/"	;						// 服务端存储位置
	
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
			// 阻塞等待第一个数据包
			socket.receive(dp);
			// 获取客户端IP和发送端口
			setClientInetAddress(dp.getAddress());
			setClientPort(dp.getPort());
			System.out.println("发送方地址---" + clientInetAddress.toString().substring(1) + ":" + clientPort);
			
			while (true) {		
				// 将收到的数据包转换成封装的Packet
				Packet packet = ByteConverter.bytesToObject(buffer);
				// 从第一个数据包中获取发送的文件名,并清空服务端的文件内容
				if(expectedseqnum == 0) {
					fileName = packet.getFileName();
					System.out.println("文件名---" + fileName);
			        File file=new File(dir + fileName);
			         if(file.exists()&&file.isFile()) {
			             file.delete();
			         }
				}
				// 接收到完成发送的FIN信号数据包，跳出循环
				if (packet.isFIN() == true) break;
				// 判断当前rwnd窗口是否已满，满了进行文件写入
				if(rwnd == 0) {
					String dirString = "server/" + fileName;
					FileIO.byte2file(dirString, data);
					System.out.println("窗口满了，写入 " + MAX_RWND / 1024 + "Mb数据.");
					// 清空List，重置接收窗口空闲空间
					data.clear();
					rwnd = MAX_RWND;
					Packet ackPacket = new Packet(expectedseqnum-1, -1, false, false, rwnd, null, fileName);
					byte[] ackBuffer = ByteConverter.objectToBytes(ackPacket);
					DatagramPacket ackdp = new DatagramPacket(ackBuffer, ackBuffer.length, clientInetAddress, clientPort);
					socket.send(ackdp);
					//System.out.println("ACK(rwnd): " + (expectedseqnum-1) + "——————expect: " + expectedseqnum);
				}
				// 接收到期望收到的数据包
				else if(packet.getSeq() == expectedseqnum) {
					// 提取数据包，递交数据
					data.add(packet.getData());
					// 期待下一个序列号的数据包
					expectedseqnum++;
					// 接受空闲窗口减少1
					rwnd--;
					//System.out.println("接收片段：" + packet.getSeq());
					// 返回一个正确接受的ACK包
					Packet ackPacket = new Packet(expectedseqnum, -1, true, false, rwnd, null, fileName);
					byte[] ackBuffer = ByteConverter.objectToBytes(ackPacket);
					DatagramPacket ackdp = new DatagramPacket(ackBuffer, ackBuffer.length, clientInetAddress, clientPort);
					socket.send(ackdp);
					//System.out.println("ACK(right): " + (expectedseqnum-1) + "——————expect: " + expectedseqnum + "——————get: " + packet.getSeq());
				}
				// 接受到非期望数据包
				else {
					// 返回一个错误接受的ACK包
					Packet ackPacket = new Packet(expectedseqnum-1, -1, true, false, rwnd, null, fileName);
					byte[] ackBuffer = ByteConverter.objectToBytes(ackPacket);
					DatagramPacket ackdp = new DatagramPacket(ackBuffer, ackBuffer.length, clientInetAddress, clientPort);
					socket.send(ackdp);
					//System.out.println("ACK(wrong): " + (expectedseqnum-1) + "——————expect: " + expectedseqnum + "——————get: " + packet.getSeq());
				}
				// 阻塞等待下一个数据包
				socket.receive(dp);
			}
			String dirString = dir + fileName;
			FileIO.byte2file(dirString, data);
			System.out.println("接收并写入完毕！");
		}
		catch (SocketException e) {
			System.out.println("ReceiveThread: 创建socket出错");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("ReceiveThread: 接收数据包出错");
			e.printStackTrace();
		}
	}
	

}
