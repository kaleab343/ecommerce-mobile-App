package com.example.econ2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import java.util.List;

public class GridAdapter extends BaseAdapter {

    private Context context;
    private List<Product> productList;
    private LayoutInflater inflater;

    public GridAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return productList.size();
    }

    @Override
    public Object getItem(int position) {
        return productList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView productImage;
        TextView productName;
        TextView productPrice;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_product, parent, false);
            holder = new ViewHolder();
            holder.productImage = convertView.findViewById(R.id.productImage);
            holder.productName = convertView.findViewById(R.id.productName);
            holder.productPrice = convertView.findViewById(R.id.productPrice);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Product product = productList.get(position);
        holder.productName.setText(product.getName());
        holder.productPrice.setText("Price: $" + product.getPrice());

        // ✅ Load image safely
        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.drawable.placeholder)  // Add this image in res/drawable
                .error(R.drawable.image_error)        // Add this image in res/drawable
                .into(holder.productImage);

        return convertView;
    }
}
