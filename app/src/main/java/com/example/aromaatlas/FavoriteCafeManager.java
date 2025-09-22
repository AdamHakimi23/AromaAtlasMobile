package com.example.aromaatlas;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;

public class FavoriteCafeManager {
    private static final String PREF_NAME = "favorite_cafes_pref";
    private static final String KEY_CAFES = "cafe_favorites";

    public static Set<String> getFavorites(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(KEY_CAFES, new HashSet<>()));
    }

    public static boolean isFavorite(Context context, String cafeId) {
        return getFavorites(context).contains(cafeId);
    }

    public static void toggleFavorite(Context context, String cafeId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> current = new HashSet<>(prefs.getStringSet(KEY_CAFES, new HashSet<>()));

        boolean isFav = current.contains(cafeId);
        if (isFav) {
            current.remove(cafeId);
        } else {
            current.add(cafeId);
        }

        prefs.edit().putStringSet(KEY_CAFES, current).apply();

        syncToFirebase(cafeId, !isFav);
    }

    private static void syncToFirebase(String cafeId, boolean add) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (add) {
            db.collection("users")
                    .document(userId)
                    .collection("favoriteCafes")
                    .document(cafeId)
                    .set(new FavoriteRecord())
                    .addOnFailureListener(e -> Log.e("FavoriteCafeSync", "Add failed", e));
        } else {
            db.collection("users")
                    .document(userId)
                    .collection("favoriteCafes")
                    .document(cafeId)
                    .delete()
                    .addOnFailureListener(e -> Log.e("FavoriteCafeSync", "Remove failed", e));
        }
    }

    public static void loadFromFirebase(Context context) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("favoriteCafes")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> favs = new HashSet<>();
                    for (var doc : snapshot) {
                        favs.add(doc.getId());
                    }

                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putStringSet(KEY_CAFES, favs)
                            .apply();
                });
    }

    private static class FavoriteRecord {
        public FieldValue timestamp = FieldValue.serverTimestamp();
    }
}
