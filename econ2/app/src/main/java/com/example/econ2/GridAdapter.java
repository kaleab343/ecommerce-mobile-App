package com.example.econ2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class GridAdapter extends BaseAdapter {

    private Context context;
    private List<Product> productList;

    public GridAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @Override
    public int getCount() { return productList.size(); }

    @Override
    public Object getItem(int position) { return productList.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
        }

        ImageView img = convertView.findViewById(R.id.productImage);
        TextView name = convertView.findViewById(R.id.productName);
        TextView price = convertView.findViewById(R.id.productPrice);

        Product product = productList.get(position);

        name.setText(product.getName());
        price.setText(product.getPrice());

        // Set the bitmap directly from the Product object
        img.setImageBitmap(product.getImage());

        return convertView;
    }
}
