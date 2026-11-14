package com.example.arbibleapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OfflineQuizManager {
    private static final String PREF_NAME = "OfflineQuizPrefs";
    private static final String KEY_PENDING_QUIZZES = "pending_quizzes";

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    public static void saveQuizOffline(Context context, Map<String, Object> quizData) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String existingData = prefs.getString(KEY_PENDING_QUIZZES, "[]");
        try {
            JSONArray array = new JSONArray(existingData);
            JSONObject newQuiz = new JSONObject(quizData);
            
            if (quizData.containsKey("timestamp")) {
                 Timestamp ts = (Timestamp) quizData.get("timestamp");
                 newQuiz.put("timestamp_seconds", ts.getSeconds());
                 newQuiz.put("timestamp_nanos", ts.getNanoseconds());
                 newQuiz.remove("timestamp");
            }
            array.put(newQuiz);
            prefs.edit().putString(KEY_PENDING_QUIZZES, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void syncOfflineQuizzes(Context context) {
        if (!isNetworkAvailable(context)) return;
        
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String pending = prefs.getString(KEY_PENDING_QUIZZES, "[]");
        
        try {
            JSONArray array = new JSONArray(pending);
            if (array.length() == 0) return;
            
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Map<String, Object> data = new HashMap<>();
                
                Iterator<String> keys = obj.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    if (key.equals("timestamp_seconds") || key.equals("timestamp_nanos")) continue;
                    
                    if (key.equals("wrongAnswers") || key.equals("correctAnswers") || key.equals("selectedAnswers")) {
                        JSONArray listArr = obj.getJSONArray(key);
                        List<String> list = new ArrayList<>();
                        for(int j=0; j<listArr.length(); j++) list.add(listArr.getString(j));
                        data.put(key, list);
                    } else {
                        data.put(key, obj.get(key));
                    }
                }
                
                if (obj.has("timestamp_seconds") && obj.has("timestamp_nanos")) {
                    data.put("timestamp", new Timestamp(obj.getLong("timestamp_seconds"), obj.getInt("timestamp_nanos")));
                } else {
                    data.put("timestamp", Timestamp.now());
                }
                
                db.collection("quiz_results").add(data);
                
                if (data.containsKey("isSunday") && (Boolean)data.get("isSunday")) {
                     String uid = (String) data.get("studentUid");
                     int xp = obj.has("xp") ? obj.getInt("xp") : 0;
                     if (uid != null && xp > 0) {
                         db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                             if (doc.exists()) {
                                 long currentXP = doc.contains("totalXP") ? doc.getLong("totalXP") : 0;
                                 long newXP = currentXP + xp;
                                 int newLevel = (int) (newXP / 250) + 1;
                                 if (newLevel > 10) newLevel = 10;
                                 Map<String, Object> updates = new HashMap<>();
                                 updates.put("totalXP", newXP);
                                 updates.put("level", newLevel);
                                 db.collection("users").document(uid).update(updates);
                             }
                         });
                     }
                }
            }
            
            prefs.edit().putString(KEY_PENDING_QUIZZES, "[]").apply();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
