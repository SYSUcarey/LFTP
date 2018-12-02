/// ��ΪJVM���ڴ�����ƣ��������һ��ֻ�ܶ�д300-MB���ļ�

package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FileIO {
	
	// ��ȡ�����ļ���List<byte[]>
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
            // ������ļ���Ҫ�ֿ�����1kb����������
            streamNum = (int)Math.floor(streamTotal/MAX_BYTE);
            // ��÷ֿ��ɶ���ǳ����ļ������ʣ�������С
            leave = (int)streamTotal%MAX_BYTE;
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
	
	// �������ȡ�ļ���List<byte[]>��(�����������Ϊ10Mb)
	public static List<byte[]> file2bList(String path, int blockNum) {
		try {
            FileInputStream inStream =new FileInputStream(new File(path));
            List<byte[]> datas = new ArrayList<>();	//���ص�������Ϣ
            final int MAX_BYTE = 1024;	//������byte[]�Ĵ�С
            final int BLOCK_SIZE = 1024*1024*10;	//����������Ĵ�С
            long streamTotal = 0;	//������������
            int bytesTotal = 0;  	//���ֳ���1kb��byte[]������
            int blockTotal = 0;	//���ֳ���10Mb����block������
            int leave = 0;  //�ļ�ʣ�µ��ַ���
            // ����ļ�������������
            streamTotal = inStream.available();
            // ������ļ���Ҫ�ֿ�����1kb byte[]������
            bytesTotal = (int)Math.floor(streamTotal/MAX_BYTE);
            // ������ļ��ֿ���10Mb���������
            blockTotal = (int)Math.floor(streamTotal/BLOCK_SIZE);
            // ��÷ֿ��ɶ���ǳ����ļ������ʣ�������С
            leave = (int)streamTotal%MAX_BYTE;
            
            // ��������Ų�ƥ��
            if(blockNum < 0 || blockNum > blockTotal) return null;
            
            // �������������10Mb��
            for(int i = 0; i < blockTotal; i++) {
            		for(int j = 0; j < BLOCK_SIZE/MAX_BYTE; j++) {
            			byte[] data = new byte[MAX_BYTE];
            			inStream.read(data, 0, MAX_BYTE);
            			if(i == blockNum) datas.add(data);
            		}
            }
            // ����������ǲ���10Mb��
            if(blockNum == blockTotal) {
            	for(int i = 0; i < bytesTotal - (BLOCK_SIZE/MAX_BYTE) * blockTotal; i++) {
            		byte[] data = new byte[MAX_BYTE];
            		inStream.read(data, 0, MAX_BYTE);
            		datas.add(data);
            	}
            	byte[] data = new byte[leave];
                inStream.read(data, 0, leave);
                datas.add(data);
            }
            inStream.close();
            System.out.println("��ȡ����" + blockNum + "���! �����С------" + datas.size());
            return datas;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
	}
	
	// List<byte[]> ���ݴ����ļ���(ֱ��׷������)
    public static void byte2file(String path,List<byte[]> datas) {
        try {
            FileOutputStream outputStream  =new FileOutputStream(new File(path), true);
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
    
    public static int getBufferLength(String path) {
    	try{
	    	FileInputStream inStream =new FileInputStream(new File(path));
	        List<byte[]> datas = new ArrayList<>();
	        final int MAX_BYTE = 1024;	//ÿ��byte[]������,��ǰ1Kb
	        long streamTotal = 0;  //������������
	        int streamNum = 0;  //����Ҫ�ֿ�������
	        int leave = 0;  //�ļ�ʣ�µ��ַ���
	        // ����ļ�������������
	        streamTotal = inStream.available();
	        // ������ļ���Ҫ�ֿ�����1kb����������
	        streamNum = (int)Math.floor(streamTotal/MAX_BYTE);
	        inStream.close();
	        return streamNum+1;
    	} catch (Exception e) {
    		e.printStackTrace();
    		return 0;
    	}
    }
    
    public static int getBlockLength(String path) {
    	try{
	    	FileInputStream inStream =new FileInputStream(new File(path));
	        List<byte[]> datas = new ArrayList<>();
	        final int BLOCK_SIZE = 1024*1024*10;	//ÿ��block���������,��ǰ10MB
	        long streamTotal = 0;  //������������
	        int blockNum = 0;  //����Ҫ�ֿ�����������
	        // ����ļ�������������
	        streamTotal = inStream.available();
	        // ������ļ���Ҫ�ֿ�����1kb����������
	        blockNum = (int)Math.floor(streamTotal/BLOCK_SIZE);
	        inStream.close();
	        return blockNum+1;
    	} catch (Exception e) {
    		e.printStackTrace();
    		return 0;
    	}
    }
    
    

    public static void main(String[] args) {
        List<byte[]> datas=file2byte("test.mp4");   
        System.out.println("readFile succeed!");
        System.out.println("Total: " + datas.size());
        
        System.out.println("Total: " + getBufferLength("test.mp4") + "kb.");
        System.out.println("BlockNum: " + getBlockLength("test.mp4"));
        for(int i = 0; i < getBlockLength("test.mp4"); i++) {
        	datas = file2bList("test.mp4", i);
        }
        //byte2file("output.rmvb",datas);
        //System.out.println("saveFile succeed!");
        //System.out.println("Total: " + getBufferLength("output.mp3") + "kb.");
        
        
    }
}
