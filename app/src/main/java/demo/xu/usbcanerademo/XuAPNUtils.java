package demo.xu.usbcanerademo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Created by 601042 on 2018/5/8.
 */

public class XuAPNUtils {
    private static final String TAG = "XuAPNUtils";
    public static boolean hasAPN;
    private static final Uri APN_URI = Uri.parse("content://telephony/carriers");
    private static final Uri CURRENT_APN_URI = Uri.parse("content://telephony/carriers/preferapn");

    // 新增一个cmnet接入点
    public void XuAPNUtils(Context context) {
        checkAPN(context);
    }

    public int addAPN(Context context) {
        int id = -1;
        String NUMERIC = getSIMInfo(context);
        Log.e(TAG,"NUMERIC:"+NUMERIC);
        if (NUMERIC == null) {
            return -1;
        }
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put("name", "物联卡");                                  //apn中文描述
        values.put("apn", "CMCC.IOTAPN");                                     //apn名称
        values.put("type", "default");                            //apn类型
        values.put("numeric", NUMERIC);
        values.put("mcc", NUMERIC.substring(0, 3));
        values.put("mnc", NUMERIC.substring(3, NUMERIC.length()));
        values.put("proxy", "");                                        //代理
        values.put("port", "");                                         //端口
        values.put("mmsproxy", "");                                     //彩信代理
        values.put("mmsport", "");                                      //彩信端口
        values.put("user", "");                                         //用户名
        values.put("server", "");                                       //服务器
        values.put("password", "");                                     //密码
        values.put("mmsc", "");                                          //MMSC
        Cursor c = null;
        Uri newRow = resolver.insert(APN_URI, values);
        if (newRow != null) {
            c = resolver.query(newRow, null, null, null, null);
            int idIndex = c.getColumnIndex("_id");
            c.moveToFirst();
            id = c.getShort(idIndex);
        }
        if (c != null)
            c.close();
        return id;
    }

    protected String getSIMInfo(Context context) {
        TelephonyManager iPhoneManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return iPhoneManager.getSimOperator();
    }

    // 设置接入点
    public void SetAPN(Context context, int id) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put("apn_id", id);
        resolver.update(CURRENT_APN_URI, values, null, null);
    }

    public void checkAPN(Context context) {
        // 检查当前连接的APN
        Cursor cr = context.getContentResolver().query(APN_URI, null, null, null, null);
        while (cr != null && cr.moveToNext()) {
            Log.e(TAG,"apn:"+cr.getString(cr.getColumnIndex("apn")));
            if (cr.getString(cr.getColumnIndex("apn")).equals("CMCC.IOTAPN")) {
                XuAPNUtils.hasAPN = true;
                break;
            }
        }

        String NUMERIC = getSIMInfo(context);
        Log.e(TAG,"mcc:"+NUMERIC.substring(0, 3)+"   mnc:"+NUMERIC.substring(3, NUMERIC.length()));
    }

}
