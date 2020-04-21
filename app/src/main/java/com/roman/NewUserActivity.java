package com.roman;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.roman.pawelm.R;

public class NewUserActivity extends AppCompatActivity {

    Button registerButton;
    EditText newLogin;
    EditText newPassword;
    EditText newPasswordConfirmation;
    TextView textViewLinkSignIn;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new_user);


        newLogin = findViewById(R.id.new_user);
        newPassword = findViewById(R.id.new_password);
        newPasswordConfirmation = findViewById(R.id.new_password_confirmation);
        registerButton=findViewById(R.id.register_button);
        textViewLinkSignIn=findViewById(R.id.textViewDontHaveAccount);

        registerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                //createNewLogin("login647","passw345");

                Intent intent = new Intent(NewUserActivity.this, MainActivity.class);
                intent.putExtra("_newLogin", newLogin.getText().toString());
                intent.putExtra("_newPass", newPassword.getText().toString());
                startActivity(intent);



            }
        });

        textViewLinkSignIn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(NewUserActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });



        newLogin.addTextChangedListener(new TextWatcher() {
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
                enableRegisterButton();
            }
        });

        newPassword.addTextChangedListener(new TextWatcher() {
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
                enableRegisterButton();
            }
        });


        newPasswordConfirmation.addTextChangedListener(new TextWatcher() {
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
                enableRegisterButton();
            }
        });





    }



//    private void createNewLogin(String _newUser, String _newPass)
//    {
//
//       //MainActivity.createNewUser(_newUser,_newPass);
//
//
////        try {
////            JSONObject json = new JSONObject();
////
////            json.put("type", "newuser");
////            json.put("newlogin", "test001");
////            json.put("newpass", "password02");
////
////            //wsListener.send(json.toString());
////            Logging.d(TAG, "Creating newuser: " + json.toString());
////        } catch (JSONException e) {
////            e.printStackTrace();
////        }
//
//
//

//    }


    void enableRegisterButton()
    {

        if ((newLogin.length() >3) && (newPassword.length()>3) && (newPassword.getText().toString().equals(newPasswordConfirmation.getText().toString())))
        {
            registerButton.setEnabled(true);
        }
        else
        {
            registerButton.setEnabled(false);
        }
    }
}
