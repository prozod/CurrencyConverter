package com.example.currencyconverter;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    private ExchangeRateDatabase exchangeRateDatabase;
    private ActivityResultLauncher<Intent> startCurrencyListActivityLauncher;
    private ShareActionProvider shareActionProvider;
    private String resultText;

    private static final String PREFERENCES_NAME = "CurrencyConverterPreferences";
    private static final String KEY_SOURCE_CURRENCY = "source_currency";
    private static final String KEY_TARGET_CURRENCY = "target_currency";
    private static final String KEY_ENTERED_VALUE = "entered_value";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        exchangeRateDatabase = new ExchangeRateDatabase(this);

        // channel for our currency updates
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Currency Update Notifications";
            String description = "Notifications for currency rate updates";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("currency_update_channel", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        JSONObject savedRates = readRatesFromFile();
        if (savedRates != null) {
            Iterator<String> keys = savedRates.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    double rate = savedRates.getDouble(key);
                    exchangeRateDatabase.setExchangeRate(key, rate);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            Log.d("file access", "using saved rates.");
        } else {
            Log.d("file access", "no saved rates found, using predefined rates.");
        }

        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        String savedSourceCurrency = preferences.getString(KEY_SOURCE_CURRENCY, "Select Currency");
        String savedTargetCurrency = preferences.getString(KEY_TARGET_CURRENCY, "Select Currency");
        String savedEnteredValue = preferences.getString(KEY_ENTERED_VALUE, "");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView spinnerFrom = findViewById(R.id.spinner_convert_from);
        TextView spinnerTo = findViewById(R.id.spinner_convert_to);
        TextView conversionResult = findViewById(R.id.conversion_result);
        EditText inputAmount = findViewById(R.id.input_amount);
        ExchangeRateDatabase exchangeRateDatabase = new ExchangeRateDatabase(this); // pass the context for the rates and file storage
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, exchangeRateDatabase.getCurrencies());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // retrieve and set the previous/last data
        spinnerFrom.setText(savedSourceCurrency);
        spinnerTo.setText(savedTargetCurrency);
        inputAmount.setText(savedEnteredValue);

        startCurrencyListActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.hasExtra("selectedCurrency")) {
                            String selectedCurrency = data.getStringExtra("selectedCurrency");
                            String spinnerType = data.getStringExtra("spinnerType");

                            int position = -1;
                            for (int i = 0; i < exchangeRateDatabase.getCurrencies().length; i++) {
                                if (exchangeRateDatabase.getCurrencies()[i].equals(selectedCurrency)) {
                                    position = i;
                                    break;
                                }
                            }

                            // if the currency was found, set the selection
                            if (position != -1) {
                                if ("from".equals(spinnerType)) {
                                    spinnerFrom.setText(selectedCurrency);
                                } else if ("to".equals(spinnerType)) {
                                    spinnerTo.setText(selectedCurrency);
                                }
                            }
                        }
                    }
                }
        );

        spinnerFrom.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CurrencyListActivity.class);
            intent.putExtra("spinnerType", "from");
            startCurrencyListActivityLauncher.launch(intent);
        });

        spinnerTo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CurrencyListActivity.class);
            intent.putExtra("spinnerType", "to");
            startCurrencyListActivityLauncher.launch(intent);
        });

        Button conversionButton = findViewById(R.id.conversion_button);

        conversionButton.setOnClickListener(v -> {
            conversionResult.setVisibility(TextView.INVISIBLE);
            String fromCurrency = spinnerFrom.getText().toString().trim();
            String toCurrency = spinnerTo.getText().toString().trim();

            // save the content preferences on btn click
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_SOURCE_CURRENCY, fromCurrency);
            editor.putString(KEY_TARGET_CURRENCY, toCurrency);
            editor.putString(KEY_ENTERED_VALUE, inputAmount.getText().toString());
            editor.apply();

            if (fromCurrency.equals("Select Currency") ||
                    toCurrency.equals("Select Currency")) {
                Toast.makeText(this, "Please select a valid currency.", Toast.LENGTH_SHORT).show();
                conversionResult.setVisibility(TextView.VISIBLE);
                conversionResult.setText("Select a valid currency.");
            } else {
                String amount = inputAmount.getText().toString();
                if (amount.isEmpty()) {
                    Toast.makeText(this, "Amount cannot be empty!", Toast.LENGTH_SHORT).show();
                    conversionResult.setVisibility(TextView.VISIBLE);
                    conversionResult.setText("Amount cannot be empty!");
                } else {
                    double result = exchangeRateDatabase.convert(Double.parseDouble(amount), fromCurrency, toCurrency);
                    DecimalFormat df = new DecimalFormat("#.00");
                    resultText = String.format("%s %s = %s %s", fromCurrency, df.format(Double.parseDouble(amount)), toCurrency, df.format(result));
                    conversionResult.setVisibility(TextView.VISIBLE);
                    conversionResult.setText(resultText);
                    setShareText("Currency Conversion: " + resultText);
                }
            }

        });


    }

    private void setShareText(String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        if (text != null) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        }
        if (shareActionProvider != null) {
            shareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);

        MenuItem shareItem = menu.findItem(R.id.share_button);

        shareActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareItem);
        setShareText(null);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.home_button) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.share_button) {
            if (resultText == null || resultText.isEmpty()) {
                Toast.makeText(this, "Please calculate the conversion first!", Toast.LENGTH_SHORT).show();
            } else {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Currency Conversion: " + resultText);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            }
            return true;
        } else if (id == R.id.update_currencies) {
            Thread updateThread = new Thread(new CurrencyUpdateRunnable(this, exchangeRateDatabase));
            updateThread.start();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveRatesToFile(JSONObject rates) {
        try {
            FileOutputStream fos = openFileOutput("currency_rates.json", MODE_PRIVATE);
            fos.write(rates.toString().getBytes());
            fos.close();
            Log.d("file access: ", "rates saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("file access: ", "failed to save rates.");
        }
    }

    private JSONObject readRatesFromFile() {
        try {
            FileInputStream fis = openFileInput("currency_rates.json");
            StringBuilder builder = new StringBuilder();
            int ch;
            while ((ch = fis.read()) != -1) {
                builder.append((char) ch);
            }
            fis.close();
            Log.d("file access", "rates restored successfully.");

            // parse the JSON response
            JSONObject jsonObject = new JSONObject(builder.toString());
            JSONObject rates = new JSONObject();

            // go through each currency and get the rate
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject currencyData = jsonObject.getJSONObject(key);
                double rate = currencyData.getDouble("rate");
                rates.put(key, rate);
            }

            return rates;

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.e("file access", "failed to restore rates.");
            return null;
        }
    }
}

