package com.example.currencyconverter;

import android.content.Intent;
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
import androidx.appcompat.widget.Toolbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> startCurrencyListActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView spinnerFrom = findViewById(R.id.spinner_convert_from);
        TextView spinnerTo = findViewById(R.id.spinner_convert_to);
        TextView conversionResult = findViewById(R.id.conversion_result);
        EditText inputAmount = findViewById(R.id.input_amount);
        ExchangeRateDatabase exchangeRateDatabase = new ExchangeRateDatabase();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, exchangeRateDatabase.getCurrencies());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

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

                            // If a matching currency was found, set the selection
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
                    conversionResult.setVisibility(TextView.VISIBLE);
                    conversionResult.setText(String.format("%s %s = %s %s", fromCurrency, df.format(Double.parseDouble(amount)), toCurrency, df.format(result)));
                }
            }

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
        if (id == R.id.share_button) {
            Log.d("Share button action", "YOU CLICKED SHARE!");
        }
        return super.onOptionsItemSelected(item);
    }
}

