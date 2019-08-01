package com.example.hansen.PwManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private EditText Username;
    private EditText Password;
    //private TextView Info;
    private Button Login;
    private int counter = 5;
    private TextView SignUp;
    private FirebaseAuth mAuth;
    private ProgressDialog loginDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // assign variables with the layout created
        Username = (EditText)findViewById(R.id.etName);
        Password = (EditText)findViewById(R.id.etPassword);
        Login = (Button)findViewById(R.id.btnLogin);
        //Info =(TextView)findViewById(R.id.tvInfo);
        SignUp =(TextView)findViewById(R.id.tvSignUp);
        loginDialog = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        //FirebaseUser currentUser = mAuth.getCurrentUser();
        //if(currentUser != null) startActivity(new Intent(MainActivity.this, SecondActivity.class));

        // log in
        Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = Username.getText().toString();
                String password = Password.getText().toString();
                if (username.isEmpty() && password.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a username and password", Toast.LENGTH_SHORT).show();
                }
                else if (password.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a password", Toast.LENGTH_SHORT).show();
                }
                else if (username.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a username", Toast.LENGTH_SHORT).show();

                }
                else {
                    validate(username, password);
                }
            }
        });

        // go to registration page
        SignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RegistrationActivity.class));

            }
        });

    }

    // validate login details
    private void validate(String UserName, String UserPassword){
        loginDialog.setMessage("Logging in");
        loginDialog.show();


        mAuth.signInWithEmailAndPassword(UserName, UserPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    loginDialog.dismiss();

                    startActivity(new Intent(MainActivity.this, ViewerActivity.class));

                } else {
                    loginDialog.dismiss();
                    Toast.makeText(MainActivity.this, "E-mail/Password is incorrect",Toast.LENGTH_SHORT).show();
                }
            }
        });


    }



}
