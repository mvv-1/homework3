package ru.ifmo.droid2016.hw3;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class EnormousPictureLoader extends Service {
    private static final int MB = 8388608; // 1 MB
    private String PIC = null;
    private AtomicBoolean busy = new AtomicBoolean(false);
    private BroadcastReceiver receiver = null;
    private Callback<Boolean> saved = null;

    public EnormousPictureLoader() {
        super();
        Log.d("SRV", "CREAT");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PIC = getFilesDir().toString() + "/gosha.jpg";
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (saved != null) loadPicture(new Callback<Byte>() {
                    @Override
                    public void call(Byte arg) {

                    }
                }, new Callback<Boolean>() {
                    @Override
                    public void call(Boolean arg) {
                        saved.call(arg);
                    }
                });
            }
        };
        IntentFilter fil = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(receiver, fil);
    }

    private boolean existsPic() {
        return new File(PIC).exists();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    private boolean loadPicture(Callback<Byte> progress, Callback<Boolean> onFinish) {
        if (busy.get()) {
            return false;
        }
        if (existsPic()) onFinish.call(true);
        else {
            busy.set(true);
            new Downloader(progress, onFinish).execute(getResources().getString(R.string.picture_url_key));
        }
        return true;
    }

    public interface Callback<A> {
        void call(A arg);
    }

    public class Binder extends android.os.Binder {
        public final File pictureFile = new File(PIC);

        public boolean getPicture(Callback<Byte> progress, Callback<Boolean> onFinish) {
            return loadPicture(progress, onFinish);
        }

        public void doWhenPicWillBeLoaded(Callback<Boolean> callback) {
            saved = callback;
        }
    }

    private class Downloader extends AsyncTask<String, Byte, Boolean> {
        private final Callback<Boolean> action;
        private final Callback<Byte> progress;

        public Downloader(Callback<Byte> progress, Callback<Boolean> result) {
            super();
            this.action = result;
            this.progress = progress;
            Log.d("AT", "constr");
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Log.d("AT", "DIB");
            final URL url;
            try {
                url = new URL(params[0]);
                //intent.getStringExtra(getResources().getString(R.string.picture_url_key)));
            } catch (MalformedURLException e) {
                Log.wtf("MF URL", e.getMessage());
                return false;
            }
            HttpURLConnection connection = null;
            BufferedInputStream input = null;
            FileOutputStream output = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                Log.d("CODE", String.valueOf(connection.getResponseCode()));
                int length = connection.getContentLength();
                input = new BufferedInputStream(connection.getInputStream(), MB);
                output = new FileOutputStream(PIC);
                byte[] buffer = new byte[MB];
                int count;
                int done = 0;
                while ((count = input.read(buffer)) != -1) {
                    done += count;
                    publishProgress((byte) (done * 100 / length));
                    output.write(buffer, 0, count);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (input != null) input.close();
                    if (output != null) output.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d("AT", "Pre");
            if (new File(PIC).exists()) {
                this.cancel(false);
                action.call(true);
                busy.set(false);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d("AT", "Post");
            if (!result) (new File(PIC)).delete();
            busy.set(false);
            action.call(result);
        }

        @Override
        protected void onProgressUpdate(Byte... values) {
            super.onProgressUpdate(values);
            Log.d("AT", "PU");
            progress.call(values[0]);
        }
    }
}
