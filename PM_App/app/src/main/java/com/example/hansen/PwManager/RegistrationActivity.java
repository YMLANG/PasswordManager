package com.example.hansen.PwManager;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class RegistrationActivity extends AppCompatActivity {

    private EditText email;
    private EditText password;
    private EditText confirmPassword;
    private Button signUp;
    private FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        setupUIviews();

        firebaseAuth = FirebaseAuth.getInstance();


        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkNotEmpty()){

                    String regEmail = email.getText().toString().trim();
                    String regPassword = password.getText().toString().trim();

                    firebaseAuth.createUserWithEmailAndPassword(regEmail, regPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                           if(task.isSuccessful()){  // if authentication is successful, go to second activity for now
                               Toast.makeText(RegistrationActivity.this,"Success", Toast.LENGTH_SHORT).show();
                               startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                           } else {
                               FirebaseAuthException e = (FirebaseAuthException )task.getException();
                               Toast.makeText(RegistrationActivity.this,"Registration Failed" + e.getMessage(), Toast.LENGTH_SHORT).show();

                           }
                        }
                    });
                }
            }
        });

    }

    private void setupUIviews(){
        email = (EditText)findViewById(R.id.etRegEmail);
        password = (EditText)findViewById(R.id.etRegPassword);
        confirmPassword = (EditText)findViewById(R.id.etRegCPassword);
        signUp = (Button)findViewById(R.id.btnSignUp);
    }

    private Boolean checkNotEmpty(){
        Boolean result = false;
        String pw = password.getText().toString();
        String cpw = confirmPassword.getText().toString();
        String em = email.getText().toString();

        // check if all the fields is filled, otherwise shows an error
        if(pw.isEmpty() || em.isEmpty() || cpw.isEmpty()){
            Toast.makeText(this, "Please enter all the required fields", Toast.LENGTH_SHORT).show();
        } else if (!pw.equals(cpw)) {
            Toast.makeText(this, "Passwords are not the same", Toast.LENGTH_SHORT).show();
        } else {
            result = true;
        }
        return result;
    }



}
