/// ��ΪJVM���ڴ�����ƣ��������һ��ֻ�ܶ�д300-MB���ļ�

package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FileIO {
	public static int MAX_BYTE = 1024;//ÿ��byte[]������,��ǰ1Kb
	public static int BLOCK_SIZE = 1024*1024*10;//����������Ĵ�С,��ǰ10MB
	public static int BYTES_IN_BLOCK = BLOCK_SIZE / MAX_BYTE;	// һ������byte[]����Ŀ��Ŀǰ10240
	
	// ��ȡ�����ļ���List<byte[]>
	public static List<byte[]> file2byte(String path) {
        try {
            FileInputStream inStream =new FileInputStream(new File(path));
            List<byte[]> datas = new ArrayList<>();
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
        List<byte[]> datas = new ArrayList<>();	//���ص�������Ϣ
		try {
            FileInputStream inStream =new FileInputStream(new File(path));
            long streamTotal = 0;	//������������
            int bytesTotal = 0;  	//���ֳ���1kb��byte[]������
            int blockTotal = 0;	//���ֳ���10Mb����block������
            int leave = 0;  //�ļ�ʣ�µ��ַ���
            // ����ļ�������������
            streamTotal = (new File(path)).length();
            // ������ļ���Ҫ�ֿ�����1kb byte[]������
            bytesTotal = getBufferLength(path);
            // ������ļ��ֿ���10Mb���������
            blockTotal = getBlockLength(path);
            // ��÷ֿ��ɶ���ǳ����ļ������ʣ�������С
            leave = (int)(streamTotal%MAX_BYTE);
            
            // ��������Ų�ƥ��
            if(blockNum < 0 || blockNum > blockTotal) return datas;
            
            // �����ƥ�䣬��������������ǰ�������
            // jdk 9.0.1 skipԴ����bug
            for(int i = 0; i < blockNum; i++) {
            	long skip = inStream.skip(BLOCK_SIZE);
            	if(skip != BLOCK_SIZE) return datas;
            }
            
            // �������������10Mb��
            if(blockNum >= 0 && blockNum < blockTotal-1){
        		for(int j = 0; j < BLOCK_SIZE/MAX_BYTE; j++) {
        			byte[] data = new byte[MAX_BYTE];
        			inStream.read(data, 0, MAX_BYTE);
        			datas.add(data);
        		}
            }
            // ����������ǲ���10Mb��
            else {
            	for(int i = 0; i < bytesTotal - (BLOCK_SIZE/MAX_BYTE) * (blockTotal-1); i++) {
            		byte[] data = new byte[MAX_BYTE];
            		inStream.read(data, 0, MAX_BYTE);
            		datas.add(data);
            	}
            	//ʣ�಻��byte[MAX_BYTE]��
            	byte[] data = new byte[leave];
                inStream.read(data, 0, leave);
                datas.add(data);
            }
            inStream.close();
            System.out.println("��ȡ����" + blockNum + "���! �����С------" + datas.size() + "kb.");
            return datas;
        } catch (Exception e) {
            e.printStackTrace();
            return datas;
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
    
    // ����ļ�byte[MAX_BYTE]������
    public static int getBufferLength(String path) {
    	try{
    		// ��ȡ�ļ� �����ֽ���
    		File file = new File(path);
    		System.out.println(file.length());
    		long streamTotal =  file.length();
    		return (int)Math.floor(streamTotal/MAX_BYTE);
    	} catch (Exception e) {
    		e.printStackTrace();
    		return 0;
    	}
    }
    
    // ����ļ����Ի��ֵ�������
    public static int getBlockLength(String path) {
    	try{
    		// ��ȡ�ļ� �����ֽ���
    		File file = new File(path);
    		long streamTotal =  file.length();
	        // ������ļ���Ҫ�ֿ�����1kb����������
	        return (int)Math.floor(streamTotal/BLOCK_SIZE) + 1;
    	} catch (Exception e) {
    		e.printStackTrace();
    		return 0;
    	}
    }
    

    

    public static void main(String[] args) {
    	
    	
    	String path = "C:/Users/chenbb/Desktop/test.zip";
        System.out.println("Total: " + getBufferLength(path) + "kb.");
        System.out.println("BlockNum: " + getBlockLength(path));
        String dirString = "download";
		File file = new File(dirString);
		if(!file.exists()) {
			file.mkdir();
		}
		
		for(int i = 0; i < getBlockLength(path); i++) {
        	List<byte[]> datas = file2bList(path, i);
        	byte2file("download/test.zip", datas);
        	System.out.println("����" + i + "������ϣ�");
        }
        
        
        
    }
}
