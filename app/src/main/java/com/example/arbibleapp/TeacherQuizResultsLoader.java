package com.example.arbibleapp;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TeacherQuizResultsLoader {
    private static final int MAX_STUDENT_UIDS_PER_QUERY = 30;

    private TeacherQuizResultsLoader() {}

    static class ResultData {
        public List<DocumentSnapshot> documents;
        public Map<String, String> userNames;
    }

    static Task<ResultData> fetchForStudents(
            FirebaseFirestore db, List<String> studentUids) {
        Set<String> uniqueUids = new LinkedHashSet<>();
        if (studentUids != null) {
            for (String studentUid : studentUids) {
                if (studentUid != null && !studentUid.trim().isEmpty()) {
                    uniqueUids.add(studentUid);
                }
            }
        }

        if (uniqueUids.isEmpty()) {
            ResultData empty = new ResultData();
            empty.documents = Collections.emptyList();
            empty.userNames = Collections.emptyMap();
            return Tasks.forResult(empty);
        }

        List<String> uids = new ArrayList<>(uniqueUids);
        List<Task<QuerySnapshot>> requests = new ArrayList<>();
        List<Task<DocumentSnapshot>> userRequests = new ArrayList<>();
        
        for (int start = 0; start < uids.size(); start += MAX_STUDENT_UIDS_PER_QUERY) {
            int end = Math.min(start + MAX_STUDENT_UIDS_PER_QUERY, uids.size());
            requests.add(db.collection("quiz_results")
                    .whereIn("studentUid", uids.subList(start, end))
                    .get());
        }

        for (String uid : uids) {
            userRequests.add(db.collection("users").document(uid).get());
        }

        Task<List<Object>> allQuizResultsTask = Tasks.whenAllSuccess(requests);
        Task<List<Object>> allUsersTask = Tasks.whenAllSuccess(userRequests);

        return Tasks.whenAllSuccess(allQuizResultsTask, allUsersTask).continueWith(task -> {
            ResultData data = new ResultData();
            data.documents = new ArrayList<>();
            data.userNames = new HashMap<>();

            List<Object> results = task.getResult();
            if (results.size() >= 2) {
                List<Object> quizSnapshots = (List<Object>) results.get(0);
                for (Object qs : quizSnapshots) {
                    data.documents.addAll(((QuerySnapshot) qs).getDocuments());
                }

                List<Object> userSnapshots = (List<Object>) results.get(1);
                for (Object us : userSnapshots) {
                    DocumentSnapshot userDoc = (DocumentSnapshot) us;
                    if (userDoc.exists()) {
                        String username = userDoc.getString("username");
                        if (username == null || username.isEmpty()) {
                            username = userDoc.getString("fullName");
                        }
                        if (username != null && !username.isEmpty()) {
                            data.userNames.put(userDoc.getId(), username);
                        }
                    }
                }
            }
            return data;
        });
    }
}