// File: CafeAdapter.java
package com.example.aromaatlas;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CafeAdapter extends RecyclerView.Adapter<CafeAdapter.CafeViewHolder> implements Filterable {

    private final List<Cafe> cafeList;
    private final List<Cafe> cafeListFull;
    private final Map<String, Double> cafeRatingsMap = new HashMap<>();
    private final Map<String, Integer> reviewCountMap = new HashMap<>();
    private Context context;

    public CafeAdapter(List<Cafe> cafeList) {
        this.cafeList = new ArrayList<>(cafeList);
        this.cafeListFull = new ArrayList<>(cafeList);
    }

    public void setCafeRatingsAndCounts(Map<String, Double> ratingsMap, Map<String, Integer> reviewCounts) {
        cafeRatingsMap.clear();
        cafeRatingsMap.putAll(ratingsMap);
        reviewCountMap.clear();
        reviewCountMap.putAll(reviewCounts);

        Comparator<Cafe> comparator = (c1, c2) -> {
            double r1 = cafeRatingsMap.getOrDefault(c1.getCafeId(), 0.0);
            double r2 = cafeRatingsMap.getOrDefault(c2.getCafeId(), 0.0);
            return Double.compare(r2, r1);
        };

        cafeList.sort(comparator);
        cafeListFull.sort(comparator);
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public CafeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_cafe_card, parent, false);
        return new CafeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CafeViewHolder holder, int position) {
        Cafe cafe = cafeList.get(position);

        holder.nameTextView.setText(cafe.getName());
        holder.descriptionTextView.setText(cafe.getDescription());

        // ❤️ Set favorite icon based on current state
        boolean isFav = FavoriteManager.isCafeFavorite(context, cafe.getCafeId());
        holder.favoriteIcon.setImageResource(
                isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline
        );

        holder.favoriteIcon.setOnClickListener(v -> {
            FavoriteManager.toggleCafeFavorite(context, cafe.getCafeId());
            boolean updatedFav = FavoriteManager.isCafeFavorite(context, cafe.getCafeId());
            holder.favoriteIcon.setImageResource(
                    updatedFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline
            );
        });




        double rating = cafeRatingsMap.getOrDefault(cafe.getCafeId(), 0.0);
        int count = reviewCountMap.getOrDefault(cafe.getCafeId(), 0);
        if (count == 0) {
            holder.ratingTextView.setText("No reviews yet");
        } else {
            holder.ratingTextView.setText(String.format("Rating: %.1f (%d reviews)", rating, count));
        }


        String formattedDay = formatWorkingDays(cafe.getWorkingDay());
        holder.workingDayTextView.setText("Working Days: " + formattedDay);

        String workingHours = cafe.getWorkingHours();
        holder.workingHoursTextView.setText("Working Hours: " + (workingHours != null ? workingHours : "-"));

        if (isCafeOpen(workingHours)) {
            holder.statusTextView.setText("Open Now");
            holder.statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
        } else {
            holder.statusTextView.setText("Closed");
            holder.statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        }

        Glide.with(context)
                .load(cafe.getImageUrl())
                .placeholder(R.drawable.placeholder)
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CafeDetailActivity.class);
            intent.putExtra("cafeName", cafe.getName());
            intent.putExtra("description", cafe.getDescription());
            intent.putExtra("imageUrl", cafe.getImageUrl());
            intent.putExtra("workingDay", cafe.getWorkingDay());
            intent.putExtra("workingHours", cafe.getWorkingHours());
            intent.putExtra("latitude", cafe.getLatitude());
            intent.putExtra("longitude", cafe.getLongitude());
            intent.putExtra("ownerId", cafe.getOwnerId());
            intent.putExtra("cafeId", cafe.getCafeId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return cafeList.size();
    }

    @Override
    public Filter getFilter() {
        return cafeFilter;
    }

    private final Filter cafeFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Cafe> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(cafeListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Cafe cafe : cafeListFull) {
                    if (cafe.getName().toLowerCase().contains(filterPattern) ||
                            cafe.getDescription().toLowerCase().contains(filterPattern)) {
                        filteredList.add(cafe);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            cafeList.clear();
            cafeList.addAll((List<Cafe>) results.values);
            notifyDataSetChanged();
        }
    };

    public void updateData(List<Cafe> newList) {
        cafeList.clear();
        cafeList.addAll(newList);
        cafeListFull.clear();
        cafeListFull.addAll(newList);
        notifyDataSetChanged();
    }

    private boolean isCafeOpen(String workingHours) {
        try {
            if (workingHours == null || !workingHours.contains("-")) return false;
            String[] parts = workingHours.split("-");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime open = LocalTime.parse(parts[0].trim(), formatter);
            LocalTime close = LocalTime.parse(parts[1].trim(), formatter);
            LocalTime now = LocalTime.now();
            return !now.isBefore(open) && !now.isAfter(close);
        } catch (DateTimeParseException | ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    private String formatWorkingDays(String raw) {
        if (raw == null || raw.isEmpty()) return "-";

        List<String> DAYS = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

        List<String> inputDays = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(DAYS::contains)
                .sorted(Comparator.comparingInt(DAYS::indexOf))
                .collect(Collectors.toList());

        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < inputDays.size()) {
            int start = i;
            while (i + 1 < inputDays.size() &&
                    DAYS.indexOf(inputDays.get(i + 1)) == DAYS.indexOf(inputDays.get(i)) + 1) {
                i++;
            }
            if (start == i) {
                result.add(inputDays.get(i));
            } else {
                result.add(inputDays.get(start) + " - " + inputDays.get(i));
            }
            i++;
        }

        return String.join(", ", result);
    }

    public static class CafeViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, descriptionTextView, workingDayTextView,
                workingHoursTextView, statusTextView, ratingTextView;
        ImageView imageView;
        ImageView favoriteIcon;

        public CafeViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.textViewCafeName);
            workingDayTextView = itemView.findViewById(R.id.textViewWorkingDay);
            workingHoursTextView = itemView.findViewById(R.id.textViewWorkingHours);
            descriptionTextView = itemView.findViewById(R.id.textViewCafeDescription);
            statusTextView = itemView.findViewById(R.id.textViewStatus);
            ratingTextView = itemView.findViewById(R.id.textViewCafeRating);
            imageView = itemView.findViewById(R.id.imageViewCafe);
            favoriteIcon = itemView.findViewById(R.id.imageViewFavorite);

        }
    }
}
