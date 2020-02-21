package com.enjoy.plugin;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;

/**
 * Demo class
 *
 * @author holy
 * @date 2020-02-21
 */
public class BaseActivity extends Activity {

    @Override
    public Resources getResources() {

        if (getApplication() != null && getApplication().getResources() != null) {
            return getApplication().getResources();
        }
        return super.getResources();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
