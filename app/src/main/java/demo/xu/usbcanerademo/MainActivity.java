package demo.xu.usbcanerademo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.pointsmart.imiusbcamera.R;

public class MainActivity extends AppCompatActivity {
    private String TAG = "TestActivity";


    GLSurfaceView surfaceview;
    private GLFrameRenderer render;
    // Used to load the 'native-lib' library on application startup.

    NativeTest NativeTest0 = new NativeTest();
    boolean start0 = false;

    NativeTest NativeTest1 = new NativeTest();
    boolean start1 = false;

    NativeTest NativeTest2 = new NativeTest();
    boolean start2 = false;

    NativeTest NativeTest3 = new NativeTest();
    boolean start3 = false;

    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        tv = (TextView) findViewById(R.id.sample_text);
        tv.setText("");
        surfaceview = (GLSurfaceView) findViewById(R.id.surfaceview);
        render = new GLFrameRenderer(this, new ISimplePlayer() {
            @Override
            public void onPlayStart() {

            }

            @Override
            public void onReceiveState(int state) {

            }
        }, surfaceview);
        render.setTest(NativeTest0);
        surfaceview.setEGLContextClientVersion(2);
        surfaceview.setRenderer(render);


//        test_view = (XuSurfaceView) findViewById(R.id.test_view);
//        test_view.setScreenWidth((int)(screenWidth));
//        test_view.setScreenHeight((int)(screenHeight));


//        view_2.setScreenWidth(screenWidth);
//        view_2.setScreenHeight(screenHeight);

//        view_3.setScreenWidth(screenWidth);
//        view_3.setScreenHeight(screenHeight);
        render.setScreenWidth(720);
        render.setScreenHeight(1280);

        render.PlayVideo();

        NativeTest0.setRender(render);


//        XuAPNUtils apn=new XuAPNUtils();
//        apn.checkAPN(TestActivity.this);
//        if (!XuAPNUtils.hasAPN) {
//            try {
//                apn.SetAPN(TestActivity.this,apn.addAPN(TestActivity.this));
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//
//        }

        final ImageView mImiImg = findViewById(R.id.img_imi);
        final ImageView mImiImgDepth = findViewById(R.id.img_imi_depth);

        mUIhandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 0) {
                    mImiImg.setImageBitmap((Bitmap) msg.obj);
                    show = false;
                } else if (msg.what == 2) {
                    mImiImgDepth.setImageBitmap((Bitmap) msg.obj);
                    show2 = false;
                }
            }
        };

    }

    private Handler mUIhandler;

    @Override
    protected void onDestroy() {
        render.stopVideo();
        NativeTest0.closeCamera();
        NativeTest1.closeCamera();
        NativeTest2.closeCamera();
        NativeTest3.closeCamera();
        super.onDestroy();

    }

    private boolean show;
    private boolean show2;

    private String dev0 = "dev/video4";
    private String dev1 = "dev/video5";
    private String dev2 = "dev/video6";
    private String dev3 = "dev/video7";


    public void closeVideo0(View view) {
        synchronized (MainActivity.this) {
            if (!start0) {
                Toast.makeText(MainActivity.this, "already stop0 ", Toast.LENGTH_LONG).show();
                return;
            }
            start0 = false;
        }
        NativeTest0.closeCamera();
    }

    public synchronized void openVideo0(View view) {

        synchronized (MainActivity.this) {
            if (start0) {
                Toast.makeText(MainActivity.this, "already start0 ", Toast.LENGTH_LONG).show();
                return;
            }
            start0 = true;
        }

        NativeTest0.regDataCallBack(new NativeTest.DataCallBack() {
            @Override
            public void onCallback(byte[] data, int length) {

                Log.e(TAG, "video0 Callback: " + data.length + "   " + length);

                if (!show) {
                    show = true;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, length);
                    mUIhandler.obtainMessage(0, bitmap).sendToTarget();
                }
            }
        });

        int result_openCamera = NativeTest0.openCamera(dev0);
        String conect = "openCamera fd: " + result_openCamera + "\n";
        if (result_openCamera != -1) {
            conect += NativeTest0.getDevicInfo();
            conect += "\n getCache:" + NativeTest0.getCache();
            conect += "\n startCapture:" + NativeTest0.startCapture();
        }
        tv.append("openVideo0\n");
        tv.append(conect);
        tv.append("\n");
        tv.append("\n");
        tv.append("\n");

        NativeTest0.start();
        int oneFrame = NativeTest0.getOneFrame();
        Toast.makeText(MainActivity.this, "getOneFrame:" + oneFrame, Toast.LENGTH_LONG).show();

    }

    public void openVideo1(View view) {
        synchronized (MainActivity.this) {
            if (start1) {
                Toast.makeText(MainActivity.this, "already start1 ", Toast.LENGTH_LONG).show();
                return;
            }
            start1 = true;
        }

        NativeTest1.regDataCallBack(new NativeTest.DataCallBack() {
            @Override
            public void onCallback(byte[] data, int length) {
            }
        });

        int result_openCamera = NativeTest1.openCamera(dev1);
        String conect = "openCamera fd: " + result_openCamera + "\n";
        if (result_openCamera != -1) {
            conect += NativeTest1.getDevicInfo();
            conect += "\n getCache:" + NativeTest1.getCache();
            conect += "\n startCapture:" + NativeTest1.startCapture();
        }
        tv.append("openVideo1\n");
        tv.append(conect);
        tv.append("\n");
        tv.append("\n");
        tv.append("\n");
        NativeTest1.start();
        int oneFrame = NativeTest1.getOneFrame();
        Toast.makeText(MainActivity.this, "getOneFrame:" + oneFrame, Toast.LENGTH_LONG).show();
    }

    public void openVideo2(View view) {
        synchronized (MainActivity.this) {
            if (start2) {
                Toast.makeText(MainActivity.this, "already start2 ", Toast.LENGTH_LONG).show();
                return;
            }
            start2 = true;
        }

        NativeTest2.regDataCallBack(new NativeTest.DataCallBack() {
            @Override
            public void onCallback(byte[] data, int length) {
                if (!show2) {
                    show2 = true;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, length);
                    mUIhandler.obtainMessage(2, bitmap).sendToTarget();
                }
            }
        });

        int result_openCamera = NativeTest2.openCamera(dev2);
        String conect = "openCamera fd: " + result_openCamera + "\n";
        if (result_openCamera != -1) {
            conect += NativeTest2.getDevicInfo();
            conect += "\n getCache:" + NativeTest2.getCache();
            conect += "\n startCapture:" + NativeTest2.startCapture();
        }
        tv.append("openVideo2\n");
        tv.append(conect);
        tv.append("\n");
        tv.append("\n");
        tv.append("\n");
        NativeTest2.start();
        int oneFrame = NativeTest2.getOneFrame();
        Toast.makeText(MainActivity.this, "getOneFrame:" + oneFrame, Toast.LENGTH_LONG).show();
    }

    public void openVideo3(View view) {
        synchronized (MainActivity.this) {
            if (start3) {
                Toast.makeText(MainActivity.this, "already start3 ", Toast.LENGTH_LONG).show();
                return;
            }
            start3 = true;
        }

        NativeTest3.regDataCallBack(new NativeTest.DataCallBack() {
            @Override
            public void onCallback(byte[] data, int length) {
            }
        });

        int result_openCamera = NativeTest3.openCamera(dev3);
        String conect = "openCamera fd: " + result_openCamera + "\n";
        if (result_openCamera != -1) {
            conect += NativeTest3.getDevicInfo();
            conect += "\n getCache:" + NativeTest3.getCache();
            conect += "\n startCapture:" + NativeTest3.startCapture();
        }
        tv.append("openVideo3\n");
        tv.append(conect);
        tv.append("\n");
        tv.append("\n");
        tv.append("\n");
        NativeTest3.start();
        int oneFrame = NativeTest3.getOneFrame();
        Toast.makeText(MainActivity.this, "getOneFrame:" + oneFrame, Toast.LENGTH_LONG).show();
    }


    public void closeVideo1(View view) {
        synchronized (MainActivity.this) {
            if (!start1) {
                Toast.makeText(MainActivity.this, "already stop1 ", Toast.LENGTH_LONG).show();
                return;
            }
            start1 = false;
        }
        NativeTest1.closeCamera();
    }

    public void closeVideo2(View view) {
        synchronized (MainActivity.this) {
            if (!start2) {
                Toast.makeText(MainActivity.this, "already stop2 ", Toast.LENGTH_LONG).show();
                return;
            }
            start2 = false;
        }
        NativeTest2.closeCamera();
    }

    public void closeVideo3(View view) {
        synchronized (MainActivity.this) {
            if (!start3) {
                Toast.makeText(MainActivity.this, "already stop3 ", Toast.LENGTH_LONG).show();
                return;
            }
            start3 = false;
        }
        NativeTest3.closeCamera();
    }
}
