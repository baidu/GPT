package com.harlan.animation;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillManager;
import android.widget.Button;
import android.widget.EditText;

import javax.crypto.spec.DESedeKeySpec;

public class AutoFillTestActivity extends Activity {

    private static final String TAG = "AutoFillTest";

    public static final boolean DEBUG = Constants.DEBUG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_fill_test);
        EditText et = (EditText) findViewById(R.id.et_autofilltest);
        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (DEBUG) {
                    Log.d(TAG, "焦点:" + hasFocus);
                }
            }
        });
        Button btn = (Button) findViewById(R.id.btn_autofilltest);
        btn.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                // only test
                AutofillManager autofillApp = (AutofillManager) getApplicationContext().getSystemService("autofill");

                AutofillManager systemService = AutoFillTestActivity.this.getSystemService(AutofillManager.class);
                boolean enabled = systemService.isEnabled();
                AutofillManager autofill = (AutofillManager) AutoFillTestActivity.this.getSystemService("autofill");
                boolean b = autofill.hasEnabledAutofillServices();
                if (DEBUG) {
                    Log.d(TAG, "enable:" + enabled + ",enableService:" + b);
                }
            }
        });
    }
}
