package com.roman;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.roman.pawelm.R;

public class LoginActivity extends AppCompatActivity {

    EditText loginUser;
    EditText loginPassword;
    Button loginButton;
    TextView textViewLinkSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginUser = findViewById(R.id.new_user);
        loginPassword = findViewById(R.id.new_password);
        loginButton=findViewById(R.id.register_button);
        textViewLinkSignUp=findViewById(R.id.textViewLinkSignUp);

//        loginButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                // Code here executes on main thread after user presses button
//                createNewLogin("login647","passw345");
//            }
//        });
//
        textViewLinkSignUp.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(LoginActivity.this, NewUserActivity.class);
                startActivity(intent);
            }
        });
    }
}
