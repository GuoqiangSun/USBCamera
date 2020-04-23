package demo.xu.usbcanerademo;



import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Created by shenglr on 16/1/22.
 */
public class CodeUtil {
    private static final String TAG = "CodeUtil";
    public static final byte[] generateCrc16(byte[] byteArray, int intLength) {
        int crc = 0xFFFF;
        for (int i = 0; i < intLength; i++) {
            crc ^= (byteArray[i] & 0xFF);
            crc = crc & 0xFFFF;
            // System.out.println(crc);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0x8408;
                } else {
                    crc = (crc >> 1);
                }
                crc = crc & 0xFFFF;
                // System.out.println(crc);
            }
        }
        // System.out.println("crc=" + crc);
        return intToByteArray(crc);
    }

    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[]{(byte) ((a >> 24) & 0xFF), (byte) ((a >> 16) & 0xFF), (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)};
    }

    // 涓や釜瀛楄妭琛ㄧず鐨勬渶澶ф暟
    public static int MAX_2_BYTE_SIZE = 65535;

    public static byte[] intToBb(int value) {

        byte high = (byte) ((value & 0xff00) >> 8);
        byte low = (byte) (value & 0x00ff);

        return new byte[]{high, low};

    }

    public static byte[] intToBbLH(int value) {

        byte high = (byte) ((value & 0xff00) >> 8);
        byte low = (byte) (value & 0x00ff);

        return new byte[]{low,high};

    }
    public static int oneByteToInt(byte b) {
        return (b & 0xFF);
    }
    public static byte intToOneByte(int num) {
        return (byte) (num & 0x000000ff);
    }
    public static int bbToInt(byte[] b) {
        int result = ((b[0] << 8) & 0xff00) | (b[1] & 0xff);
        return result;
    }
    public static short byteToshort(byte[] b){
        return (short)(((b[0] & 0x00FF) << 8) | (0x00FF & b[1]));
    }

    public static byte[] intTobb(int value){
        byte[] buf = new byte[]{00,00};
        ByteArrayOutputStream boutput = new ByteArrayOutputStream();
        DataOutputStream doutput = new DataOutputStream(boutput);
        try {
            doutput.writeInt(value);
            buf = boutput.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            boutput.close();
            doutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buf;
    }
    public static int bbToIntLH(byte[] b) {
        int result = ((b[1] << 8) & 0xff00) | (b[0] & 0xff);
        return result;
    }

    public static int bbbbToint(byte[] buf) {
        return buf[3] & 0xFF | (buf[2] & 0xFF) << 8 | (buf[1] & 0xFF) << 16 | (buf[0] & 0xFF) << 24;
    }

    public static int getUnsignedintbyBB(byte[] b){
        short data = (short)(((b[0] & 0x00FF) << 8) | (0x00FF & b[1]));
        return data&0x0FFFF ;
    }

    public static int getUnsignedintbyB(byte b){
        return b&0xff ;
    }




    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }
    private static ByteBuffer buffer = ByteBuffer.allocate(8);

    public static byte[] longToBytes(long x) {
        buffer.putLong(0, x);
        return buffer.array();
    }
    public static long bytesToLong(byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        long temp = buffer.getLong();
        buffer.clear();
        return temp;
    }
    public static byte[] stringToByte(String str){
        byte[] data = new byte[str.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = Integer.valueOf(str.substring(0 + i * 2, 2 + i * 2), 16).byteValue();
        }
        return data;
    }
    public static byte[] toByteArray(int iSource, int iArrayLen) {
        byte[] bLocalArr = new byte[iArrayLen];
        for (int i = 0; (i < 4) && (i < iArrayLen); i++) {
            bLocalArr[i] = (byte) (iSource >> 8 * i & 0xFF);
        }
        return bLocalArr;
    }


    public static String bytesToString(byte[] b){
        StringBuffer sb = new StringBuffer();
        for(byte temp : b){
            sb.append(String.format("%02x",temp));
        }
        return sb.toString();
    }

    public static String toBitStr(int b){
        String temp = Integer.toBinaryString(b);
        StringBuffer sb = new StringBuffer();
        for(int i = 0 ;i < 8-temp.length(); i++){
            sb.append("0");
        }
        sb.append(temp);
        String bitstr = sb.reverse().toString();
        return bitstr;
    }

    public static String EBToBitStr(byte[] bs){
        StringBuffer value = new StringBuffer();
        for(byte temp : bs){
            value.append(toBitStr(temp));
        }
        return value.toString();
    }
    public static String ascii2String(byte[] ASCIIs) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ASCIIs.length; i++) {
            sb.append((char) ascii2Char(ASCIIs[i]));
        }
        return sb.toString();
    }
    public static char ascii2Char(int ASCII) {
        return (char) ASCII;
    }

}
