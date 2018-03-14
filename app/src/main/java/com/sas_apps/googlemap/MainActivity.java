package com.sas_apps.googlemap;

import android.app.Dialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    Button buttonOpenMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        buttonOpenMap=findViewById(R.id.button_openMap);
        buttonOpenMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGoogleMapServiceAvailable()){
                    Intent intent=new Intent(MainActivity.this,MapActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    public boolean isGoogleMapServiceAvailable(){
        int googleMapAvailable=GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        Log.d(TAG, "isGoogleMapServiceAvailable: Checking google map service...");
        if (googleMapAvailable== ConnectionResult.SUCCESS){
            Log.d(TAG, "isGoogleMapServiceAvailable: Google maps service is available");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(googleMapAvailable)){
            Log.d(TAG, "isGoogleMapServiceAvailable: Google maps service is unavailable but error can be fixed");
            int errorDialogRequest = 44;
            Dialog dialog=GoogleApiAvailability.getInstance().getErrorDialog(this,googleMapAvailable, errorDialogRequest);
            dialog.show();
        }
        else {
            Toast.makeText(this, "Google maps is not available", Toast.LENGTH_SHORT).show();
        }


     return false;
    }
}
