/// ��ΪJVM���ڴ�����ƣ��������һ��ֻ�ܶ�д300-MB���ļ�

package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class FileIO {
	public static List<byte[]> file2byte(String path) {
        try {
            FileInputStream inStream =new FileInputStream(new File(path));
            List<byte[]> datas = new ArrayList<>();
            final int MAX_BYTE = 1024;	//ÿ��byte[]������,��ǰ1Kb
            long streamTotal = 0;  //������������
            int streamNum = 0;  //����Ҫ�ֿ�������
            int leave = 0;  //�ļ�ʣ�µ��ַ���
            // ����ļ�������������
            streamTotal = inStream.available();
            // ������ļ���Ҫ�ֿ�������
            streamNum = (int)Math.floor(streamTotal/MAX_BYTE);
            // ��÷ֿ��ɶ���ǳ����ļ������ʣ�������С
            if(streamNum > 0)
            	leave = (int)streamTotal/streamNum;
            else leave = (int)streamTotal;
            if(streamNum > 0) {
            	for(int i = 0; i < streamNum; i++) {
            		byte[] data;
            		data = new byte[MAX_BYTE];
            		inStream.read(data, 0, MAX_BYTE);
            		datas.add(data);
            	}
            }
            // �������ʣ��Ĳ����ַ�
            byte[] data = new byte[leave];
            inStream.read(data, 0, leave);
            datas.add(data);
            inStream.close();
            System.out.println("��ȡ�ļ����,�� " + streamNum + "��");
            return datas;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void byte2file(String path,List<byte[]> datas) {
        try {
            FileOutputStream outputStream  =new FileOutputStream(new File(path));
            for(int i = 0; i < datas.size(); i++) {
            	outputStream.write(datas.get(i));
            	outputStream.flush();
        		//System.out.println("д���ļ�Ƭ��" + i);
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static int getFileSize(String path) {
    	return 0;
    }
    
    public static int getBufferLength(String path) {
    	return 0;
    }
    

    public static void main(String[] args) {
        List<byte[]> datas=file2byte("test.txt");   
        System.out.println("readFile succeed!");
        byte2file("output.txt",datas);
        System.out.println("saveFile succeed!");
        
    }
}
