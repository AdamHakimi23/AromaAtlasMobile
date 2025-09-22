// File: CafeMenuAdapter.java
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

public class CafeMenuAdapter extends RecyclerView.Adapter<CafeMenuAdapter.MenuViewHolder> {
    private final List<MenuItemModel> menuItems;

    public CafeMenuAdapter(List<MenuItemModel> menuItems) {
        this.menuItems = menuItems;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        MenuItemModel item = menuItems.get(position);

        holder.nameText.setText(item.getName());
        holder.beanText.setText("Bean: " + item.getBean());
        holder.typeText.setText("Type: " + item.getDrink());
        holder.priceText.setText(String.format("Price: RM%.2f", item.getPrice()));

        Glide.with(holder.itemView.getContext())
                .load(item.getImage())
                .placeholder(R.drawable.placeholder_image)
                .into(holder.imageView);

        // ✅ Load favorite state by drink ID (not cafeId)
        boolean isFav = FavoriteManager.isDrinkFavorite(holder.itemView.getContext(), item.getId());
        holder.imageViewFavorite.setImageResource(
                isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline
        );

        // ✅ Toggle favorite by drink ID
        holder.imageViewFavorite.setOnClickListener(v -> {
            FavoriteManager.toggleDrinkFavorite(holder.itemView.getContext(), item.getId());
            notifyItemChanged(position);
        });
    }



    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, beanText, typeText, priceText;
        ImageView imageView;

        ImageView imageViewFavorite;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.menu_item_name);
            beanText = itemView.findViewById(R.id.menu_item_bean);
            typeText = itemView.findViewById(R.id.menu_item_type);
            priceText = itemView.findViewById(R.id.menu_item_price);
            imageView = itemView.findViewById(R.id.menu_item_image);
            imageViewFavorite = itemView.findViewById(R.id.imageViewFavorite); // ✅ link this
        }
    }
}
