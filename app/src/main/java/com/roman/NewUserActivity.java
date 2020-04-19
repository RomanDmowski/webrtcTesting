package com.roman;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
                createNewLogin("login647","passw345");
            }
        });

        textViewLinkSignIn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(NewUserActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

    }



    private void createNewLogin(String _newUser, String _newPass)
    {

       //MainActivity.createNewUser(_newUser,_newPass);


//        try {
//            JSONObject json = new JSONObject();
//
//            json.put("type", "newuser");
//            json.put("newlogin", "test001");
//            json.put("newpass", "password02");
//
//            //wsListener.send(json.toString());
//            Logging.d(TAG, "Creating newuser: " + json.toString());
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }




    }
}
