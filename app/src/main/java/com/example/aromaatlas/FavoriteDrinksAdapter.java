package com.example.aromaatlas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class FavoriteDrinksAdapter extends RecyclerView.Adapter<FavoriteDrinksAdapter.FavoriteDrinkViewHolder> {

    private final List<MenuItemModel> favoriteDrinks;

    public FavoriteDrinksAdapter(List<MenuItemModel> favoriteDrinks) {
        this.favoriteDrinks = favoriteDrinks;
    }

    @NonNull
    @Override
    public FavoriteDrinkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_drink, parent, false);
        return new FavoriteDrinkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteDrinkViewHolder holder, int position) {
        MenuItemModel drink = favoriteDrinks.get(position);

        holder.nameText.setText(drink.getName());
        holder.typeText.setText("Type: " + drink.getDrink());
        holder.priceText.setText(String.format("RM %.2f", drink.getPrice()));

        Glide.with(holder.itemView.getContext())
                .load(drink.getImage())
                .placeholder(R.drawable.placeholder_image)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return favoriteDrinks.size();
    }

    static class FavoriteDrinkViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, typeText, priceText;
        ImageView imageView;

        public FavoriteDrinkViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.drink_name);
            typeText = itemView.findViewById(R.id.drink_type);
            priceText = itemView.findViewById(R.id.drink_price);
            imageView = itemView.findViewById(R.id.drink_image);
        }
    }
}
