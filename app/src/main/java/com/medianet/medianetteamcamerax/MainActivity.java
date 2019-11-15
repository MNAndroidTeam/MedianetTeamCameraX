package com.medianet.medianetteamcamerax;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.medianet.camerax.TakePicture;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent=new Intent(this, TakePicture.class);
       // intent.putExtra("absolutePath", getExternalMediaDirs()[0]+"/mbh.jpg");
        startActivityForResult(intent,4413);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == 4413) {


        }
    }//onA
}
