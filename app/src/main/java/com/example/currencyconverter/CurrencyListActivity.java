package com.example.currencyconverter;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Arrays;

public class CurrencyListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currencylist);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Currency List");
        }

        ExchangeRateDatabase exchangeRateDatabase = new ExchangeRateDatabase(this);
        CurrencyAdapter adapter = new CurrencyAdapter(this, Arrays.asList(exchangeRateDatabase.getCurrencies()));

        ListView listView = findViewById(R.id.currency_list);
        listView.setAdapter(adapter);

        Intent intent = getIntent();
        String spinnerType = intent.getStringExtra("spinnerType");

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCurrency = adapter.getItem(position).toString();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("selectedCurrency", selectedCurrency);
            resultIntent.putExtra("spinnerType", spinnerType);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedCurrency = adapter.getItem(position).toString();

            String capitalCity = exchangeRateDatabase.getCapital(selectedCurrency);

            if (capitalCity != null) {
                String mapUri = "geo:0,0?q=" + capitalCity;
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(mapUri));
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Log.e("CurrencyListActivity", String.format("No application can handle the GEO intent (URI: %s)", mapUri));
                    Toast.makeText(CurrencyListActivity.this, "No application can run the GEO intent", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(CurrencyListActivity.this, "Capital not found for this currency", Toast.LENGTH_SHORT).show();
            }

            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
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
        }
        return super.onOptionsItemSelected(item);
    }
}
