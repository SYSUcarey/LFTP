package tools;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Random;

public class Percentage {
	
	
	// fileSizeΪ�ļ���С(kb)��Ҳ��Ҫ���͵����ݰ������seq(1kbһ�����ݰ�)
	// dateΪ�����ļ����俪ʼ��Dateʱ�䣬Ҳ�����ͷ�����seq=0��ʱ��
	// ackNumΪ��ǰ�����Ѿ���ɵ����ݰ����seq(zero-base)
	public static void showPercentage(int fileSize, Date date, int ackNum) {
		// TODO:չʾ���Ȱٷֱ�
		float percentage = 0;
		if(ackNum>=0)
			percentage = (float)(ackNum+1) * 100 / (float)fileSize;
		if(percentage > 100) percentage = 100;
		// �����ٷֱȽ���Ϊ��λС��
		DecimalFormat decimalFormat = new DecimalFormat("0.00");//���췽�����ַ���ʽ�������С������2λ,����0����.
		String p = decimalFormat.format(percentage);//format ���ص����ַ���
		System.out.print("\r");
		System.out.print(p + "%\t");
		
		// TODO��չʾ���ȶ���
		StringBuilder percentageAnimationMessage = new StringBuilder();
		percentageAnimationMessage.append("[");
		for(int j = 0; j < 100; j++) {
			if(j < (int)percentage)
				percentageAnimationMessage.append("=");
			if(j == (int)percentage)
				percentageAnimationMessage.append(">");
			else if(j > (int)percentage)
				percentageAnimationMessage.append(" ");
		}
		percentageAnimationMessage.append("]\t");
		System.out.print(percentageAnimationMessage.toString());
		
		// TODO��չʾƽ���ٶ�
		int speed = getAverageSpeed(fileSize, date, ackNum);
		String speedMessage = speed + "kb/s";
		while(speedMessage.length()<16) speedMessage+=" ";//��ȫ����
		System.out.print(speedMessage);
		
		// TODO�� չʾʣ��ʱ��
		String remainTime = timeTransform(getRemainTime(fileSize, date, ackNum));
		System.out.print(remainTime);
		if(percentage >= 100) System.out.println();
	}
	
	// ����ƽ���ٶȼ���ʣ����Ҫ��ʱ��
	public static int getRemainTime(int fileSize, Date date, int ackNum) {
		int speed = getAverageSpeed(fileSize, date, ackNum);
		if(speed == 0) return 24*60*60+1;
		return (fileSize-ackNum)/speed;
	}
	
	// ��ô��ļ����俪ʼ�����µ�ƽ���ٶ�
	public static int getAverageSpeed(int fileSize, Date date, int ackNum) {
		long startTime = date.getTime();
		long nowTime = new Date().getTime();
		long after = nowTime - startTime;
		if(after <= 0) return 0;
		// һ������û����
		if(ackNum < 0) return 0;
		return (int)((ackNum+1) * 1000 / after);
	}
	
	// ʱ��ת����������(int)ת�����ַ������
	public static String timeTransform(int secondNum) {
		if (secondNum > 24*60*60) return "More than A day";
		int hour = secondNum / (60*60);
		int min = secondNum / 60 - hour * 60;
		int second = secondNum % 60;
		String resultString;
		if(min == 0) resultString = second+"s";
		else if(hour == 0) resultString =  min+"m"+second+"s";
		else resultString = hour+"h"+min+"m"+second+"s";
		while (resultString.length() < 9) resultString += " ";//��ȫ����
		return resultString;
		
	}
	
	
}
