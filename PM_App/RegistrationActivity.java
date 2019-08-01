package com.example.hansen.PwManager;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegistrationActivity extends AppCompatActivity {

    private EditText userName;
    private EditText email;
    private EditText password;
    private EditText cpassword;
    private Button signUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        setupUIviews();

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkNotEmpty()){
                    Intent intent = new Intent(RegistrationActivity.this, SecondActivity.class);
                    startActivity(intent);
                }
            }
        });

    }

    private void setupUIviews(){
        userName = (EditText)findViewById(R.id.etRegName);
        email = (EditText)findViewById(R.id.etRegEmail);
        password = (EditText)findViewById(R.id.etRegPassword);
        cpassword = (EditText) findViewById(R.id.etRegCPassword);
        signUp = (Button)findViewById(R.id.btnSignUp);
    }

    private Boolean checkNotEmpty(){
        Boolean result = false;
        String name = userName.getText().toString();
        String pw = password.getText().toString();
        String cpw = cpassword.getText().toString();
        String em = email.getText().toString();

        // check if all the fields is filled, otherwise shows an error
        if(name.isEmpty() || pw.isEmpty() || em.isEmpty() || cpw.isEmpty()){
            Toast.makeText(this, "Please enter all the required fields", Toast.LENGTH_SHORT).show();
        } else if (!pw.toString().equals(cpw.toString())) {
            Toast.makeText(this, "Passwords are not the same", Toast.LENGTH_SHORT).show();
        } else {
            result = true;
        }
        return result;
    }



}
