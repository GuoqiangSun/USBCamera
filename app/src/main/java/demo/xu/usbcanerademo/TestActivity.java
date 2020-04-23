package demo.xu.usbcanerademo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.pointsmart.imiusbcamera.R;

/**
 * android:sharedUserId="android.uid.system"
 * android:sharedUserId="android.uid.shared"
 * android:sharedUserId="android.media"
 */
public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
    }

}
