package com.example.arbibleapp;

import android.app.Application;
import android.content.Intent;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AccountRecoveryTest {
    private final Application app = ApplicationProvider.getApplicationContext();
    void main(Runnable r) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(r);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    @Test public void existingAccountDoesNotSaveReplacementProfile() {
        android.content.SharedPreferences prefs = app.getSharedPreferences("collision-test-only", 0);
        prefs.edit().clear().commit();
        int[] calls = {0,0}; TaskCompletionSource<String> task = new TaskCompletionSource<>();
        RegistrationModel.Backend backend = new RegistrationModel.Backend() {
            public String uid() { return null; }
            public Task<String> create(String email, String password) { calls[0]++; return task.getTask(); }
            public Task<Void> save(String uid, Map<String,Object> data) { calls[1]++; throw new AssertionError("Existing profile must not be changed"); }
        };
        RegistrationModel[] model = new RegistrationModel[1];
        main(() -> {
            model[0] = new RegistrationModel(app, backend, prefs);
            model[0].register("Different name", "existing@gmail.com", "different-password", "Male", "Teacher");
            model[0].register("Different name", "existing@gmail.com", "different-password", "Male", "Teacher");
        });
        main(() -> task.setException(new FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "exists")));
        assertEquals(1, calls[0]); assertEquals(0, calls[1]); assertEquals("existing@gmail.com", model[0].existingEmail);
        assertFalse(model[0].complete); assertFalse(model[0].busy); assertNull(model[0].error); assertTrue(prefs.getAll().isEmpty());
    }
    @Test public void registrationRejectsAdminRoleBeforeCreatingAccount() {
        android.content.SharedPreferences prefs = app.getSharedPreferences("admin-registration-test-only", 0);
        prefs.edit().clear().commit();
        int[] creates = {0};
        RegistrationModel.Backend backend = new RegistrationModel.Backend() {
            public String uid() { return null; }
            public Task<String> create(String email, String password) { creates[0]++; return new TaskCompletionSource<String>().getTask(); }
            public Task<Void> save(String uid, Map<String,Object> data) { fail("Profile must not be saved"); return null; }
        };
        RegistrationModel[] model = new RegistrationModel[1];
        main(() -> {
            model[0] = new RegistrationModel(app, backend, prefs);
            model[0].register("Admin", "admin@gmail.com", "password123", "Male", "Admin");
        });
        assertEquals(0, creates[0]);
        assertFalse(model[0].complete);
        assertFalse(model[0].busy);
        assertTrue(model[0].error.contains("Student or Teacher"));
        assertTrue(prefs.getAll().isEmpty());
    }
    @Test public void registrationSelectorOnlyOffersStudentAndTeacher() {
        try (ActivityScenario<Register> scenario = ActivityScenario.launch(Register.class)) {
            scenario.onActivity(activity -> {
                com.google.android.material.button.MaterialButtonToggleGroup group = activity.findViewById(R.id.toggleUserType);
                assertEquals(2, group.getChildCount());
                assertNotNull(group.findViewById(R.id.btnStudentRegister));
                assertNotNull(group.findViewById(R.id.btnTeacherRegister));
            });
        }
    }
    @Test public void wrongPasswordStaysOnLoginAndSuccessfulRetryResumesVerification() {
        TaskCompletionSource<Class<?>> first = new TaskCompletionSource<>(), second = new TaskCompletionSource<>();
        int[] calls = {0}; LoginModel[] model = new LoginModel[1];
        main(() -> {
            model[0] = new LoginModel(app, (email, password) -> ++calls[0] == 1 ? first.getTask() : second.getTask());
            model[0].signIn("existing@gmail.com", "wrong"); model[0].signIn("existing@gmail.com", "wrong");
        });
        main(() -> first.setException(new Exception("wrong credentials")));
        assertNull(model[0].destination); assertFalse(model[0].busy); assertNotNull(model[0].error); assertEquals(1,calls[0]);
        main(() -> model[0].signIn("existing@gmail.com", "original"));
        main(() -> second.setResult(EmailVerificationActivity.class)); assertEquals(EmailVerificationActivity.class, model[0].destination);
    }
    @Test public void profileRoutingPreservesVerifiedRolesAndRequiresSavedDetailsForRepair() {
        for (String role : new String[]{"Student","Teacher","Admin"}) {
            assertEquals(EmailVerificationActivity.class, VerifiedAccountFlow.profileDestination(true, false, false, role));
            assertEquals(VerifiedAccountFlow.dashboard(role), VerifiedAccountFlow.profileDestination(true, true, true, role));
        }
        assertEquals(Register.class, VerifiedAccountFlow.profileDestination(false, false, true, null));
        assertEquals(Register.class, VerifiedAccountFlow.profileDestination(false, true, true, null));
        try { VerifiedAccountFlow.profileDestination(false, false, false, null); fail(); }
        catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("profile is missing")); }
    }
    private AlertDialog dialog(Register activity) {
        try { java.lang.reflect.Field f=Register.class.getDeclaredField("recoveryDialog"); f.setAccessible(true); return (AlertDialog)f.get(activity); }
        catch(Exception e) { throw new AssertionError(e); }
    }
    private void recoveryAction(int button, Class<?> target) {
        org.junit.Assume.assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        java.util.concurrent.atomic.AtomicReference<Intent> launched=new java.util.concurrent.atomic.AtomicReference<>();
        android.app.Instrumentation.ActivityMonitor monitor=new android.app.Instrumentation.ActivityMonitor() {
            @Override public android.app.Instrumentation.ActivityResult onStartActivity(Intent intent) {
                if (intent.getComponent()!=null && target.getName().equals(intent.getComponent().getClassName())) {
                    launched.set(intent); return new android.app.Instrumentation.ActivityResult(0,null);
                }
                return null;
            }
        };
        instrumentation.addMonitor(monitor);
        try (ActivityScenario<Register> scenario=ActivityScenario.launch(Register.class)) {
            scenario.onActivity(a -> {
                RegistrationModel model=new ViewModelProvider(a).get(RegistrationModel.class);
                model.existingEmail="existing@gmail.com"; model.changes.setValue(model.changes.getValue()+1);
                assertTrue(dialog(a).isShowing());
            });
            scenario.recreate();
            scenario.onActivity(a -> { assertTrue(dialog(a).isShowing()); dialog(a).getButton(button).performClick(); });
            assertNotNull(launched.get()); assertEquals("existing@gmail.com",launched.get().getStringExtra(VerifiedAccountFlow.EXTRA_EMAIL));
            assertEquals(1,launched.get().getExtras().size());
        } finally { instrumentation.removeMonitor(monitor); }
    }
    @Test public void collisionDialogSurvivesRotationAndOpensLoginWithEmailOnly() { recoveryAction(AlertDialog.BUTTON_POSITIVE,login.class); }
    @Test public void collisionDialogOpensPasswordRecoveryWithEmailOnly() { recoveryAction(AlertDialog.BUTTON_NEUTRAL,ForgotPassword.class); }
    @Test public void loginAndResetPrefillEmailWithoutPasswordAndPreserveEditsOnRotation() {
        org.junit.Assume.assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        try (ActivityScenario<login> scenario=ActivityScenario.launch(new Intent(app,login.class).putExtra(VerifiedAccountFlow.EXTRA_EMAIL,"existing@gmail.com"))) {
            scenario.onActivity(a -> {
                assertEquals("existing@gmail.com",((EditText)a.findViewById(R.id.etEmail)).getText().toString());
                assertEquals("",((EditText)a.findViewById(R.id.etPassword)).getText().toString());
                ((EditText)a.findViewById(R.id.etEmail)).setText("edited@gmail.com");
            });
            scenario.recreate(); scenario.onActivity(a -> assertEquals("edited@gmail.com",((EditText)a.findViewById(R.id.etEmail)).getText().toString()));
        }
        try (ActivityScenario<ForgotPassword> scenario=ActivityScenario.launch(new Intent(app,ForgotPassword.class).putExtra(VerifiedAccountFlow.EXTRA_EMAIL,"existing@gmail.com"))) {
            scenario.onActivity(a -> assertEquals("existing@gmail.com",((EditText)a.findViewById(R.id.etEmail)).getText().toString()));
        }
    }
}
