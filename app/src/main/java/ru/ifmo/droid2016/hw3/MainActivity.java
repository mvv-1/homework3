package ru.ifmo.droid2016.hw3;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    EnormousPictureLoader.Binder binder;
    ServiceConnection conn;
    TextView text;
    ImageView pic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.text);
        pic = (ImageView) findViewById(R.id.enormous_pic);
        text.setText(getResources().getString(R.string.no_pic));
        repair();
    }

    private void repair() {
        startService(new Intent(this, EnormousPictureLoader.class));
        bindService(new Intent(this, EnormousPictureLoader.class), conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binder = (EnormousPictureLoader.Binder) service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w("MAIN", "Suddenly lost service");
                repair();
            }
        }, Context.BIND_NOT_FOREGROUND);
    }

    public void loadContent(final View imageView) {
        if (binder == null) {
            Log.d("CONN", "NULL");
            return;
        }
        binder.getPicture(new EnormousPictureLoader.Callback<Byte>() {
            @Override
            public void call(Byte arg) {
                text.setText(getResources().getString(R.string.loading) + ' ' + arg + '%');
            }
        }, new EnormousPictureLoader.Callback<Boolean>() {
            @Override
            public void call(Boolean arg) {
                if (arg) {
                    text.setText(getResources().getString(R.string.done));
                    pic.setImageURI(Uri.fromFile(binder.pictureFile));
                } else {
                    text.setText(getResources().getString(R.string.error));
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
    }

    public void clearContent(final View textView) {
        binder.pictureFile.delete();
    }
}
