package com.example.arbibleapp;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class EmailVerificationTest {
    Application app;
    SharedPreferences prefs;
    Fake backend;
    EmailVerificationModel model;
    static final class Fake implements EmailVerificationModel.Backend {
        String uid = "verification-test";
        int sends, checks;
        TaskCompletionSource<Void> send = new TaskCompletionSource<>();
        TaskCompletionSource<Class<?>> check = new TaskCompletionSource<>();
        public String uid() { return uid; }
        public String email() { return "test@example.com"; }
        public Task<Void> send() { sends++; return send.getTask(); }
        public Task<Class<?>> check() { checks++; return check.getTask(); }
        public void signOut() { uid = null; }
    }
    void main(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    @Before public void setup() {
        app = ApplicationProvider.getApplicationContext();
        prefs = app.getSharedPreferences("verification-test-only", 0); prefs.edit().clear().commit();
        backend = new Fake(); main(() -> model = new EmailVerificationModel(app, backend, prefs));
    }
    @After public void cleanup() { prefs.edit().clear().commit(); }
    @Test public void initialEmailIsSentOnceAndCooldownSurvivesNewModel() {
        main(() -> { model.initialSend(); model.initialSend(); model.send(); model.check(); });
        assertEquals(1, backend.sends); assertEquals(0, backend.checks);
        main(() -> backend.send.setResult(null));
        assertTrue(model.remainingSeconds() > 0);
        main(() -> { model = new EmailVerificationModel(app, backend, prefs); model.initialSend(); model.send(); });
        assertEquals(1, backend.sends);
        prefs.edit().putLong("sent-" + backend.uid, System.currentTimeMillis() - 61000).commit();
        backend.send = new TaskCompletionSource<>(); main(() -> model.send()); assertEquals(2, backend.sends);
    }
    @Test public void failedEmailCanBeRetriedWithoutAutomaticResendOnRecreation() {
        main(() -> model.initialSend()); main(() -> backend.send.setException(new Exception("offline")));
        assertFalse(model.busy); assertTrue(model.message.contains("Could not send"));
        main(() -> { model = new EmailVerificationModel(app, backend, prefs); model.initialSend(); });
        assertEquals(1, backend.sends);
        backend.send = new TaskCompletionSource<>(); main(() -> model.send()); assertEquals(2, backend.sends);
    }
    @Test public void unverifiedAndNetworkFailureStayOnScreenAndAllowRetry() {
        main(() -> { model.check(); model.check(); }); assertEquals(1, backend.checks);
        main(() -> backend.check.setResult(EmailVerificationActivity.class));
        assertNull(model.destination); assertFalse(model.busy); assertTrue(model.message.contains("not verified"));
        backend.check = new TaskCompletionSource<>(); main(() -> model.check());
        main(() -> backend.check.setException(new Exception("offline")));
        assertNull(model.destination); assertFalse(model.busy); assertTrue(model.message.contains("connection"));
    }
    @Test public void allVerifiedRolesNavigateOnlyOnceAndInvalidRoleFails() {
        for (String role : Arrays.asList("Student", "Teacher", "Admin")) {
            backend = new Fake();
            main(() -> { model = new EmailVerificationModel(app, backend, prefs); model.check(); });
            Class<?> destination = VerifiedAccountFlow.dashboard(role);
            main(() -> backend.check.setResult(destination));
            assertEquals(destination, model.destination);
            main(() -> { model.check(); model.send(); }); assertEquals(1, backend.checks); assertEquals(0, backend.sends);
        }
        try { VerifiedAccountFlow.dashboard(null); fail("Missing roles must not default to Student"); }
        catch (IllegalStateException expected) { }
    }
    @Test public void logoutIgnoresLateSuccessAndExpiredSessionReturnsToLogin() {
        main(() -> { model.check(); model.leave(); });
        main(() -> backend.check.setResult(StudentDashboard.class));
        assertEquals(login.class, model.destination); assertNull(backend.uid);
        backend = new Fake(); main(() -> { model = new EmailVerificationModel(app, backend, prefs); model.check(); });
        main(() -> backend.check.setException(new FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "disabled")));
        assertEquals(login.class, model.destination); assertNull(backend.uid);
    }
    @Test public void missingSessionReturnsToLoginWithoutSending() {
        backend.uid = null; main(() -> model.initialSend());
        assertEquals(login.class, model.destination); assertEquals(0, backend.sends);
    }
    @Test public void profileFailureRetriesWithoutCreatingAnotherAccountAndSurvivesNewModel() {
        final class Registration implements RegistrationModel.Backend {
            String uid; int creates, saves; Map<String,Object> saved;
            TaskCompletionSource<String> create = new TaskCompletionSource<>();
            TaskCompletionSource<Void> save = new TaskCompletionSource<>();
            public String uid() { return uid; }
            public Task<String> create(String email, String password) { creates++; return create.getTask(); }
            public Task<Void> save(String uid, Map<String,Object> data) { saves++; saved = data; return save.getTask(); }
        }
        Registration service = new Registration(); RegistrationModel[] registration = new RegistrationModel[1];
        main(() -> {
            registration[0] = new RegistrationModel(app, service, prefs);
            registration[0].register("Test", "test@gmail.com", "secret", "Male", "Teacher");
            registration[0].register("Test", "test@gmail.com", "secret", "Male", "Teacher");
        });
        assertEquals(1, service.creates);
        main(() -> { service.uid = "new-account"; service.create.setResult(service.uid); });
        assertEquals(1, service.saves); assertFalse(service.saved.containsKey("password"));
        main(() -> service.save.setException(new Exception("offline")));
        assertTrue(registration[0].hasPending()); assertFalse(registration[0].complete);
        service.save = new TaskCompletionSource<>();
        main(() -> { registration[0] = new RegistrationModel(app, service, prefs); registration[0].retry(); });
        assertEquals(1, service.creates); assertEquals(2, service.saves); assertEquals("Teacher", service.saved.get("userType"));
        main(() -> service.save.setResult(null));
        assertTrue(registration[0].complete); assertFalse(registration[0].hasPending());
    }
    @Test public void signedOutDashboardAndVerificationEntryReturnToLogin() {
        org.junit.Assume.assumeTrue(com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null);
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        for (Class<?> screen : Arrays.asList(StudentDashboard.class, TeacherDashboard.class, AdminDashboard.class, EmailVerificationActivity.class)) {
            android.app.Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(login.class.getName(), null, false);
            try {
                app.startActivity(new android.content.Intent(app, screen).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK));
                android.app.Activity destination = instrumentation.waitForMonitorWithTimeout(monitor, 10000);
                assertNotNull("Expected login from " + screen.getSimpleName(), destination);
                main(() -> destination.finish());
            } finally { instrumentation.removeMonitor(monitor); }
        }
    }
    @Test public void verificationLayoutFitsNarrowScreenAtLargeFont() {
        main(() -> {
            android.content.res.Configuration config = new android.content.res.Configuration(app.getResources().getConfiguration()); config.fontScale = 1.5f;
            android.content.Context context = new android.view.ContextThemeWrapper(app.createConfigurationContext(config), R.style.Theme_ArBibleApp);
            android.view.View view = android.view.LayoutInflater.from(context).inflate(R.layout.activity_email_verification, null);
            ((android.widget.TextView)view.findViewById(R.id.tvVerificationEmail)).setText("long.account.name@gmail.com");
            ((android.widget.TextView)view.findViewById(R.id.tvVerificationStatus)).setText("Open the email link and return here to continue.");
            int width = Math.round(320 * context.getResources().getDisplayMetrics().density);
            view.measure(android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(640, android.view.View.MeasureSpec.EXACTLY));
            view.layout(0,0,width,640);
            assertTrue(view.findViewById(R.id.btnVerificationCheck).getMeasuredHeight() > 0);
            assertTrue(view.findViewById(R.id.btnVerificationLogin).getMeasuredHeight() > 0);
        });
    }
}
