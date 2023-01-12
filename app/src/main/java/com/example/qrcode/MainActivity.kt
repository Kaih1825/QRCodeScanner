package com.example.qrcode

import android.Manifest
import android.app.ActionBar.LayoutParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
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
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.listview.view.*
import java.util.Date.from
import java.util.Vector

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
        var cur=db.rawQuery("SELECT * FROM dbTable",null)
        cur.moveToFirst()
        var id=Vector<Int>()
        var name=Vector<String>()
        for(i in 0 until cur.count){
            id.add(cur.getInt(0))
            name.add(cur.getString(1))
            cur.moveToNext()
        }
        val clipboardManager=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        listView.adapter=listViewAdpater(this,id,name,db,listView,clipboardManager)
        btn_sql.setOnClickListener {
            db.execSQL("INSERT INTO dbTable(url) VALUES('${txt_result.text}')")
            var cur=db.rawQuery("SELECT * FROM dbTable",null)
            cur.moveToFirst()
            var id=Vector<Int>()
            var name=Vector<String>()
            for(i in 0 until cur.count){
                id.add(cur.getInt(0))
                name.add(cur.getString(1))
                cur.moveToNext()
            }
            val clipboardManager=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            listView.adapter=listViewAdpater(this,id,name,db,listView,clipboardManager)
        }
    }
}



fun checkCamera(context: Context,activity: MainActivity){
    if(ActivityCompat.checkSelfPermission(context,android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
        ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.CAMERA),1)
    }
}

class listViewAdpater(var context: Context,var id:Vector<Int>,var name:Vector<String>,var db:SQLiteDatabase,var listView: ListView,var clipboardManager: ClipboardManager):BaseAdapter(){
    override fun getCount(): Int {
        return id.size
    }

    override fun getItem(p0: Int): Any {
        return name[p0]
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        var v=LayoutInflater.from(context).inflate(R.layout.listview,null)
        v.txtId.text=id[p0].toString()
        v.txtName.text=name[p0]

        v.btnDel.setOnClickListener {
            db.execSQL("DELETE FROM dbTable WHERE id=${v.txtId.text}")
            var cur=db.rawQuery("SELECT * FROM dbTable",null)
            cur.moveToFirst()
            var id=Vector<Int>()
            var name=Vector<String>()
            for(i in 0 until cur.count){
                id.add(cur.getInt(0))
                name.add(cur.getString(1))
                cur.moveToNext()
            }
            listView.adapter=listViewAdpater(context,id,name,db,listView,clipboardManager)
        }

        v.btnCopy.setOnClickListener {
            var clipData=ClipData.newPlainText("url", v.txtName.text)
            clipboardManager.setPrimaryClip(clipData)
        }
        v.btnGo.setOnClickListener {
            try{
                var intent=Intent(Intent.ACTION_VIEW, Uri.parse(v.txtName.text.toString()))
                startActivity(context,intent,null)
            }catch (ex:java.lang.Exception){
                Toast.makeText(context,"發生錯誤，是不是網址有錯？",Toast.LENGTH_SHORT).show()
            }
        }
        return v
    }

}