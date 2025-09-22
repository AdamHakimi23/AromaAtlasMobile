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

public class OwnerCafeAdapter extends RecyclerView.Adapter<OwnerCafeAdapter.OwnerCafeViewHolder> {

    private List<Cafe> cafeList;

    public OwnerCafeAdapter(List<Cafe> cafeList) {
        this.cafeList = cafeList;
    }

    @NonNull
    @Override
    public OwnerCafeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_owner_cafe, parent, false);
        return new OwnerCafeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OwnerCafeViewHolder holder, int position) {
        Cafe cafe = cafeList.get(position);
        holder.name.setText(cafe.getName());
        holder.description.setText(cafe.getDescription());

        Glide.with(holder.itemView.getContext())
                .load(cafe.getImageUrl())
                .placeholder(R.drawable.placeholder)
                .into(holder.image);
    }

    @Override
    public int getItemCount() {
        return cafeList.size();
    }

    static class OwnerCafeViewHolder extends RecyclerView.ViewHolder {
        TextView name, description;
        ImageView image;

        public OwnerCafeViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.ownerCafeName);
            description = itemView.findViewById(R.id.ownerCafeDesc);
            image = itemView.findViewById(R.id.ownerCafeImage);
        }
    }
}
