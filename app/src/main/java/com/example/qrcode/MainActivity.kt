package com.example.qrcode

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.qrcode.databinding.ActivityMainBinding
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.common.collect.Iterables.size
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.camera.CameraSourceConfig
import com.google.mlkit.vision.camera.CameraXSource
import com.google.mlkit.vision.common.InputImage

var binding:ActivityMainBinding?=null
private lateinit var cameraXSource: CameraXSource
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        val barcodeScannerOptions =
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        val barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions)
        val cameraSourceConfig = CameraSourceConfig.Builder(
            this, barcodeScanner
        ) { task ->
            var task: Task<List<Barcode?>?> = task
            task = barcodeScanner.process(InputImage.fromBitmap(binding!!.preView.getBitmap()!!, 0))
            task.addOnSuccessListener(object : OnSuccessListener<List<Barcode?>?> {
                override fun onSuccess(p0: List<Barcode?>?) {
                    if (p0!!.isNotEmpty()) {
                        val intent = Intent()
                        intent.putExtra("QR", p0[0]?.displayValue)
                        this@MainActivity.setResult(RESULT_OK, intent)

                        binding!!.txtResult.text=p0[0]?.displayValue;
                    }
                }
            })
        }.setFacing(CameraSourceConfig.CAMERA_FACING_BACK).build()
        cameraXSource = CameraXSource(cameraSourceConfig, binding!!.preView)
        if(ActivityCompat.checkSelfPermission(this@MainActivity,android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.CAMERA),1)
        }
        cameraXSource.start()

        binding!!.btnGo.setOnClickListener {
            try{
                var intent=Intent(Intent.ACTION_VIEW, Uri.parse(binding!!.txtResult.text.toString()))
                startActivity(intent)
            }catch (ex:java.lang.Exception){
                Toast.makeText(this,"發生錯誤，是不是網址有錯？",Toast.LENGTH_SHORT).show()
            }

        }
        binding!!.btnCopy.setOnClickListener {
            val clipboardManager=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            var clipData=ClipData.newPlainText("url", binding!!.txtResult.text)
            clipboardManager.setPrimaryClip(clipData)
        }
        var db=openOrCreateDatabase("db.db", MODE_PRIVATE,null)
        try{
            db.execSQL("CREATE TABLE dbTable (id INTEGER PRIMARY KEY,url TEXT)")
        }catch (ex:Exception){}
    }


}



fun checkCamera(context: Context,activity: MainActivity){
    if(ActivityCompat.checkSelfPermission(context,android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
        ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.CAMERA),1)
    }
}