
package com.roman.romcontrol.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Locale;

import org.xml.sax.SAXException;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.roman.romcontrol.WeatherInfo;
import com.roman.romcontrol.xml.WeatherXmlParser;

public class WeatherService extends IntentService {

    public static final String TAG = "WeatherService";

    public static final String INTENT_REQUEST_WEATHER = "com.aokp.romcontrol.INTENT_WEATHER_REQUEST";
    public static final String INTENT_UPDATE_WEATHER = "com.aokp.romcontrol.INTENT_WEATHER_UPDATE";
    public static final String INTENT_UPDATE_WEATHER_AUTO_OBTAINED = "com.aokp.romcontrol.INTENT_WEATHER_UPDATE";

    public static final String EXTRA_CITY = "city";
    public static final String EXTRA_ZIP = "zip";
    public static final String EXTRA_CONDITION = "condition";
    public static final String EXTRA_FORECAST_DATE = "forecase_date";
    public static final String EXTRA_TEMP_F = "temp_f";
    public static final String EXTRA_TEMP_C = "temp_c";
    public static final String EXTRA_HUMIDITY = "humidity";
    public static final String EXTRA_WIND = "wind";
    public static final String EXTRA_LOW = "todays_low";
    public static final String EXTRA_HIGH = "todays_high";

    public WeatherService() {
        super("WeatherService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        WeatherInfo w = null;
        String extra = null;
        String action = intent.getAction();

        if (action != null && action.equals(INTENT_UPDATE_WEATHER_AUTO_OBTAINED)) {
            Log.i(TAG, "Got location from network, sending weather update intent");
            Bundle b = intent.getExtras();
            Location loc = (Location) b.get(android.location.LocationManager.KEY_LOCATION_CHANGED);
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(),
                        loc.getLongitude(), 1);
                sendBroadcast(parseXml(addresses.get(0).getPostalCode()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            /*
             * if a zip or location is sent as an extra with the intent, it will use that as the
             * location instead of trying to acquire it via the network
             */
            Log.i(TAG, "Requesting weather data.");
            if (intent.hasExtra(EXTRA_ZIP)) {
                extra = intent.getCharSequenceExtra(EXTRA_ZIP).toString();
                w = parseXml(extra);
                if (w != null)
                    sendBroadcast(w);
            } else if (intent.hasExtra(EXTRA_CITY)) {
                extra = intent.getCharSequenceExtra(EXTRA_CITY).toString();
            }

            if (extra != null) {
                w = parseXml(extra);
                if (w != null)
                    sendBroadcast(w);
            } else
                getLocationAndStartService();

        }

    }

    private WeatherInfo parseXml(String extra) {
        try {
            return new WeatherXmlParser(getApplicationContext(), extra)
                    .parse();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendBroadcast(WeatherInfo w) {
        Intent broadcast = new Intent(INTENT_UPDATE_WEATHER);
        broadcast.putExtra(EXTRA_CITY, w.city);
        broadcast.putExtra(EXTRA_CONDITION, w.condition);
        broadcast.putExtra(EXTRA_FORECAST_DATE, w.forecast_date);
        broadcast.putExtra(EXTRA_HIGH, w.todaysHigh);
        broadcast.putExtra(EXTRA_LOW, w.todaysLow);
        broadcast.putExtra(EXTRA_HUMIDITY, w.humidify);
        broadcast.putExtra(EXTRA_TEMP_C, w.temp_c);
        broadcast.putExtra(EXTRA_TEMP_F, w.temp_f);
        broadcast.putExtra(EXTRA_WIND, w.wind);
        broadcast.putExtra(EXTRA_ZIP, w.postal_code);
        getApplicationContext().sendBroadcast(broadcast);
        Log.i(TAG, "Sent weather broadcast.");
    }

    private void getLocationAndStartService() {
        final LocationManager locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);

        Intent i = new Intent(getApplicationContext(), WeatherService.class);
        i.setAction(INTENT_UPDATE_WEATHER_AUTO_OBTAINED);
        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, i, 0);

        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, pi);
    }

}
