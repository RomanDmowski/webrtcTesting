package com.roman;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

        loginUser = findViewById(R.id.login_user);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        textViewLinkSignUp = findViewById(R.id.textViewLinkSignUp);

        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveLogPass();
                finish();
            }
        });

        textViewLinkSignUp.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(LoginActivity.this, NewUserActivity.class);
                startActivity(intent);
            }
        });



        loginUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                enableLoginButton();
            }
        });

        loginPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                enableLoginButton();
            }
        });

        setLoginPasswordValues();


    }


    void enableLoginButton()
    {
        if ((loginUser.length()>3) && (loginPassword.length()>3))
        {
            loginButton.setEnabled(true);
        }
        else
        {
            loginButton.setEnabled(false);
        }
    }

    private void saveLogPass()
    {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = SP.edit();
        editor.putString(getString(R.string.preferences_user_key),loginUser.getText().toString());
        editor.putString(getString(R.string.preferences_pass_key),loginPassword.getText().toString());
        editor.apply();
    }

    private void setLoginPasswordValues()
    {
        String log;
        String pass;
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        log= SP.getString(getString(R.string.preferences_user_key),"");
        pass = SP.getString(getString(R.string.preferences_pass_key),"");

        loginUser.setText(log);
        loginPassword.setText(pass);


    }


}
