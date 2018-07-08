package com.simons.owner.traffickcam2;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

public class PermissionsActivity extends AppCompatActivity {

    private boolean hasCameraPermission;
    private boolean hasLocationPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        getCameraPermission();
        getLocationPermission();

        if(hasCameraPermission && hasLocationPermission)
        {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }else
        {
            printPermissionMessage();
        }


    }

    private void printPermissionMessage()
    {
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Alert")
                .setMessage("TraffickCam requires certain permissions.  Please edit permissions in your settings.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        dialog = builder.create();
        dialog.show();
    }

    private void getLocationPermission()
    {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                1
        );

        int permissionCheck = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        );

        hasLocationPermission = (permissionCheck == PackageManager.PERMISSION_GRANTED);
    }


    private void getCameraPermission()
    {
        // request permission to use camera
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                1
        );

        // check to see whether or not camera permission has been granted
        int permissionCheck = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        );

        // if TraffickCam has permission to use the user's camera, hasCameraPermission is true
        hasCameraPermission = (permissionCheck == PackageManager.PERMISSION_GRANTED);
    }

}
