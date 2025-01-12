package com.example.currencyconverter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CurrencyUpdateRunnable implements Runnable {
    private static final String URL = "https://www.floatrates.com/daily/eur.json";
    private static final String TAG = "CurrencyUpdateRunnable";
    private final Context context;
    private final ExchangeRateDatabase exchangeRateDatabase;

    public CurrencyUpdateRunnable(Context context, ExchangeRateDatabase exchangeRateDatabase) {
        this.context = context;
        this.exchangeRateDatabase = exchangeRateDatabase;
    }

    @Override
    public void run() {
        Log.d(TAG, "Currency update started");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(URL)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonData = response.body().string();
                Log.d(TAG, "Currency data fetched successfully");

                JSONObject jsonObject = new JSONObject(jsonData);

                // saev the rates to a file
                saveRatesToFile(jsonObject);
                Log.d(TAG, "Rates saved to file successfully");


                // update the "database"
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject currencyData = jsonObject.getJSONObject(key);
                    String currencyCode = currencyData.getString("code");
                    double exchangeRate = currencyData.getDouble("rate");

                    exchangeRateDatabase.setExchangeRate(currencyCode, exchangeRate);
                    Log.d(TAG, "Updated rate for: " + currencyCode + " = " + exchangeRate);
                }

                // send the user successful update notifications/UI updates on the main thread
                ((MainActivity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Exchange rates updated", Toast.LENGTH_SHORT).show();
                    showNotification("Currency Rates Updated", "The exchange rates have been successfully updated");
                });
                Log.d(TAG, "Currency update completed successfully");
            } else {
                Log.e(TAG, "Failed to fetch currency data, response not successful");
                ((MainActivity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Failed to fetch exchange rates", Toast.LENGTH_SHORT).show()
                );
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error updating exchange rates", e);
            e.printStackTrace();
            ((MainActivity) context).runOnUiThread(() ->
                    Toast.makeText(context, "Error updating exchange rates", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void saveRatesToFile(JSONObject rates) {
        try {
            FileOutputStream fos = context.openFileOutput("currency_rates.json", Context.MODE_PRIVATE);
            fos.write(rates.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showNotification(String title, String content) {
        // this is to open the mainacitivty when you click theeee eeee notification
        Intent intent = new Intent(context, MainActivity.class);

        // FLAG_IMMUTABLE flag, i dont know what this does exactly but it was required for the pending intent to work
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // construct the notification we sent to the UI
        Notification notification = new NotificationCompat.Builder(context, "currency_update_channel")
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_notify_currency)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(1, notification);
        }
    }

}
