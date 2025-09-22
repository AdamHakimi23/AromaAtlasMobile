// File: MenuItemViewHolder.java
package com.example.aromaatlas;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

public class MenuItemViewHolder extends RecyclerView.ViewHolder {

    public final TextView txtName, txtPrice, txtDrinkType;
    public final ImageView imgMenu;
    public final Button btnEdit, btnDelete, btnBook;

    public MenuItemViewHolder(@NonNull View itemView) {
        super(itemView);

        txtName = itemView.findViewById(R.id.txtName);
        txtPrice = itemView.findViewById(R.id.txtPrice);
        txtDrinkType = itemView.findViewById(R.id.txtDrinkType);
        imgMenu = itemView.findViewById(R.id.imgMenu);
        btnEdit = itemView.findViewById(R.id.btnEdit);
        btnDelete = itemView.findViewById(R.id.btnDelete);
        btnBook = itemView.findViewById(R.id.btnBook); // ✅ was already added
    }

    public void bind(MenuItemModel model) {
        txtName.setText(model.getName());
        txtPrice.setText("RM" + String.format("%.2f", model.getPrice()));
        txtDrinkType.setText(model.getDrink()); // ✅ changed from getDrink() to getDrinkType()

        Glide.with(itemView.getContext())
                .load(model.getImage()) // ✅ changed from getImage()
                .placeholder(R.drawable.placeholder)
                .into(imgMenu);
    }
}
