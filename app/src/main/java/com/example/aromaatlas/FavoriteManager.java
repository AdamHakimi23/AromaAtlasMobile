package com.example.aromaatlas;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FavoriteManager {
    private static final String PREF_NAME = "favorites_pref";
    private static final String KEY_CAFE = "favorites_cafes";
    private static final String KEY_DRINK = "favorites_drinks";

    // === Café Methods ===

    public static Set<String> getCafeFavorites(Context context) {
        return getPrefsSet(context, KEY_CAFE);
    }

    public static boolean isCafeFavorite(Context context, String cafeId) {
        return getCafeFavorites(context).contains(cafeId);
    }

    public static void toggleCafeFavorite(Context context, String cafeId) {
        toggleLocalAndSync(context, cafeId, KEY_CAFE, "cafeFavorites");
    }

    // === Drink Methods ===

    public static Set<String> getDrinkFavorites(Context context) {
        return getPrefsSet(context, KEY_DRINK);
    }

    public static boolean isDrinkFavorite(Context context, String drinkId) {
        return getDrinkFavorites(context).contains(drinkId);
    }

    public static void toggleDrinkFavorite(Context context, String drinkId) {
        toggleLocalAndSync(context, drinkId, KEY_DRINK, "drinkFavorites");
    }

    // === Shared Helpers ===

    private static void toggleLocalAndSync(Context context, String id, String prefKey, String firebaseCollection) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> current = new HashSet<>(prefs.getStringSet(prefKey, new HashSet<>()));

        boolean isFav = current.contains(id);
        if (isFav) current.remove(id);
        else current.add(id);

        prefs.edit().putStringSet(prefKey, current).apply();
        syncToFirebase(id, !isFav, firebaseCollection);
    }

    private static Set<String> getPrefsSet(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(key, new HashSet<>()));
    }

    private static void syncToFirebase(String id, boolean add, String collection) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (add) {
            Map<String, Object> favData = new HashMap<>();
            favData.put("timestamp", FieldValue.serverTimestamp());
            db.collection("users").document(userId)
                    .collection(collection)
                    .document(id)
                    .set(favData)
                    .addOnFailureListener(e -> Log.e("FavoriteSync", "Add failed", e));
        } else {
            db.collection("users").document(userId)
                    .collection(collection)
                    .document(id)
                    .delete()
                    .addOnFailureListener(e -> Log.e("FavoriteSync", "Remove failed", e));
        }
    }

    public static void loadAllFromFirebase(Context context) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Cafes
        db.collection("users").document(userId).collection("cafeFavorites")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> favs = new HashSet<>();
                    for (var doc : snapshot) favs.add(doc.getId());
                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                            .edit().putStringSet(KEY_CAFE, favs).apply();
                });

        // Drinks
        db.collection("users").document(userId).collection("drinkFavorites")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> favs = new HashSet<>();
                    for (var doc : snapshot) favs.add(doc.getId());
                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                            .edit().putStringSet(KEY_DRINK, favs).apply();
                });
    }
}
