#include <jni.h>

//#include <bits/stdc++.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include<pthread.h>
#include <getopt.h>             /* getopt_long() */
#include <fcntl.h>              /* low-level i/o */
#include <unistd.h>
#include <errno.h>
#include <malloc.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/mman.h>
#include <sys/ioctl.h>
#include <linux/time.h>
#include <asm/types.h>          /* for videodev2.h */
#include <linux/videodev2.h>
#include <sstream>
#include "ql_log.h"

#include <iostream>

using namespace std;

namespace android {
    struct buffer {
        void *start;
        size_t length;
    };
#ifndef NELEM
# define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
#endif

    unsigned int n_buffers = 4;        //缓存多少帧
    int fd = -1;                        //镜头ID
//    static char *dev_name = "/dev/video0";     //镜头名//
    struct buffer *buffers = NULL;              //图像数据
    void *framebuf = NULL;                      //一帧图像数据
    int oneFrameLength = 0;                     //一帧图像数据的长度
    FILE *outf = 0;                     //保存的文件

    pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;
    JavaVM *mjavaVM;
    jobject mjavaobject;
    bool canCallback = true;
    struct Testarg {
        jmethodID mID;
    };


    typedef long LONG;
    typedef unsigned long DWORD;
    typedef unsigned short WORD;

    typedef struct {
        WORD bfType;
        DWORD bfSize;
        WORD bfReserved1;
        WORD bfReserved2;
        DWORD bfOffBits;
    } BMPFILEHEADER_T;

    typedef struct {
        DWORD biSize;
        LONG biWidth;
        LONG biHeight;
        WORD biPlanes;
        WORD biBitCount;
        DWORD biCompression;
        DWORD biSizeImage;
        LONG biXPelsPerMeter;
        LONG biYPelsPerMeter;
        DWORD biClrUsed;
        DWORD biClrImportant;
    } BMPINFOHEADER_T;

    void savebmp(char *pdata, int width, int height) {      //分别为rgb数据，要保存的bmp文件名，图片长宽
        int size = width * height * 3 * sizeof(char); // 每个像素点3个字节
        // 位图第一部分，文件信息
        BMPFILEHEADER_T bfh;
        bfh.bfType = (WORD) 0x4d42;  //bm
        bfh.bfSize = size  // data size
                     + sizeof(BMPFILEHEADER_T) // first section size
                     + sizeof(BMPINFOHEADER_T) // second section size
                ;
        bfh.bfReserved1 = 0; // reserved
        bfh.bfReserved2 = 0; // reserved
        bfh.bfOffBits = sizeof(BMPFILEHEADER_T) + sizeof(BMPINFOHEADER_T);//真正的数据的位置

        // 位图第二部分，数据信息
        BMPINFOHEADER_T bih;
        bih.biSize = sizeof(BMPINFOHEADER_T);
        bih.biWidth = width;
        bih.biHeight = -height;//BMP图片从最后一个点开始扫描，显示时图片是倒着的，所以用-height，这样图片就正了
        bih.biPlanes = 1;//为1，不用改
        bih.biBitCount = 24;
        bih.biCompression = 0;//不压缩
        bih.biSizeImage = size;
        bih.biXPelsPerMeter = 2835;//像素每米
        bih.biYPelsPerMeter = 2835;
        bih.biClrUsed = 0;//已用过的颜色，24位的为0
        bih.biClrImportant = 0;//每个像素都重要
        FILE *fp = fopen("sdcard/test.bmp", "a");
        if (fp == NULL) {
            LOGD("fp is null");
        }
        if (!fp) return;


        fwrite(&bfh, 8, 1,
               fp);//由于linux上4字节对齐，而信息头大小为54字节，第一部分14字节，第二部分40字节，所以会将第一部分补齐为16自己，直接用sizeof，打开图片时就会遇到premature end-of-file encountered错误
        fwrite(&bfh.bfReserved2, sizeof(bfh.bfReserved2), 1, fp);
        fwrite(&bfh.bfOffBits, sizeof(bfh.bfOffBits), 1, fp);
        fwrite(&bih, sizeof(BMPINFOHEADER_T), 1, fp);
        fwrite(pdata, size, 1, fp);
        fclose(fp);
    }


/*************************************************
Function:       yuv422_to_rgb24
Description:    yuv422 转 rgb24
*************************************************/
    static void yuv422_to_rgb24(char *yuv422, char *rgb24, int width, int height) {
        int x, y;
        uint8_t *yuv444;
        yuv444 = (uint8_t *) malloc(sizeof(uint8_t) * width * height * 3);
        for (x = 0, y = 0; x < width * height * 2, y < width * height * 3; x += 4, y += 6) {
            yuv444[y] = yuv422[x];
            yuv444[y + 1] = yuv422[x + 1];
            yuv444[y + 2] = yuv422[x + 3];
            yuv444[y + 3] = yuv422[x + 2];
            yuv444[y + 4] = yuv422[x + 1];
            yuv444[y + 5] = yuv422[x + 3];
        }
        for (x = 0; x < width * height * 3; x += 3) {
            rgb24[x + 2] = yuv444[x] + 1.402 * (yuv444[x + 2] - 128);
            rgb24[x + 1] =
                    yuv444[x] - 0.34414 * (yuv444[x + 1] - 128) - 0.71414 * (yuv444[x + 2] - 128);
            rgb24[x] = yuv444[x] + 1.772 * (yuv444[x + 1] - 128);
            if (rgb24[x] > 255)rgb24[x] = 255;
            if (rgb24[x] < 0)rgb24[x] = 0;
            if (rgb24[x + 1] > 255)rgb24[x + 1] = 255;
            if (rgb24[x + 1] < 0)rgb24[x + 1] = 0;
            if (rgb24[x + 2] > 255)rgb24[x + 2] = 255;
            if (rgb24[x + 2] < 0)rgb24[x + 2] = 0;
        }
        free(yuv444);
    }


/*************************************************
Function:       process_image
Description:    把图像数据保存到文件
*************************************************/
    static int process_image(const void *p, int len) {

        int writed_count = 0;
        //  static char[115200] Outbuff ;
        if (outf == NULL || outf == 0) {
            return -3;
        }
        fputc('.', stdout);

        if (len > 0) {

            fputc('.', stdout);

            writed_count = fwrite(p, 1, len, outf);

        }

        fflush(stdout);
        return writed_count;

    }


/*************************************************
Function:       openUSBCamera
Description:    打开USB摄像头
*************************************************/
    int openUSBCamera(const char *m_dev_name) {
        struct stat st;

        //先判断该文件的状态
        if (-1 == stat(m_dev_name, &st)) {
            LOGE("Cannot identify '%s': %d, %s\n", m_dev_name, errno, strerror(errno));
            return -1;
        }
        //再判断该文件是不是设备
        if (!S_ISCHR(st.st_mode)) {
            LOGE("%s is not device/n", m_dev_name);
            return -1;
        }
        //打开设备
        fd = open(m_dev_name, O_RDWR /* required */| O_NONBLOCK, 0);
        //判断是否打开成功
        if (-1 == fd) {
            LOGI("Cannot open '%s': %d, %s\n", m_dev_name, errno, strerror(errno));
            return -1;
        }
        canCallback = true;
        //返回该设备的ID
        return fd;

    };

/*************************************************
Function:       closeUSBCamera// 函数名称
Description:    关闭USB摄像头// 函数功能、性能等的描述
Input:          // 输入参数说明，包括每个参数的作
                  // 用、取值说明及参数间关系。
Output:         // 对输出参数的说明。
Return:         // 函数返回值的说明
Others:         // 其它说明
*************************************************/
    int closeUSBCamera() {
        if (fd == -1) {
            return 0;
        }

//        if (outf != NULL && outf != 0) {
//            fclose(outf);
//        }

        canCallback = false;
        return 0;
    };

/*************************************************
检查是否支持某种格式
*************************************************/
    int canGetType() {
        struct v4l2_format fmt;
        fmt.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        fmt.fmt.pix.pixelformat = V4L2_PIX_FMT_NV12;
        if (ioctl(fd, VIDIOC_TRY_FMT, &fmt) == -1) {
            if (errno == EINVAL) {
                printf("not support format RGB32!/n");
            }
        }


    }

/*************************************************
Function:       getInfo
Description:    获取USB摄像头的信息
Return:         设备信息
*************************************************/
    string getInfo() {
        /*  查看设备名称等信息*/
        struct v4l2_capability cap;
        ioctl(fd, VIDIOC_QUERYCAP, &cap);
        //把结果封装一下
        std::stringstream ss;
        ss << " [DriverName]:" << cap.driver
           << " [CardName]:" << cap.card
           << " [BusInfo]:" << cap.bus_info
           << " [DriverVersion]:"
           << ((cap.version >> 24) & 0XFF) << " | "
           << ((cap.version >> 16) & 0XFF) << " | "
           << ((cap.version >> 8) & 0XFF) << " | "
           << (cap.version & 0xff)
           << " [Capabilities]:"
           << ((cap.capabilities >> 24) & 0XFF) << " | "
           << ((cap.capabilities >> 16) & 0XFF) << " | "
           << ((cap.capabilities >> 8) & 0XFF) << " | "
           << (cap.capabilities & 0xff)
           << "\n";

        /* 查看支持格式*/
        struct v4l2_fmtdesc fmtdesc;
        fmtdesc.index = 0;
        fmtdesc.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        //获取支持的格式列表  并把结果封装一下
        ss << " SupportFormat:";
        while (ioctl(fd, VIDIOC_ENUM_FMT, &fmtdesc) != -1) {
            //把结果封装一下
            ss << (fmtdesc.index + 1) << ":" << (fmtdesc.description) << "  ";
            fmtdesc.index++;
        }
        ss << "\n";

        //如果需要设置的话 使用 ioctl(fd, VIDIOC_S_FMT, &fmt);

        /*  查看当前帧的相关信息（长宽）*/
        struct v4l2_format fmt;
        fmt.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        ioctl(fd, VIDIOC_G_FMT, &fmt);
        //把结果封装一下
        ss << " v4l2_format: width:" << fmt.fmt.pix.width << " height:"
           << fmt.fmt.pix.height << "\n";
        //如果需要设置的话 使用 ioctl(fd, VIDIOC_S_FMT, &fmt);
        return ss.str();
    }

/*************************************************
Function:       getCache
Description:    申请一个缓冲区(4帧) 并用buffers把指针存起来
*************************************************/
    int getCache() {
        //定义buffer（缓冲区）的属性
        struct v4l2_requestbuffers req;
        //缓存多少帧
        req.count = 4;
        //buffer的类型
        req.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        //采用内存映射的方式
        req.memory = V4L2_MEMORY_MMAP;
        //申请缓冲区
        int result_getcache = ioctl(fd, VIDIOC_REQBUFS, &req);
        //如果为-1则说明申请失败
        if (result_getcache == -1) {
            fprintf(stderr, "VIDIOC_REQBUFS fail");
            return -1;
        }
        //用buffer存储缓冲区的指针
        buffers = (buffer *) calloc(req.count, sizeof(*buffers));
        if (!buffers) {
            fprintf(stderr, "Out of memory/n");
            return -2;

        }
        //开始映射  四帧图像的区域都要
        for (n_buffers = 0; n_buffers < req.count; ++n_buffers) {
            struct v4l2_buffer buf;
            memset(&buf, 0, sizeof(buf));
            buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
            buf.memory = V4L2_MEMORY_MMAP;
            buf.index = n_buffers;
            // 查询序号为n_buffers 的缓冲区，得到其起始物理地址和大小 -1则说明失败
            if (-1 == ioctl(fd, VIDIOC_QUERYBUF, &buf)) {
                return -3;
            }
            buffers[n_buffers].length = buf.length;
            // 映射内存
            buffers[n_buffers].start = mmap(NULL, buf.length, PROT_READ | PROT_WRITE, MAP_SHARED,
                                            fd, buf.m.offset);
            //如果映射失败则直接返回
            if (MAP_FAILED == buffers[n_buffers].start) {
                return -4;
            }
        }
        return 0;
    }


/*************************************************
Function:       startCapture
Description:    开始捕获数据（数据会存入缓冲区）
*************************************************/
    int startCapture(void) {
        unsigned int i;
        enum v4l2_buf_type type;
        //把四个帧放入队列
        for (i = 0; i < n_buffers; ++i) {
            struct v4l2_buffer buf;
            memset(&buf, 0, sizeof(buf));
            buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
            buf.memory = V4L2_MEMORY_MMAP;
            buf.index = i;
            //把帧放入队列
            if (-1 == ioctl(fd, VIDIOC_QBUF, &buf)) {
                LOGE("VIDIOC_QBUF error %d, %s\n", errno, strerror(errno));
                return -1;
            }
        }
        //类型设置为捕获数据
        type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        //启动数据流
        if (-1 == ioctl(fd, VIDIOC_STREAMON, &type)) {
            LOGE("VIDIOC_STREAMON error %d, %s\n", errno, strerror(errno));
            return -2;
        }
        return 0;
    }

    jbyteArray data[50];

/*************************************************
Function:       getOneFrame
Description:    从缓冲区获取一帧数据
Return:         结果
*************************************************/
    int getOneFrame() {
        struct v4l2_buffer buf;
        unsigned int i;
        memset(&buf, 0, sizeof(buf));
        buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        buf.memory = V4L2_MEMORY_MMAP;
        //从队列中取数据到缓冲区
        if (-1 == ioctl(fd, VIDIOC_DQBUF, &buf)) {
            LOGE("VIDIOC_DQBUF error %d , %s", errno, strerror(errno));
        }
        //
        assert(buf.index < n_buffers);
        if (buf.bytesused <= 0xaf) {
            /* Prevent crash on empty image */
            LOGI("Ignoring empty buffer ...\n");
            return -1;
        }
        oneFrameLength = buf.bytesused;
        framebuf = (void *) malloc(oneFrameLength);
        pthread_mutex_lock(&lock);
        //从视频数据copy到framebuf中
        memcpy(framebuf, buffers[buf.index].start, oneFrameLength);
        pthread_mutex_unlock(&lock);
        //填充队列
        if (-1 == ioctl(fd, VIDIOC_QBUF, &buf))
            LOGE("VIDIOC_QBUF error %d, %s", errno, strerror(errno));
        return 0;


    }


    /*************************************************
Function:       YUV422To420
Description:    422转420
*************************************************/
    int YUV422To420(char *yuv422, char *yuv420, int width, int height) {

        int ynum = width * height;
        int i, j, k = 0;
        //得到Y分量
        for (i = 0; i < ynum; i++) {
            yuv420[i] = yuv422[i * 2];
        }
        //得到U分量
        for (i = 0; i < height; i++) {
            if ((i % 2) != 0)continue;
            for (j = 0; j < (width / 2); j++) {
                if ((4 * j + 1) > (2 * width))break;
                yuv420[ynum + k * 2 * width / 4 + j] = yuv422[i * 2 * width + 4 * j + 1];
            }
            k++;
        }
        k = 0;
        //得到V分量
        for (i = 0; i < height; i++) {
            if ((i % 2) == 0)continue;
            for (j = 0; j < (width / 2); j++) {
                if ((4 * j + 3) > (2 * width))break;
                yuv420[ynum + ynum / 4 + k * 2 * width / 4 + j] = yuv422[i * 2 * width + 4 * j + 3];

            }
            k++;
        }


        return 1;
    }


/*************************************************
Function:       thread_entry
Description:    获取图像并callback到java层的线程
*************************************************/
    void *thread_entry(void *data) {


        //获取全局的*env;
        JNIEnv *env;
        mjavaVM->AttachCurrentThread(&env, NULL);
        jclass clazz = env->GetObjectClass(mjavaobject);
        //获取到java的方法ID
        jmethodID mID = env->GetMethodID(clazz, "myCallback", "([BI)V");

        //开始while循环获取图像并callback出去，用canCallback这个变量控制是否停止
        while (canCallback) {
            if (env != NULL && mID != NULL && mjavaobject != NULL) {
                struct v4l2_buffer buf;
                unsigned int i;
                memset(&buf, 0, sizeof(buf));
                buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
                buf.memory = V4L2_MEMORY_MMAP;
                //从队列中取数据到缓冲区
                if (-1 == ioctl(fd, VIDIOC_DQBUF, &buf)) {
                    LOGE("VIDIOC_DQBUF error %d , %s", errno, strerror(errno));
                    usleep(50000);
                    continue;
                }
                //判断获取到的数据是否有效
                assert(buf.index < n_buffers);
                if (buf.bytesused <= 0xaf) {
                    LOGI("Ignoring empty buffer ...\n");
                    usleep(50000);
                    continue;
                }
                //记录下获取到数据的长度
                oneFrameLength = buf.bytesused;

                pthread_mutex_lock(&lock);
                //先定义一个存YUV420的
                char *yuv420;
                memset(&yuv420, 0, sizeof((640 * 480) + (640 * 480 / 2)));

                //实例化一个数组
                jbyteArray temp_result = env->NewByteArray(oneFrameLength);
                //把获取到的图像数据赋值给数组
                env->SetByteArrayRegion(temp_result, 0, oneFrameLength,
                                        (jbyte *) buffers[buf.index].start);
                //调用java层的代码
                env->CallVoidMethod(mjavaobject, mID, temp_result, oneFrameLength);
                //销毁掉创建出来的两个临时变量
                env->DeleteLocalRef(temp_result);
                pthread_mutex_unlock(&lock);
                //填充队列
                if (-1 == ioctl(fd, VIDIOC_QBUF, &buf)) {
                    LOGE("VIDIOC_QBUF error %d, %s", errno, strerror(errno));
                    usleep(50000);
                    continue;
                }
            } else {
                LOGD("menv == NULL || obj == NULL || mID == NULL...\n");
            }
            //间隔50毫秒
            //单位：微秒   1000微秒=1毫秒
            usleep(50000);
        }

        //释放申请的缓冲
        unsigned int i;
        for (i = 0; i < n_buffers; ++i) {
            if (-1 == munmap(buffers[i].start, buffers[i].length)) {
                LOGE("munmap error %d , %s", errno, strerror(errno));
            }
        }
            free(buffers);

        if (fd != -1) {
            LOGI("close fd \n");
            close(fd);
        }

        fd = -1;
        //销毁掉
        mjavaVM->DetachCurrentThread();
    }


    void *thread_entry_old(void *data) {


        //获取全局的*env;
        JNIEnv *env;
        mjavaVM->AttachCurrentThread(&env, NULL);
        jclass clazz = env->GetObjectClass(mjavaobject);
        //获取到java的方法ID
        jmethodID mID = env->GetMethodID(clazz, "myCallback", "([BI)V");

        //开始while循环获取图像并callback出去，用canCallback这个变量控制是否停止
        while (canCallback) {
            if (env != NULL && mID != NULL && mjavaobject != NULL) {
                struct v4l2_buffer buf;
                unsigned int i;
                memset(&buf, 0, sizeof(buf));
                buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
                buf.memory = V4L2_MEMORY_MMAP;
                //从队列中取数据到缓冲区
                if (-1 == ioctl(fd, VIDIOC_DQBUF, &buf)) {
                    LOGE("VIDIOC_DQBUF error %d , %s", errno, strerror(errno));
                    usleep(50000);
                    continue;
                }
                //判断获取到的数据是否有效
                assert(buf.index < n_buffers);
                if (buf.bytesused <= 0xaf) {
                    LOGI("Ignoring empty buffer ...\n");
                    usleep(50000);
                    continue;
                }
                //记录下获取到数据的长度
                oneFrameLength = buf.bytesused;

                pthread_mutex_lock(&lock);
                //先定义一个存YUV420的
                char *yuv420;
                memset(&yuv420, 0, sizeof((640 * 480) + (640 * 480 / 2)));

                //实例化一个数组
                jbyteArray temp_result = env->NewByteArray(oneFrameLength);
                //把获取到的图像数据赋值给数组
                env->SetByteArrayRegion(temp_result, 0, oneFrameLength,
                                        (jbyte *) buffers[buf.index].start);
                //调用java层的代码
                env->CallVoidMethod(mjavaobject, mID, temp_result, oneFrameLength);
                //销毁掉创建出来的两个临时变量
                env->DeleteLocalRef(temp_result);
                pthread_mutex_unlock(&lock);
                //填充队列
                if (-1 == ioctl(fd, VIDIOC_QBUF, &buf)) {
                    LOGE("VIDIOC_QBUF error %d, %s", errno, strerror(errno));
                    usleep(50000);
                    continue;
                }
            } else {
                LOGD("menv == NULL || obj == NULL || mID == NULL...\n");
            }
            //间隔50毫秒
            //单位：微秒   1000微秒=1毫秒
            usleep(50000);
        }
        //销毁掉
        mjavaVM->DetachCurrentThread();
    }


    extern "C"
    JNIEXPORT jstring

    JNICALL
    Java_demo_xu_usbcanerademo_NativeTest_stringFromJNI(
            JNIEnv *env,
            jobject /* this */) {
        std::string hello = "Hello from C++";
        return env->NewStringUTF(hello.c_str());
    }
    extern "C"
    JNIEXPORT jstring JNICALL
    Java_demo_xu_usbcanerademo_NativeTest_test(JNIEnv *env, jobject instance) {

        // TODO
        std::stringstream ss;
        ss << "DriverName:" << 1;
        string test = ss.str();
        return env->NewStringUTF(test.c_str());
    }


    extern "C"
    JNIEXPORT jint JNICALL
    Java_demo_xu_usbcanerademo_NativeTest_openCamera(JNIEnv *env, jobject instance,
                                                     jstring devName) {
        // TODO

        const char *str;
        str = env->GetStringUTFChars(devName, JNI_FALSE);
        int ret = openUSBCamera(str);

        env->ReleaseStringUTFChars(devName, str);
        return ret;

    }
    extern "C"
    JNIEXPORT jint JNICALL
    Java_demo_xu_usbcanerademo_NativeTest_closeCamera(JNIEnv *env, jobject instance) {
        // TODO
        return closeUSBCamera();
    }

    extern "C"
    JNIEXPORT jstring JNICALL
    Java_demo_xu_usbcanerademo_NativeTest_getDevicInfo(JNIEnv *env, jobject instance) {
        // TODO
        string resule = getInfo();
        return env->NewStringUTF(resule.c_str());
    }

    extern "C"
    JNIEXPORT jint JNICALL
    Java_demo_xu_usbcanerademo_NativeTest_getCache(JNIEnv *env, jobject instance) {
        // TODO
        return getCache();
    }

    extern "C"
    JNIEXPORT jint JNICALL
    Java_demo_xu_usbcanerademo_NativeTest_startCapture(JNIEnv *env, jobject instance) {
        // TODO
        return startCapture();
    }

    extern "C"
    JNIEXPORT jint JNICALL
    Java_demo_xu_usbcanerademo_NativeTest_getOneFrame(JNIEnv *env, jobject instance) {
        // TODO


        return getOneFrame();
//        env->ReleaseByteArrayElements( data_, data, 0);

    }

    extern "C"
    JNIEXPORT void JNICALL
    Java_demo_xu_usbcanerademo_NativeTest_start(JNIEnv *env, jobject instance) {

        // TODO
        __android_log_print(ANDROID_LOG_ERROR, "Callback JNI", "begin");

        env->GetJavaVM(&mjavaVM);
        mjavaobject = env->NewGlobalRef(instance);
        pthread_t trecv;
        int result = pthread_create(&trecv, NULL, thread_entry, NULL);
        LOGD("result%d", result);
        __android_log_print(ANDROID_LOG_ERROR, "Callback JNI", "end");

    }

}