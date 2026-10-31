package com.example.arbibleapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

public class EmailVerificationActivity extends AppCompatActivity {
    private EmailVerificationModel model;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        public void run() { render(); handler.postDelayed(this, 1000); }
    };
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); EdgeToEdge.enable(this);
        setContentView(R.layout.activity_email_verification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        model = new ViewModelProvider(this).get(EmailVerificationModel.class);
        findViewById(R.id.btnVerificationCheck).setOnClickListener(v -> model.check());
        findViewById(R.id.btnVerificationResend).setOnClickListener(v -> model.send());
        findViewById(R.id.btnVerificationLogin).setOnClickListener(v -> model.leave());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            public void handleOnBackPressed() { model.leave(); }
        });
        model.changes.observe(this, ignored -> render());
        model.initialSend();
    }
    @Override protected void onResume() {
        super.onResume(); handler.post(tick); model.check();
    }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
    private void render() {
        if (model.destination != null) { VerifiedAccountFlow.open(this, model.destination); return; }
        ((TextView)findViewById(R.id.tvVerificationEmail)).setText(model.email());
        ((TextView)findViewById(R.id.tvVerificationStatus)).setText(model.message);
        findViewById(R.id.btnVerificationCheck).setEnabled(!model.busy);
        Button resend = findViewById(R.id.btnVerificationResend);
        long seconds = model.remainingSeconds();
        resend.setEnabled(!model.busy && seconds == 0);
        resend.setText(seconds > 0 ? getString(R.string.verification_resend_wait, seconds) : getString(R.string.verification_resend));
    }
}
