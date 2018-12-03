package main;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Locale;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

class LFTP {
    @Parameter(names={"--clientport", "-cp"})
    static String cport = "6000";
    @Parameter(names={"--serverport", "-sp"})
    static String sport = "7545";
    @Parameter(names={"--server", "-s"})
    static String serverAddress = "127.0.0.1";
    @Parameter(names= {"--filename", "-f"})
    static String fileName;
    static Client client;

    public static void main(String[] argv) {
        LFTP main = new LFTP();
        JCommander jCommander = new JCommander();
        jCommander.addObject(main);
        String[] tmp = new String[argv.length - 1];
        for (int i = 1; i < argv.length; ++i) {
        	tmp[i - 1] = argv[i].toLowerCase();
        }
        jCommander.parse(tmp);
        argv[0] = argv[0].toLowerCase(Locale.ROOT);
        switch (argv[0]) {
			case "server":
				System.out.println("[cmd]����������");
				Server server = new Server(Integer.parseInt(sport));
				System.out.println("[cmd]��������Ϣ�����˿�: " + sport);
				try {
					server.run();
				} catch (SocketException e) {
					System.err.println(e.getMessage());
				}
				break;
			case "lget":
				System.out.println("[cmd]�ӷ����������ļ�");
				System.out.println("[cmd]�ͻ��˽��ն˿�: " + cport);
				System.out.println("[cmd]�����������˿�: " + sport);
				System.out.println("[cmd]��������ַ: " + serverAddress);
				if (fileName == null) {
					System.err.println("[ERROR]�ļ�������Ϊ��");
					System.exit(0);
				}
				System.out.println("[cmd]�ļ���: " + fileName);
				
				createClient(argv[0]);
				
				break;
			case "lsend":
				System.out.println("[cmd]������������ļ�");
				System.out.println("[cmd]�ͻ��˷��Ͷ˿�: " + cport);
				System.out.println("[cmd]�����������˿�: " + sport);
				System.out.println("[cmd]��������ַ: " + serverAddress);
				if (fileName == null) {
					System.err.println("[ERROR]�ļ�������Ϊ��");
					System.exit(0);
				}
				System.out.println("[cmd]�ļ���: " + fileName);
				
				createClient(argv[0]);
				
				break;
			case "listall":
				System.out.println("[cmd]�г�������Ŀ¼�������ļ�");				
				System.out.println("[cmd]�ͻ��˶˿�: " + cport);				
				System.out.println("[cmd]�������˿�: " + sport);				
				System.out.println("[cmd]��������ַ: " + serverAddress);
				
				createClient(argv[0]);
				
				break;
			default:
				System.out.println("[cmd]��������: " + argv[0]);
				break;
		}
    }
    
    private static void createClient(String operation) {
    	InetAddress address = null;
		try {
			address = InetAddress.getByName(serverAddress);
		} catch (UnknownHostException e) {
			System.err.println("������������ַ����");
		}
		if (address == null) System.exit(0);
		client = new Client(Integer.parseInt(cport), operation, fileName, address, Integer.parseInt(sport));
		client.run();
    }
}