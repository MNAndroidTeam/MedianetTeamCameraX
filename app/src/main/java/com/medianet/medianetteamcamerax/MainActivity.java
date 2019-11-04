package com.medianet.medianetteamcamerax;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import com.medianet.camerax.TakePicture;

import java.io.File;

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
            String imagePath=data.getStringExtra("image_absolute_path");

            if (imagePath.length()>0){
                Toast.makeText(this, imagePath, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "image canceled", Toast.LENGTH_SHORT).show();
            }

        }
    }//onA
}
