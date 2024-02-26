package com.vicaw.receitasfinal.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.vicaw.receitasfinal.R;

import java.util.ArrayList;


public class IngredientListAdapter extends BaseAdapter {

    private static LayoutInflater layoutInflater = null;
    private final ArrayList<String> values;

    public IngredientListAdapter(Context context, ArrayList<String> values ) {
        this.values = values;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount(){
        return values.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View item = layoutInflater.inflate(R.layout.row_ingredient,null);

        TextView textViewNome = item.findViewById(R.id.rowIngredient_name);
        String ingredient = values.get(position);
        textViewNome.setText(ingredient);

        return item;
    }


}

