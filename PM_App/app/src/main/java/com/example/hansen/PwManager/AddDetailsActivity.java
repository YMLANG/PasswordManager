package com.example.hansen.PwManager;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.util.ArrayUtils;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AddDetailsActivity extends AppCompatActivity {

    private EditText app;
    private EditText username;
    private EditText password;
    private EditText pwdLength;
    private Button save;
    private TextView genPass;
    private CheckBox specBox;
    private CheckBox upperBox;
    private CheckBox numBox;
    private Button genButton;
    private TextView strength;
    private LinearLayout genLayout;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private static final String key = "This is a key123";
    private static final String iv = "This is an IV123";

    private ArrayList<String> passwordChars;
    private ArrayList<String> capsChars;
    private ArrayList<String> specialChars;
    private ArrayList<String> numChars;
    private ArrayList<String> usedChars;
    private HashSet<Character> strengthChars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        app = (EditText)findViewById(R.id.appDetails);
        username = (EditText)findViewById(R.id.usernameDetails);
        password = (EditText)findViewById(R.id.passwordDetails);
        save =(Button)findViewById(R.id.saveDetails);
        genPass = (TextView)findViewById(R.id.genPassword);
        specBox = (CheckBox)findViewById(R.id.specBox);
        upperBox = (CheckBox)findViewById(R.id.upperBox);
        numBox = (CheckBox)findViewById(R.id.numBox);
        genButton = (Button)findViewById(R.id.genButton);
        genLayout = (LinearLayout) findViewById(R.id.genLayout);
        pwdLength = (EditText)findViewById(R.id.pwdLength);
        strength = (TextView)findViewById(R.id.strength);

        passwordChars = new ArrayList<String>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i",
                "j", "k", "l", "m", "o", "p", "q", "r", "s",
                "t", "u", "v", "w", "x", "y", "z"));
        capsChars = new ArrayList<String>(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I",
                "J", "K", "L", "M", "O", "P", "Q", "R", "S",
                "T", "U", "V", "W", "X", "Y", "Z"));
        specialChars = new ArrayList<String>(Arrays.asList("!", "@", "#", "$", "%", "^", "&", "*", "(",
                ")", "-", "=", "_", "+", "}", "[", "]", "<",
                ">", ",", "."));
        numChars = new ArrayList<String>(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference ref = database.getReference();

                mAuth = FirebaseAuth.getInstance();
                String user_id = mAuth.getCurrentUser().getUid();

                Map<String, Account> accounts_to_save = new HashMap<>();

                String app_name = app.getText().toString();
                String acc = username.getText().toString();
                String pass = password.getText().toString();
                if (app_name.isEmpty() || acc.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(AddDetailsActivity.this, "Please fill out all the fields", Toast.LENGTH_SHORT).show();
                }
                else {
                    String accEncrypted = acc;
                    String passEncrypted = pass;
                    String debugging = "muppet";
                    try {
                        accEncrypted = encrypt(acc);
                        passEncrypted = encrypt(pass);
                        debugging = "wewlad";
                    } catch (Exception e) {
                        e.printStackTrace();
                        debugging = "goddammit";
                    }
                    Account toAdd = new Account(accEncrypted, passEncrypted);
                    accounts_to_save.put(app_name, toAdd);

                    DatabaseReference userRef = database.getReference("users");
                    DatabaseReference accRef = userRef.child(user_id);

                    for (String key : accounts_to_save.keySet()) {
                        accRef.child(key).setValue(accounts_to_save.get(key));
                    }

                    Toast.makeText(AddDetailsActivity.this, "Account details saved successfully", Toast.LENGTH_SHORT).show();
                    //Toast.makeText(AddDetailsActivity.this,debugging, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(AddDetailsActivity.this, ViewerActivity.class));
                }
            }
        });

        genPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(genLayout.getVisibility() == View.VISIBLE) {
                    genLayout.setVisibility(View.INVISIBLE);
                }
                else {
                    genLayout.setVisibility(View.VISIBLE);
                }
        }
        });

        genButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usedChars = new ArrayList<String>();
                usedChars.addAll(passwordChars);
                if(specBox.isChecked()) {
                    usedChars.addAll(specialChars);
                }
                if(upperBox.isChecked()) {
                    usedChars.addAll(capsChars);
                }
                if(numBox.isChecked()) {
                    usedChars.addAll(numChars);
                }
                int passLength = 0;
                String input = pwdLength.getText().toString();
                if(input.isEmpty()) {
                    passLength = 16;
                }
                else {
                    passLength = Integer.parseInt(input);
                }
                password.setText(generatePassword(passLength, usedChars));
            }
        });

        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() == 0) {
                    strength.setText("None");
                    strength.setTextColor(Color.GRAY);
                }
                else {
                    checkPasswordStrength(s.toString());
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private String encrypt(String detail) throws Exception{
        Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
        while ((detail.length() % 16) != 0) {
            detail = detail + "{";
        }
        SecretKeySpec sKeySpec = new SecretKeySpec(key.getBytes(), "AES");
        c.init(Cipher.ENCRYPT_MODE, sKeySpec, new IvParameterSpec(iv.getBytes()));
        byte[] encDetail = c.doFinal(detail.getBytes());
        String encryptedDetail = Base64.encodeToString(encDetail, Base64.DEFAULT);
        return encryptedDetail;
    }

    private String generatePassword(int passwordLength, ArrayList<String> passwordChars) {
        String generatedPassword = "";
        for(int i = 0; i < passwordLength; i++) {
            generatedPassword = generatedPassword + passwordChars.get(randomNumber(0, (passwordChars.size()-1)));
        }
        return generatedPassword;
    }

    private static int randomNumber(int min, int max) {
        int x = (int)(Math.random()*((max-min)+1))+min;
        return x;
    }

    private void checkPasswordStrength (String password) {
        strengthChars = new HashSet<Character>();
        int score = 0;
        char[] stringArray = password.toCharArray();
        for (char letter : stringArray) {
            if(strengthChars.contains(letter)) {
                score++;
            }
            else {
                strengthChars.add(letter);
                score = score + 3;
            }
        }
        if (score >= 0 && score < 18) {
            strength.setText("Weak");
            strength.setTextColor(Color.RED);
        }
        else if (score >= 18 && score < 36) {
            strength.setText("Medium");
            strength.setTextColor(Color.rgb(255, 165, 0));
        }
        else if (score >= 36) {
            strength.setText("Strong");
            strength.setTextColor(Color.GREEN);
        }
    }
}