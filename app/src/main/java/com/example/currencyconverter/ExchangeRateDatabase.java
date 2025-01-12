package com.example.currencyconverter;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ExchangeRateDatabase {
    private final static ExchangeRate[] RATES = {
            new ExchangeRate("EUR", "Bruxelles", 1.0),
            new ExchangeRate("USD", "Washington", 1.0845),
            new ExchangeRate("JPY", "Tokyo", 130.02),
            new ExchangeRate("BGN", "Sofia", 1.9558),
            new ExchangeRate("CZK", "Prague", 27.473),
            new ExchangeRate("DKK", "Copenhagen", 7.4690),
            new ExchangeRate("GBP", "London", 0.73280),
            new ExchangeRate("HUF", "Budapest", 299.83),
            new ExchangeRate("PLN", "Warsaw", 4.0938),
            new ExchangeRate("RON", "Bucharest", 4.4050),
            new ExchangeRate("SEK", "Stockholm", 9.3207),
            new ExchangeRate("CHF", "Bern", 1.0439),
            new ExchangeRate("ISK", "Rejkjavic", 141.10),
            new ExchangeRate("NOK", "Oslo", 8.6545),
            new ExchangeRate("HRK", "Zagreb", 7.6448),
            new ExchangeRate("TRY", "Ankara", 2.8265),
            new ExchangeRate("AUD", "Canberra", 1.4158),
            new ExchangeRate("BRL", "Brasilia", 3.5616),
            new ExchangeRate("CAD", "Ottawa", 1.3709),
            new ExchangeRate("CNY", "Beijing", 6.7324),
            new ExchangeRate("HKD", "Hong Kong", 8.4100),
            new ExchangeRate("IDR", "Jakarta", 14172.71),
            new ExchangeRate("ILS", "Jerusalem", 4.3019),
            new ExchangeRate("INR", "New Delhi", 67.9180),
            new ExchangeRate("KRW", "Seoul", 1201.04),
            new ExchangeRate("MXN", "Mexico City", 16.5321),
            new ExchangeRate("MYR", "Kuala Lumpur", 4.0246),
            new ExchangeRate("NZD", "Wellington", 1.4417),
            new ExchangeRate("PHP", "Manila", 48.527),
            new ExchangeRate("SGD", "Singapore", 1.4898),
            new ExchangeRate("THB", "Bangkok", 35.328),
            new ExchangeRate("ZAR", "Cape Town", 13.1446)
    };

    private final static Map<String, ExchangeRate> CURRENCIES_MAP = new HashMap<>();

    private final static String[] CURRENCIES_LIST;

    static {
        for (ExchangeRate r : RATES) {
            CURRENCIES_MAP.put(r.getCurrencyName(), r);
        }
        CURRENCIES_LIST = new String[CURRENCIES_MAP.size()];

        CURRENCIES_MAP.keySet().toArray(CURRENCIES_LIST);
        Arrays.sort(CURRENCIES_LIST);

    }

    public ExchangeRateDatabase(Context context) {
        // try to load saved rates from file
        JSONObject savedRates = readRatesFromFile(context);
        if (savedRates != null) {
            try {
                Iterator<String> keys = savedRates.keys();
                while (keys.hasNext()) {
                    String currency = keys.next();
                    // get nested JSONObject for each currency and extract the rate out of it
                    JSONObject currencyData = savedRates.getJSONObject(currency);
                    double rate = currencyData.getDouble("rate");

                    if (CURRENCIES_MAP.containsKey(currency)) {
                        CURRENCIES_MAP.get(currency).setRateForOneEuro(rate);
                        Log.d("ExchangeRateDatabase", "Loaded saved rate for " + currency + ": " + rate);
                    }
                }
            } catch (JSONException e) {
                Log.e("ExchangeRateDatabase", "Error parsing saved rates.", e);
            }
        } else {
            Log.d("ExchangeRateDatabase", "No saved rates found. Using hardcoded rates.");
        }
    }

    private JSONObject readRatesFromFile(Context context) {
        if (context == null) {
            Log.e("ExchangeRateDatabase", "Context is null, cannot read from file");
            return null;
        }
        try {
            FileInputStream fis = context.openFileInput("currency_rates.json");
            StringBuilder builder = new StringBuilder();
            int ch;
            while ((ch = fis.read()) != -1) {
                builder.append((char) ch);
            }
            fis.close();
            return new JSONObject(builder.toString());
        } catch (IOException e) {
            Log.e("ExchangeRateDatabase", "File not found or cannot be read, using default rates", e);
            return null;
        } catch (JSONException e) {
            Log.e("ExchangeRateDatabase", "File contains invalid JSON, using default rates", e);
            return null;
        }
    }

    public String[] getCurrencies() {
        return CURRENCIES_LIST;
    }

    public double getExchangeRate(String currency) {
        return CURRENCIES_MAP.get(currency).getRateForOneEuro();
    }

    public void setExchangeRate(String currency, double exchangeRate) {
        if (CURRENCIES_MAP.containsKey(currency)) {
            CURRENCIES_MAP.get(currency).setRateForOneEuro(exchangeRate);
            Log.d("ExchangeRateDatabase", "Updated " + currency + " with new rate: " + exchangeRate);
        } else {
            Log.d("ExchangeRateDatabase", "Currency " + currency + " not found for update.");
        }
    }

    public String getCapital(String currency) {
        return CURRENCIES_MAP.get(currency).getCapital();
    }

    public double convert(double value, String currencyFrom, String currencyTo) {
        return value / getExchangeRate(currencyFrom) * getExchangeRate(currencyTo);
    }
}
