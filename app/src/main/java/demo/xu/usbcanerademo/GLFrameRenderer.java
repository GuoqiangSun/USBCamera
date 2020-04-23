package demo.xu.usbcanerademo;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * Created by 601042 on 2016/7/29.
 */
public class GLFrameRenderer implements GLSurfaceView.Renderer, Runnable {

    private final static String TAG = "GLFrameRenderer";

    NativeTest test;

    private ISimplePlayer mParentAct; //请无视之
    private GLSurfaceView mTargetSurface;
    private GLProgram prog = new GLProgram(0);
    private int mVideoWidth = -1, mVideoHeight = -1;
    private ByteBuffer y;
    private ByteBuffer u;
    private ByteBuffer v;
    private ByteBuffer bitmapbuffer;
    private Context context;


    public GLFrameRenderer(Context context, ISimplePlayer callback, GLSurfaceView surface) {
        mParentAct = callback; //请无视之
        mTargetSurface = surface;
        this.context = context;
        int i = mPixel.length;
        for (i = 0; i < mPixel.length; i++) {
            mPixel[i] = (byte) 0x00;
        }
    }

    public void setTest(NativeTest test) {
        this.test = test;
    }

    private volatile Thread runner = null;

    public void PlayVideo() {

        if (runner == null) {
            runner = new Thread(this);
//            runner.start0();
        }
    }

    public void stopVideo() {

        if (runner != null) {
            Thread moribund = runner;
            runner = null;
            moribund.interrupt();


        }
    }

    Line line = null;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "GLFrameRenderer :: onSurfaceCreated");
        if (!prog.isProgramBuilt()) {
            prog.buildProgram();
            Log.d(TAG, "GLFrameRenderer :: buildProgram done");
            PlayVideo();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "GLFrameRenderer :: onSurfaceChanged");
        GLES20.glViewport(0, 0, width, height);
    }

    // 画线的坐标
    float vertexArray2[] = {
            -0.8f, -0.4f * 1.732f, 0.0f,
            -0.4f, 0.4f * 1.732f, 0.0f,
            0.0f, -0.4f * 1.732f, 0.0f,
            0.4f, 0.4f * 1.732f, 0.0f,};
    // 画点的坐标
    float[] vertexArray = new float[]{
            -0.8f, -0.4f * 1.732f, 0.0f,
            0.8f, -0.4f * 1.732f, 0.0f,
            0.0f, 0.4f * 1.732f, 0.0f,};

    // 画线
    public void DrawScene(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertexArray2.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        FloatBuffer vertex = vbb.asFloatBuffer();
        vertex.put(vertexArray);
        vertex.position(0);

        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -4);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertex);

        int index = new Random().nextInt(4);
        switch (index) {

            case 1:
                gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
                gl.glDrawArrays(GL10.GL_LINES, 0, 4);
                break;
            case 2:
                gl.glColor4f(0.0f, 1.0f, 0.0f, 1.0f);
                gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, 4);
                break;
            case 3:
                gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
                gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, 4);
                break;
        }

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (y != null) {
                // reset position, have to be done
                y.position(0);
                u.position(0);
                v.position(0);
                prog.buildTextures(y, u, v, mVideoWidth, mVideoHeight);
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                prog.drawFrame();
//                  DrawScene(gl);


            }
        }
    }

    /**
     * this method will be called from native code, it happens when the video is about to play or
     * the video size changes.
     */
    public void update(int w, int h) {
        Log.d(TAG, "INIT E");
        if (w > 0 && h > 0) {
            if (w != mVideoWidth && h != mVideoHeight) {
                this.mVideoWidth = w;
                this.mVideoHeight = h;
                int yarraySize = w * h;
                int uvarraySize = yarraySize / 4;
                synchronized (this) {
                    y = ByteBuffer.allocate(yarraySize);
                    u = ByteBuffer.allocate(uvarraySize);
                    v = ByteBuffer.allocate(uvarraySize);
                }
            }
        }

        mParentAct.onPlayStart(); //请无视之
        Log.d(TAG, "INIT X");
    }

    /**
     * this method will be called from native code, it's used for passing yuv data to me.
     */
    public synchronized void update(byte[] ydata, byte[] udata, byte[] vdata) {
        synchronized (this) {
            y.put(mPixel, 0, width * height);
            u.put(mPixel, width * height, (width * height) / 4);
            v.put(mPixel, (width * height) + ((width * height) / 4), (width * height) / 4);
//            y.put(ydata, 0, ydata.length);
//            u.put(udata, 0, udata.length);
//            v.put(vdata, 0, vdata.length);
        }

        // request to render
        mTargetSurface.requestRender();
    }

    Handler myhandler = new Handler();
    Runnable drawrunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("video_client.c", "开始扔出一帧图像在画");
            byte[] ydata = Arrays.copyOfRange(mPixel, 0, width * height);
            byte[] udata = Arrays.copyOfRange(mPixel, width * height, (width * height) + (width * height / 4));
            byte[] vdata = Arrays.copyOfRange(mPixel, (width * height) + (width * height / 4), (width * height) + (width * height / 2));
            ml++;
        }
    };

    private int dest = 0;

    public void hasData(byte[] data, int length) {
//        mPixel = data;
//        mPixel = Arrays.copyOfRange(data, 0, length);
        int remain = pxLength - dest;
        if (length > remain) {
            length = remain;
        }
        System.arraycopy(data, 0, mPixel, dest, length);
        dest += length;
        if (dest >= pxLength) {
            dest = 0;
        }

//        UpdateTask task = new UpdateTask();
//        task.execute(mPixel);
    }

    private final int pxLength = width * height * 2;
    /**
     * 图像数据
     */
    byte[] mPixel = new byte[pxLength];
    int ml = 0;
    private boolean isGetPic = false;

    @Override
    public void run() {
        try {
            int iTemp = 0;
            int bytesRead = 0;
            byte[] SockBuf = new byte[2048 * 16 * 4];


            update(width, height);

            while (!Thread.currentThread().isInterrupted()) {
//                byte[] ydata = Arrays.copyOfRange(mPixel, 0, width*height);
//                byte[]  udata = Arrays.copyOfRange(mPixel, width*height,(width*height)+(width*height/4));
//                byte[]  vdata= Arrays.copyOfRange(mPixel, (width*height)+(width*height/2), (width*height)+(width*height/2));
                update(null, null, null);

//                byte[] ydata = new byte[width*height];
//                byte[] udata = new byte[width*height/2];
//                byte[] vdata = new byte[width*height/2];
//
//                int yi = 0,ui = 0,vi = 0;
//                for(int i = 0;i<mPixel.length;i++){
//                    switch (i%4){
//                        case 0:
//                            //y
//                            ydata[yi] = mPixel[i];
//                            yi++;
//                            break;
//                        case 1:
//                            //u
//                            udata[ui] = mPixel[i];
//                            ui++;
//                            break;
//                        case 2:
//                            //y
//                            ydata[yi] = mPixel[i];
//                            yi++;
//                            break;
//                        case 3:
//                            //v
//                            vdata[vi] = mPixel[i];
//                            vi++;
//                            break;
//                    }
//                }
//                update(ydata,udata,vdata);


                ml++;
                Thread.sleep(55);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class UpdateTask extends AsyncTask<byte[], Void, String> {
        @Override
        protected void onPreExecute() {
//            byte[] ydata = Arrays.copyOfRange(mPixel, 0, width*height);
//            byte[]  udata = Arrays.copyOfRange(mPixel, width*height,(width*height)+(width*height/4));
//            byte[]  vdata= Arrays.copyOfRange(mPixel, (width*height)+(width*height/2), (width*height)+(width*height/2));


            byte[] udata = new byte[width * height / 2];
            byte[] vdata = new byte[width * height / 2];
            byte[] ydata = Arrays.copyOfRange(mPixel, 0, width * height);
            byte[] temp = Arrays.copyOfRange(mPixel, width * height, width * height * 2);
            int yi = 0, ui = 0, vi = 0;
            for (int i = 0; i < temp.length; i++) {

                switch (i % 2) {
                    case 0:
                        //u
                        udata[ui] = temp[i];
                        ui++;
                        break;
                    case 1:
                        //v
                        vdata[vi] = temp[i];
                        vi++;
                        break;
                }
            }


            update(ydata, udata, vdata);
            ml++;
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(byte[]... params) {
            //在这里开始画图片
//            Log.d("video_client.c", "开始扔出一帧图像在画");
//            byte[] ydata = Arrays.copyOfRange(mPixel, 0, width*height);
//            byte[]  udata = Arrays.copyOfRange(mPixel, width*height,(width*height)+(width*height/4));
//            byte[]  vdata= Arrays.copyOfRange(mPixel, (width*height)+(width*height/4), (width*height)+(width*height/2));
//            update(ydata,udata,vdata);
//            ml++;

            return null;
        }
    }


    public int getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }


    /**
     * 视频分辨率
     */
    private int screenWidth;
    private int screenHeight;
    public static int width = 640;
    public static int height = 480;


}