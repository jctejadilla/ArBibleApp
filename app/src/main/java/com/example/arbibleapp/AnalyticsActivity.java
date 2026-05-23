package com.example.arbibleapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsActivity extends AppCompatActivity {

    private LineChart lineChartXP;
    private BarChart barChartPerformance, barChartAttendance;
    private PieChart pieChartEngagement;
    private RecyclerView rvStoryCompletion;
    private StoryCompletionAdapter completionAdapter;
    private List<StoryCompletionModel> completionList = new ArrayList<>();
    private TextView tvXPCycle, tvResetsIn;
    private TextView tvAvgXPValue, tvAvgQuizValue, tvAvgAttendanceValue, tvAvgSessionValue;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    
    private final List<String> studentUids = new ArrayList<>();

    private com.google.firebase.firestore.ListenerRegistration resultsListener, sessionsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_analytics);
        
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupNavigation();
        fetchClassData();
    }

    private void initViews() {
        lineChartXP = findViewById(R.id.lineChartXP);
        barChartPerformance = findViewById(R.id.barChartPerformance);
        barChartAttendance = findViewById(R.id.barChartAttendance);
        pieChartEngagement = findViewById(R.id.pieChartEngagement);
        rvStoryCompletion = findViewById(R.id.rvStoryCompletion);
        tvXPCycle = findViewById(R.id.tvXPCycle);
        tvResetsIn = findViewById(R.id.tvResetsIn);

        tvAvgXPValue = findViewById(R.id.tvAvgXPValue);
        tvAvgQuizValue = findViewById(R.id.tvAvgQuizValue);
        tvAvgAttendanceValue = findViewById(R.id.tvAvgAttendanceValue);
        tvAvgSessionValue = findViewById(R.id.tvAvgSessionValue);

        setupLineChart();
        setupBarChart();
        setupPieChart();
        setupAttendanceChart();

        rvStoryCompletion.setLayoutManager(new LinearLayoutManager(this));
        completionAdapter = new StoryCompletionAdapter(completionList);
        rvStoryCompletion.setAdapter(completionAdapter);
    }

    private void setupNavigation() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, TeacherDashboard.class));
            finish();
        });
        findViewById(R.id.navStudentsTab).setOnClickListener(v -> {
            startActivity(new Intent(this, ClassManagement.class));
            finish();
        });
        findViewById(R.id.navAttendanceTab).setOnClickListener(v -> {
            startActivity(new Intent(this, attendance_teacher_main.class));
            finish();
        });
    }

    private void setupLineChart() {
        lineChartXP.getDescription().setEnabled(false);
        lineChartXP.setNoDataText("Loading data...");
        lineChartXP.setDrawGridBackground(false);
        lineChartXP.setDrawBorders(false);
        lineChartXP.setTouchEnabled(true);

        XAxis xAxis = lineChartXP.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#EEEEEE"));
        xAxis.setGridLineWidth(1f);
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Week 1", "Week 2", "Week 3", "Week 4"}));
        xAxis.setGranularity(1f);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(Color.GRAY);

        YAxis leftAxis = lineChartXP.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(2000f);
        leftAxis.setLabelCount(5, true);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(Color.GRAY);

        lineChartXP.getAxisRight().setEnabled(false);
        lineChartXP.getLegend().setEnabled(false);
        
        loadEmptyXPData();
    }

    private void setupBarChart() {
        barChartPerformance.getDescription().setEnabled(false);
        barChartPerformance.setNoDataText("Loading data...");
        barChartPerformance.setDrawGridBackground(false);
        barChartPerformance.getAxisRight().setEnabled(false);
        barChartPerformance.setDrawBorders(false);
        
        XAxis xAxis = barChartPerformance.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(Color.GRAY);
        
        YAxis leftAxis = barChartPerformance.getAxisLeft();
        leftAxis.setAxisMaximum(100f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setLabelCount(5, true);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setTextColor(Color.GRAY);

        barChartPerformance.getLegend().setEnabled(false);
    }

    private void setupPieChart() {
        pieChartEngagement.setUsePercentValues(true);
        pieChartEngagement.setNoDataText("Loading data...");
        pieChartEngagement.getDescription().setEnabled(false);
        pieChartEngagement.setExtraOffsets(5, 10, 5, 5);
        pieChartEngagement.setDragDecelerationFrictionCoef(0.95f);
        pieChartEngagement.setDrawHoleEnabled(true);
        pieChartEngagement.setHoleColor(Color.WHITE);
        pieChartEngagement.setTransparentCircleRadius(61f);
        pieChartEngagement.setEntryLabelColor(Color.BLACK);
        pieChartEngagement.setEntryLabelTextSize(12f);
    }

    private void loadEmptyXPData() {
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 4; i++) entries.add(new Entry(i, 0f));
        LineDataSet set = new LineDataSet(entries, "");
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setColor(Color.TRANSPARENT);
        lineChartXP.setData(new LineData(set));
        lineChartXP.invalidate();
    }

    private void fetchClassData() {
        if (mAuth.getCurrentUser() == null) return;
        String teacherUid = mAuth.getCurrentUser().getUid();

        db.collection("enrollments").whereEqualTo("teacherUid", teacherUid).get()
                .addOnSuccessListener(enrollments -> {
                    studentUids.clear();
                    for (DocumentSnapshot doc : enrollments) {
                        String uid = doc.getString("studentUid");
                        if (uid != null) studentUids.add(uid.trim());
                    }
                    
                    if (!studentUids.isEmpty()) {
                        loadXPData();
                        loadQuizData();
                        loadEngagementData();
                        loadAttendanceData();
                        loadStoryCompletionData();
                    } else {
                        Toast.makeText(this, "No students enrolled in your class.", Toast.LENGTH_SHORT).show();
                        barChartPerformance.setNoDataText("No students enrolled");
                        pieChartEngagement.setNoDataText("No students enrolled");
                        barChartPerformance.invalidate();
                        pieChartEngagement.invalidate();
                    }
                });
    }

    private void loadXPData() {
        Calendar cal = Calendar.getInstance();
        String currentMonthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        int year = cal.get(Calendar.YEAR);
        tvXPCycle.setText(String.format(Locale.getDefault(), "%s %d - Current Cycle", currentMonthName, year));

        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        int daysLeft = lastDay - currentDay;
        tvResetsIn.setText(String.format(Locale.getDefault(), "Resets in %d days", daysLeft));

        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        com.google.firebase.Timestamp startOfMonth = new com.google.firebase.Timestamp(cal.getTime());

        db.collection("quiz_results")
                .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
                .get()
                .addOnSuccessListener(results -> {
                    long[] weeklyXP = new long[4];
                    boolean dataFound = false;
                    long totalXPAccumulator = 0;

                    for (DocumentSnapshot doc : results) {
                        String sUid = doc.getString("studentUid");
                        if (sUid != null && studentUids.contains(sUid)) {
                            Long xp = doc.getLong("xp");
                            com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
                            if (xp != null) {
                                totalXPAccumulator += xp;
                                if (ts != null) {
                                    Calendar resultCal = Calendar.getInstance();
                                    resultCal.setTime(ts.toDate());
                                    int day = resultCal.get(Calendar.DAY_OF_MONTH);
                                    
                                    int weekIndex;
                                    if (day <= 7) weekIndex = 0;
                                    else if (day <= 14) weekIndex = 1;
                                    else if (day <= 21) weekIndex = 2;
                                    else weekIndex = 3;

                                    weeklyXP[weekIndex] += xp;
                                    dataFound = true;
                                }
                            }
                        }
                    }

                    if (!studentUids.isEmpty()) {
                        long avgXP = totalXPAccumulator / studentUids.size();
                        tvAvgXPValue.setText(String.format(Locale.getDefault(), "%,d", avgXP));
                    }

                    if (!dataFound) {
                        lineChartXP.setNoDataText("No XP recorded this month yet");
                        lineChartXP.setData(null);
                        lineChartXP.invalidate();
                        return;
                    }

                    ArrayList<Entry> entries = new ArrayList<>();
                    float highestXP = 0;
                    for (int i = 0; i < 4; i++) {
                        float val = (float) weeklyXP[i];
                        entries.add(new Entry(i, val));
                        if (val > highestXP) highestXP = val;
                    }

                    LineDataSet set1 = new LineDataSet(entries, "Class XP");
                    set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    set1.setColor(Color.parseColor("#8CB6A3"));
                    set1.setLineWidth(3f);
                    set1.setCircleColor(Color.parseColor("#8CB6A3"));
                    set1.setCircleRadius(5f);
                    set1.setDrawValues(true);
                    set1.setValueTextColor(Color.GRAY);
                    set1.setDrawFilled(true);
                    set1.setFillColor(Color.parseColor("#8CB6A3"));
                    set1.setFillAlpha(30);

                    LineData data = new LineData(set1);
                    lineChartXP.setData(data);
                    
                    YAxis leftAxis = lineChartXP.getAxisLeft();
                    leftAxis.setAxisMaximum(Math.max(2000f, highestXP + 500f));
                    
                    lineChartXP.animateX(1000);
                });
    }

    private void loadQuizData() {
        db.collection("quiz_results")
                .get()
                .addOnSuccessListener(results -> {
                    Map<String, List<Double>> storyScores = new HashMap<>();
                    double totalPercentSum = 0;
                    long totalQuizCount = 0;
                    long totalDurationSeconds = 0;
                    long durationCount = 0;

                    for (DocumentSnapshot doc : results) {
                        String sUid = doc.getString("studentUid");
                        if (sUid != null && studentUids.contains(sUid)) {
                            String story = doc.getString("storyTitle");
                            Long score = doc.getLong("score");
                            Long total = doc.getLong("totalQuestions");
                            if (story != null && score != null && total != null && total > 0) {
                                double percent = (score.doubleValue() / total.doubleValue()) * 100.0;
                                if (!storyScores.containsKey(story)) storyScores.put(story, new ArrayList<>());
                                storyScores.get(story).add(percent);
                                
                                totalPercentSum += percent;
                                totalQuizCount++;
                            }

                            String dur = doc.getString("duration");
                            if (dur != null && dur.contains("m") && dur.contains("s")) {
                                try {
                                    String[] parts = dur.split(" ");
                                    int m = Integer.parseInt(parts[0].replace("m", ""));
                                    int s = Integer.parseInt(parts[1].replace("s", ""));
                                    totalDurationSeconds += (m * 60L) + s;
                                    durationCount++;
                                } catch (Exception ignored) {}
                            }
                        }
                    }

                    if (totalQuizCount > 0) {
                        int avgScore = (int) (totalPercentSum / totalQuizCount);
                        tvAvgQuizValue.setText(avgScore + "%");
                    }

                    if (durationCount > 0) {
                        long avgSec = totalDurationSeconds / durationCount;
                        if (avgSec < 60) {
                            tvAvgSessionValue.setText(avgSec + "s");
                        } else {
                            long avgMin = avgSec / 60;
                            tvAvgSessionValue.setText(avgMin + "m");
                        }
                    }

                    if (storyScores.isEmpty()) {
                        barChartPerformance.setNoDataText("No quiz results yet");
                        barChartPerformance.setData(null);
                        barChartPerformance.invalidate();
                        return;
                    }

                    ArrayList<BarEntry> entries = new ArrayList<>();
                    ArrayList<String> labels = new ArrayList<>();
                    int idx = 0;
                    for (String story : storyScores.keySet()) {
                        List<Double> scores = storyScores.get(story);
                        if (scores != null) {
                            double sum = 0;
                            for (double s : scores) sum += s;
                            entries.add(new BarEntry(idx, (float) (sum / scores.size())));
                            String shortName = story.length() > 10 ? story.substring(0, 10) + ".." : story;
                            labels.add(shortName);
                            idx++;
                        }
                    }

                    BarDataSet set = new BarDataSet(entries, "");
                    set.setColor(Color.parseColor("#8CB6A3"));
                    set.setDrawValues(true);
                    set.setValueTextColor(Color.GRAY);

                    BarData data = new BarData(set);
                    data.setBarWidth(0.6f);
                    barChartPerformance.setData(data);
                    barChartPerformance.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                    barChartPerformance.animateY(1000);
                });
    }

    private void loadEngagementData() {
        db.collection("quiz_results")
                .get()
                .addOnSuccessListener(results -> {
                    Map<String, List<DocumentSnapshot>> studentResults = new HashMap<>();
                    for (DocumentSnapshot doc : results) {
                        String sUid = doc.getString("studentUid");
                        if (sUid != null && studentUids.contains(sUid)) {
                            if (!studentResults.containsKey(sUid)) studentResults.put(sUid, new ArrayList<>());
                            studentResults.get(sUid).add(doc);
                        }
                    }

                    int high = 0, moderate = 0, low = 0;
                    boolean hasData = false;

                    for (String uid : studentUids) {
                        List<DocumentSnapshot> sRes = studentResults.get(uid);
                        double scoreComponent = 0;
                        double timeComponent = 0;
                        double interactionComponent = 0;

                        if (sRes != null && !sRes.isEmpty()) {
                            hasData = true;
                            double totalPerc = 0;
                            long totalSec = 0;
                            for (DocumentSnapshot d : sRes) {
                                Long s = d.getLong("score");
                                Long t = d.getLong("totalQuestions");
                                if (s != null && t != null && t > 0) totalPerc += (s.doubleValue() / t.doubleValue()) * 100;
                                
                                String dur = d.getString("duration");
                                if (dur != null && dur.contains("m") && dur.contains("s")) {
                                    try {
                                        String[] parts = dur.split(" ");
                                        int m = Integer.parseInt(parts[0].replace("m", ""));
                                        int s_ = Integer.parseInt(parts[1].replace("s", ""));
                                        totalSec += (m * 60L) + s_;
                                    } catch (Exception ignored) {}
                                }
                            }
                            scoreComponent = (totalPerc / sRes.size()) * 0.4;
                            timeComponent = Math.min(100.0, (totalSec / (double)sRes.size()) / 6.0) * 0.3;
                            interactionComponent = Math.min(100.0, sRes.size() * 10.0) * 0.3;
                        }

                        double totalEngagement = scoreComponent + timeComponent + interactionComponent;
                        if (sRes != null && !sRes.isEmpty()) {
                            if (totalEngagement >= 80) high++;
                            else if (totalEngagement >= 50) moderate++;
                            else low++;
                        }
                    }

                    if (!hasData) {
                        pieChartEngagement.setNoDataText("No engagement data yet");
                        pieChartEngagement.setData(null);
                        pieChartEngagement.invalidate();
                        return;
                    }

                    ArrayList<PieEntry> pieEntries = new ArrayList<>();
                    if (high > 0) pieEntries.add(new PieEntry(high, "High"));
                    if (moderate > 0) pieEntries.add(new PieEntry(moderate, "Moderate"));
                    if (low > 0) pieEntries.add(new PieEntry(low, "Low"));

                    PieDataSet dataSet = new PieDataSet(pieEntries, "");
                    dataSet.setColors(
                        Color.parseColor("#1D4A4B"),
                        Color.parseColor("#8CB6A3"),
                        Color.parseColor("#C85F5F")
                    );
                    dataSet.setSliceSpace(3f);
                    
                    PieData data = new PieData(dataSet);
                    data.setValueFormatter(new PercentFormatter(pieChartEngagement));
                    data.setValueTextSize(12f);
                    data.setValueTextColor(Color.WHITE);
                    pieChartEngagement.setData(data);
                    pieChartEngagement.animateY(1000);
                });
    }

    private void setupAttendanceChart() {
        barChartAttendance.getDescription().setEnabled(false);
        barChartAttendance.setNoDataText("Loading data...");
        barChartAttendance.setDrawGridBackground(false);
        barChartAttendance.setDrawBarShadow(false);
        barChartAttendance.setDrawValueAboveBar(true);

        XAxis xAxis = barChartAttendance.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setTextColor(Color.GRAY);

        YAxis leftAxis = barChartAttendance.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.GRAY);

        barChartAttendance.getAxisRight().setEnabled(false);
        barChartAttendance.getLegend().setEnabled(true);
        barChartAttendance.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        barChartAttendance.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        barChartAttendance.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        barChartAttendance.getLegend().setDrawInside(false);
    }

    private void loadAttendanceData() {
        List<String> sundays = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat labelSdf = new SimpleDateFormat("MMM d", Locale.getDefault());
        List<String> labels = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        // Go back to the most recent Sunday
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }

        // Get the last 4 Sundays
        for (int i = 0; i < 4; i++) {
            sundays.add(0, sdf.format(cal.getTime()));
            labels.add(0, labelSdf.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_MONTH, -7);
        }

        db.collection("attendance")
                .whereIn("date", sundays)
                .get()
                .addOnSuccessListener(results -> {
                    Map<String, Integer> presentMap = new HashMap<>();
                    for (String s : sundays) presentMap.put(s, 0);

                    for (DocumentSnapshot doc : results) {
                        String sUid = doc.getString("studentUid");
                        String date = doc.getString("date");
                        if (sUid != null && studentUids.contains(sUid) && date != null) {
                            Integer current = presentMap.get(date);
                            if (current != null) {
                                presentMap.put(date, current + 1);
                            }
                        }
                    }

                    ArrayList<BarEntry> presentEntries = new ArrayList<>();
                    ArrayList<BarEntry> absentEntries = new ArrayList<>();

                    int totalStudents = studentUids.size();

                    for (int i = 0; i < sundays.size(); i++) {
                        Integer p = presentMap.get(sundays.get(i));
                        int present = p != null ? p : 0;
                        int absent = Math.max(0, totalStudents - present);
                        
                        presentEntries.add(new BarEntry(i, (float) present));
                        absentEntries.add(new BarEntry(i, (float) absent));
                    }

                    BarDataSet set1 = new BarDataSet(presentEntries, "present");
                    set1.setColor(Color.parseColor("#00C853")); // Green
                    set1.setDrawValues(false);

                    BarDataSet set2 = new BarDataSet(absentEntries, "absent");
                    set2.setColor(Color.parseColor("#EF5350")); // Red
                    set2.setDrawValues(false);

                    BarData data = new BarData(set1, set2);
                    float groupSpace = 0.4f;
                    float barSpace = 0.05f;
                    float barWidth = 0.25f;

                    // Calculate Average Attendance for summary card
                    int totalPresent = 0;
                    for (int p : presentMap.values()) totalPresent += p;
                    int totalPossible = studentUids.size() * sundays.size();
                    if (totalPossible > 0) {
                        tvAvgAttendanceValue.setText(totalPresent + "/" + totalPossible);
                    }

                    data.setBarWidth(barWidth);
                    barChartAttendance.setData(data);
                    barChartAttendance.groupBars(0, groupSpace, barSpace);
                    
                    XAxis xAxis = barChartAttendance.getXAxis();
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                    xAxis.setAxisMinimum(0);
                    xAxis.setAxisMaximum(labels.size());
                    xAxis.setCenterAxisLabels(true);
                    
                    barChartAttendance.getAxisLeft().setAxisMaximum(Math.max(10, totalStudents + 5));
                    barChartAttendance.invalidate();
                    barChartAttendance.animateY(1000);
                });
    }

    private void loadStoryCompletionData() {
        String[] stories = {
            "Creation and the Fall",
            "Early Humanity and the Flood",
            "The Tower of Babel",
            "Slavery in Egypt and the Exodus",
            "The Law and Wandering in the Desert"
        };
        
        completionList.clear();
        for (int i = 0; i < stories.length; i++) {
            completionList.add(new StoryCompletionModel(stories[i], "", studentUids.size()));
        }
        completionAdapter.notifyDataSetChanged();

        if (resultsListener != null) resultsListener.remove();
        if (sessionsListener != null) sessionsListener.remove();

        resultsListener = db.collection("quiz_results").addSnapshotListener((results, e) -> {
            if (e != null) {
                return;
            }
            if (results == null) return;
            
            for (StoryCompletionModel model : completionList) {
                model.completedUids.clear();
            }

            for (DocumentSnapshot doc : results) {
                String sUid = doc.getString("studentUid");
                String story = doc.getString("storyTitle");
                
                if (sUid != null && story != null) {
                    sUid = sUid.trim();
                    story = story.trim();
                    
                    if (studentUids.contains(sUid)) {
                        for (StoryCompletionModel model : completionList) {
                            if (model.title.equalsIgnoreCase(story)) {
                                if (!model.completedUids.contains(sUid)) {
                                    model.completedUids.add(sUid);
                                }
                                break;
                            }
                        }
                    }
                }
            }
            refreshProgressUI();
        });

        sessionsListener = db.collection("quiz_sessions").addSnapshotListener((sessions, e) -> {
            if (e != null) {
                return;
            }
            if (sessions == null) return;

            for (StoryCompletionModel model : completionList) {
                model.inProgressUids.clear();
            }

            for (DocumentSnapshot doc : sessions) {
                String sUid = doc.getId().trim(); // Student UID is the document ID
                String story = doc.getString("storyTitle");
                
                if (story != null) {
                    story = story.trim();

                    boolean isMyStudent = false;
                    for (String enrolledUid : studentUids) {
                        if (enrolledUid.equalsIgnoreCase(sUid)) {
                            isMyStudent = true;
                            break;
                        }
                    }

                    if (isMyStudent) {
                        for (StoryCompletionModel model : completionList) {
                            if (model.title.equalsIgnoreCase(story)) {
                                if (!model.inProgressUids.contains(sUid)) {
                                    model.inProgressUids.add(sUid);
                                }
                                break;
                            }
                        }
                    }
                }
            }
            refreshProgressUI();
        });
    }

    private void refreshProgressUI() {
        completionAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resultsListener != null) resultsListener.remove();
        if (sessionsListener != null) sessionsListener.remove();
    }

    private static class StoryCompletionModel {
        String title, subtitle;
        int totalStudents;
        List<String> completedUids = new ArrayList<>();
        List<String> inProgressUids = new ArrayList<>();

        StoryCompletionModel(String title, String subtitle, int total) {
            this.title = title;
            this.subtitle = subtitle;
            this.totalStudents = total;
        }

        int getCompleted() {
            return completedUids.size();
        }

        int getInProgress() {
            int count = 0;
            for (String uid : inProgressUids) {
                boolean alreadyCompleted = false;
                for (String cUid : completedUids) {
                    if (cUid.equalsIgnoreCase(uid)) {
                        alreadyCompleted = true;
                        break;
                    }
                }
                if (!alreadyCompleted) {
                    count++;
                }
            }
            return count;
        }

        int getNotStarted() {
            return Math.max(0, totalStudents - getCompleted() - getInProgress());
        }
    }

    private class StoryCompletionAdapter extends RecyclerView.Adapter<StoryCompletionAdapter.ViewHolder> {
        List<StoryCompletionModel> list;
        StoryCompletionAdapter(List<StoryCompletionModel> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story_completion, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StoryCompletionModel m = list.get(position);
            holder.title.setText(m.title);
            holder.total.setText(m.totalStudents + " total");
            
            int compCount = m.getCompleted();
            int progCount = m.getInProgress();
            int progTotal = m.inProgressUids.size(); // Show ALL currently active students
            int startCount = m.getNotStarted();

            holder.tvComp.setText(compCount + " completed");
            holder.tvProg.setText(progTotal + " in progress");
            holder.tvStart.setText(startCount + " not started");

            if (progTotal > 0) {
                holder.tvProg.setTextColor(Color.parseColor("#FFB300"));
                holder.tvProg.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                holder.tvProg.setTextColor(Color.GRAY);
                holder.tvProg.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            if (m.totalStudents > 0) {
                holder.layoutProgressBar.setWeightSum((float) m.totalStudents);
                
                holder.vComp.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) compCount));
                holder.vProg.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) progCount));
                holder.vStart.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) startCount));
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, total, tvComp, tvProg, tvStart;
            View vComp, vProg, vStart;
            LinearLayout layoutProgressBar;
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.tvStoryTitle);
                total = v.findViewById(R.id.tvTotalCount);
                tvComp = v.findViewById(R.id.tvCompletedCount);
                tvProg = v.findViewById(R.id.tvInProgressCount);
                tvStart = v.findViewById(R.id.tvNotStartedCount);
                vComp = v.findViewById(R.id.viewCompleted);
                vProg = v.findViewById(R.id.viewInProgress);
                vStart = v.findViewById(R.id.viewNotStarted);
                layoutProgressBar = v.findViewById(R.id.layoutProgressBar);
            }
        }
    }
}
