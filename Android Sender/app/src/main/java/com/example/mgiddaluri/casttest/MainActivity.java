package com.example.mgiddaluri.casttest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;


public class MainActivity extends ActionBarActivity {
    SharedPreferences sharedPreferences;
    Boolean sharedPreferencesResult;
    EditText UserName;
    EditText Password;
    ImageView imageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences=getSharedPreferences("prefernces", 0);
        sharedPreferencesResult= sharedPreferences.getBoolean("complete", false);
        if(sharedPreferencesResult==true) {
            String Username = sharedPreferences.getString("FixedUserName", "Uname");
            String Paswrd=sharedPreferences.getString("FixedPassword","Pas");
            Intent intent1=new Intent(MainActivity.this,MeetingInfo.class);
            intent1.putExtra("UserName",Username);
            intent1.putExtra("Password",Paswrd);
            finish();
            startActivity(intent1);
        }
        UserName=(EditText)findViewById(R.id.uname);
        Password=(EditText)findViewById(R.id.pswd);
        imageButton=(ImageView)findViewById(R.id.login);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editors=sharedPreferences.edit();
                String Uname=UserName.getText().toString().trim();
                String Pas=Password.getText().toString().trim();
                editors.putString("FixedUserName",Uname);
                editors.putString("FixedPassword",Pas);
                editors.putBoolean("complete",true);
                editors.commit();
                Intent intent = new Intent(MainActivity.this,MeetingInfo.class);
                intent.putExtra("UserName", Uname);
                intent.putExtra("Password", Pas);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
