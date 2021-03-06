
package com.roman.romcontrol.service;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class WeatherRefreshService extends Service {

    public static final String TAG = "WeatherRefreshService";

    Context mContext;
    SharedPreferences prefs;
    AlarmManager alarms;

    PendingIntent weatherRefreshIntent;

    int refreshIntervalInMinutes;

    public static final String KEY_START_ON_BOOT = "start_on_boot";
    public static final String KEY_REFRESH = "refresh_interval";

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
        prefs = getApplicationContext().getSharedPreferences("weather", MODE_PRIVATE);
        alarms = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        refreshIntervalInMinutes = prefs.getInt(KEY_REFRESH, 0);
        Log.i("Refresher", "service started with refresh: " + refreshIntervalInMinutes);
        prefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(KEY_REFRESH)) {
                    refreshIntervalInMinutes = prefs.getInt(KEY_REFRESH, 0);
                    Log.i("Refresher", "new value: " + refreshIntervalInMinutes);
                    scheduleRefresh();
                }
                Log.i("Refresher", "new key: " + key);
            }
        });
    }

    private void scheduleRefresh() {
        cancelRefresh();
        if (refreshIntervalInMinutes == 0) {
            Log.i(TAG, "Did not schedule refresh.");
            return;
        }

        Log.i(TAG, "scheduling with refresh interval : " + refreshIntervalInMinutes + " minutes");

        Intent i = new Intent(getApplicationContext(), WeatherService.class);
        i.setAction(WeatherService.INTENT_REQUEST_WEATHER);
        weatherRefreshIntent = PendingIntent.getService(getApplicationContext(), 0, i,
                0);

        Calendar timeToStart = Calendar.getInstance();
        timeToStart.setTimeInMillis(System.currentTimeMillis());
        timeToStart.add(Calendar.MINUTE, 1);

        long interval = TimeUnit.MILLISECONDS.convert(refreshIntervalInMinutes,
                TimeUnit.MINUTES);

        alarms.setInexactRepeating(AlarmManager.RTC, timeToStart.getTimeInMillis(), interval,
                weatherRefreshIntent);
    }

    private void cancelRefresh() {
        if (weatherRefreshIntent != null)
            alarms.cancel(weatherRefreshIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Log.i("LocalService", "Received start id " + startId + ": " + intent);
        refreshIntervalInMinutes = prefs.getInt(KEY_REFRESH, 0);
        scheduleRefresh();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
