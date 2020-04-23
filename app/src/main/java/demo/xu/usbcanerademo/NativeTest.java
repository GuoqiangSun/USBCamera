package demo.xu.usbcanerademo;

import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by 601042 on 2018/2/28.
 */

public class NativeTest {
    static {
        System.loadLibrary("native-lib");
    }


    /**
     * 打开USB镜头
     *
     * @return 0：成功
     */
    public native int openCamera(String devName);

    /**
     * 获取USB镜头的信息并设置相应的属性
     *
     * @return 0：成功
     */
    public native String getDevicInfo();

    /**
     * 申请一个图像数据的缓冲区
     *
     * @return 0：成功
     */
    public native int getCache();

    /**
     * 开始捕获数据
     *
     * @return 0：成功
     */
    public native int startCapture();

    /**
     * 获取一帧数据
     *
     * @return 0：成功
     */
    public native int getOneFrame();

    /**
     * 关闭USB镜头
     *
     * @return 0：成功
     */
    public native int closeCamera();


    /**
     * 开启图像callback
     *
     * @return 0：成功
     */
    public native void start();

    /**
     * 图像callback
     *
     * @param data：一帧图像数据
     * @param length：一帧图像数据的长度
     */
    public void myCallback(byte[] data, int length) {
        //给图像去显示
        if (mDataCallBack != null) {
            mDataCallBack.onCallback(data, length);
        }

    }

    public interface DataCallBack {
        void onCallback(byte[] data, int length);
    }

    private DataCallBack mDataCallBack;

    public void regDataCallBack(DataCallBack mDataCallBack) {
        this.mDataCallBack = mDataCallBack;
    }

    public void unregDataCallBack() {
        this.mDataCallBack = null;
    }

    volatile boolean save;

    private static GLFrameRenderer render;

    public void setRender(GLFrameRenderer render) {
        this.render = render;
    }
}
