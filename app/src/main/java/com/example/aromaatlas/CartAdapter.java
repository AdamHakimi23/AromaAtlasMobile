package com.example.aromaatlas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final List<CartItem> cartItems;

    public CartAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);

        holder.txtCartName.setText(item.getName() + " x" + item.getQuantity());

        String detail = "Type: " + item.getDrinkType() +
                "\nPrice: RM " + item.getPrice();

        if (!item.getNote().isEmpty()) {
            detail += "\nNote: " + item.getNote();
        }

        holder.txtCartDetail.setText(detail);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView txtCartName, txtCartDetail;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCartName = itemView.findViewById(R.id.txtCartName);
            txtCartDetail = itemView.findViewById(R.id.txtCartDetail);
        }
    }
}
