package com.example.qrcode

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkCamera(this,this)
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE,Barcode.FORMAT_AZTEC).build()

    }
}

fun checkCamera(context: Context,activity: MainActivity){
    if(ActivityCompat.checkSelfPermission(context,android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
        ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.CAMERA),1)
    }
}