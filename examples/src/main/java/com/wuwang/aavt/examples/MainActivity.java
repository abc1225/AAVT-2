package com.wuwang.aavt.examples;

import android.Manifest;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private PermissionAsker mAsker;
    private final int REQUEST_CODE_VIDEO_PLAY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAsker=new PermissionAsker(10,new Runnable() {
            @Override
            public void run() {
                setContentView(R.layout.activity_main);
            }
        }, new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "必要权限被拒绝，应用退出",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }).askPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mAsker.onRequestPermissionsResult(grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null && data.getData() != null){
            if(REQUEST_CODE_VIDEO_PLAY == requestCode){
                String path = GetPathFromUri4kitkat.getPath(this,data.getData());
                Intent intent = new Intent(this,VideoPlayerActivity.class);
                intent.putExtra("videoPath",path);
                startActivity(intent);
            }
        }
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.mMp4Process:
                startActivity(new Intent(this,ExampleMp4ProcessActivity.class));
                break;
            case R.id.mCameraRecord:
                startActivity(new Intent(this,CameraRecorderActivity.class));
                break;
            case R.id.mYuvExport:
                startActivity(new Intent(this,YuvExportActivity.class));
                break;
            case R.id.mPlayVideo:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                startActivityForResult(intent,REQUEST_CODE_VIDEO_PLAY);
                break;
            default:break;
        }
    }
}
