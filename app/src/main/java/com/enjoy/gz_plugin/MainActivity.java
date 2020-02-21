package com.enjoy.gz_plugin;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printClassLoader();

                try {
                    Class<?> clazz = Class.forName("com.enjoy.plugin.Test");
                    Method print = clazz.getMethod("print");
                    print.invoke(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent();
                // 启动插件类
                intent.setComponent(new ComponentName("com.enjoy.plugin",
                        "com.enjoy.plugin.MainActivity"));
                startActivity(intent);
            }
        });
    }


    private void printClassLoader() {
        ClassLoader classLoader = getClassLoader();
        while (classLoader != null) {
            Log.e("gz", "printClassLoader: " + classLoader);
            classLoader = classLoader.getParent();
        }
        // 相同1,不相同2  ---

        // Boot
        Log.e("gz", "Activity: " + Activity.class.getClassLoader());
        // Path
        Log.e("gz", "AppCompatActivity: " + AppCompatActivity.class.getClassLoader());

    }

}
