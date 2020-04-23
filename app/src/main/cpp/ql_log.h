//
// Created by zhoukai on 17-1-3.
//

#ifndef MODEMTOOLTEST_QL_LOG_H
#define MODEMTOOLTEST_QL_LOG_H

#include <android/log.h>

#define DTAG    "USBCameraDemo" // 这个是自定义的LOG的标识
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,DTAG,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,DTAG,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,DTAG,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,DTAG,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL,DTAG,__VA_ARGS__) // 定义LOGF类型




#endif //MODEMTOOLTEST_QL_LOG_H
