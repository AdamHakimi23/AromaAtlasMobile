package com.example.aromaatlas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CafeProfileAdapter extends RecyclerView.Adapter<CafeProfileAdapter.CafeViewHolder> {

    private Context context;
    private List<Cafe> cafeList;

    public CafeProfileAdapter(Context context, List<Cafe> cafeList) {
        this.context = context;
        this.cafeList = cafeList;
    }

    @NonNull
    @Override
    public CafeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cafe_card_profile, parent, false);
        return new CafeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CafeViewHolder holder, int position) {
        Cafe cafe = cafeList.get(position);
        holder.cafeName.setText(cafe.getName());
        holder.cafeDescription.setText(cafe.getDescription());

        Glide.with(context)
                .load(cafe.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .into(holder.cafeImage);
    }

    @Override
    public int getItemCount() {
        return cafeList.size();
    }

    public static class CafeViewHolder extends RecyclerView.ViewHolder {
        TextView cafeName, cafeDescription;
        ImageView cafeImage;

        public CafeViewHolder(@NonNull View itemView) {
            super(itemView);
            cafeName = itemView.findViewById(R.id.textCafeName);
            cafeDescription = itemView.findViewById(R.id.textCafeDescription);
            cafeImage = itemView.findViewById(R.id.imageCafe);
        }
    }
}
