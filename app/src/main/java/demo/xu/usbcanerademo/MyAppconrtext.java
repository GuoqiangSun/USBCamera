package demo.xu.usbcanerademo;

import android.app.Application;

/**
 * Created by 601042 on 2018/2/23.
 */

public class MyAppconrtext extends Application {
    private final static String TAG = "MyAppconrtext";
    /**
     * Application单例
     */
    private static MyAppconrtext sInstance;
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        // 注册全局异常捕获者
        initCrashHandler();
    }
    private void initCrashHandler(){
        //TODO  初始化全局的异常捕获者
        XuCrashHandler crashHandler = XuCrashHandler.getInstance();
        crashHandler.init(this);
        // 发送以前没发送的报告(可选)
        //crashHandler.sendPreviousReportsToServer();
    }
    public static MyAppconrtext getsInstance() {
        return sInstance;
    }




}
