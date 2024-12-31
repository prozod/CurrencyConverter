package com.example.currencyconverter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class CurrencyAdapter extends BaseAdapter {

    private Context context;
    private List<String> currencies;

    public CurrencyAdapter(Context context, List<String> currencies) {
        this.context = context;
        this.currencies = currencies;
    }

    @Override
    public int getCount() {
        return currencies.size();
    }

    @Override
    public Object getItem(int position) {
        return currencies.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.activity_currency_item, parent, false);
        }

        TextView currencyName = convertView.findViewById(R.id.currency_item);
        ImageView currencyFlag = convertView.findViewById(R.id.currency_flag);

        String currency = currencies.get(position);

        int resourceId = context.getResources().getIdentifier("flag_" + currency.toLowerCase(), "drawable", context.getPackageName());
        currencyFlag.setImageResource(resourceId);


        currencyName.setText(currency);

        return convertView;
    }
}
