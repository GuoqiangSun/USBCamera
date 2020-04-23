package demo.xu.usbcanerademo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 * Author: Guoqiang_Sun
 * Date: 2019/12/30.
 * Description:
 */
public class CService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
