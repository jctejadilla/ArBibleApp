package com.example.arbibleapp;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;

public class LoginModel extends AndroidViewModel {
    final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    boolean busy;
    String error;
    Class<?> destination;
    interface Backend { com.google.android.gms.tasks.Task<Class<?>> signIn(String email, String password); }
    private final Backend backend;
    public LoginModel(Application app) {
        this(app, (email, password) -> FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).continueWithTask(auth -> {
            if (!auth.isSuccessful()) throw auth.getException();
            return VerifiedAccountFlow.destination(app, auth.getResult().getUser());
        }));
    }
    LoginModel(Application app, Backend backend) { super(app); this.backend = backend; }
    void signIn(String email, String password) {
        if (busy || destination != null) return;
        busy = true; error = null; changed();
        backend.signIn(email, password).addOnCompleteListener(task -> {
            busy = false;
            if (task.isSuccessful()) destination = task.getResult();
            else error = task.getException() instanceof IllegalStateException ? task.getException().getMessage()
                    : "Could not sign in. Check your credentials and connection, then try again.";
            changed();
        });
    }
    private void changed() { changes.setValue(changes.getValue() + 1); }
}
