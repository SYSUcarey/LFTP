package main;

import java.io.File;

import service.ReceiveThread;

public class Server {
	public static void main(String[] argv) {
		int serverPort = 3888;
		Thread receiveThread = new Thread(new ReceiveThread(serverPort));
		receiveThread.start();
		System.out.println("�ļ����ܶ˿ڣ�" + serverPort);
		String dirString = "server";
		File file = new File(dirString);
		if(!file.exists()) {
			file.mkdir();
		}
	}
}
