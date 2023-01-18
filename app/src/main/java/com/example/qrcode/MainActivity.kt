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
//定義ViewBinding
var binding:ActivityMainBinding?=null
//定義CameraXSource
private lateinit var cameraXSource: CameraXSource
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //設定ViewBinding
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        //定義BarCode Scanner
        val barcodeScannerOptions =
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        val barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions)
        //設定CameraXSource
        val cameraSourceConfig = CameraSourceConfig.Builder(
            this, barcodeScanner
        ) { task ->
            //定義掃描的Task
            var task: Task<List<Barcode?>?> = task
            //解析掃描到的BarCode
            task = barcodeScanner.process(InputImage.fromBitmap(binding!!.preView.getBitmap()!!, 0))
            //若掃描成功，執行OnSuccessListener內的程式
            task.addOnSuccessListener(object : OnSuccessListener<List<Barcode?>?> {
                override fun onSuccess(p0: List<Barcode?>?) {
                    if (p0!!.isNotEmpty()) {
                        //取得結果
                        val intent = Intent()
                        intent.putExtra("QR", p0[0]?.displayValue)
                        this@MainActivity.setResult(RESULT_OK, intent)
                        //將結果顯示在畫面上
                        binding!!.txtResult.text=p0[0]?.displayValue
                    }
                }
            })
        }.setFacing(CameraSourceConfig.CAMERA_FACING_BACK).build()
        cameraXSource = CameraXSource(cameraSourceConfig, binding!!.preView)
        //設定相機權限
        if(ActivityCompat.checkSelfPermission(this@MainActivity,android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.CAMERA),1)
        }
        //開啟相機
        cameraXSource.start()
        //設定前往的監聽式
        binding!!.btnGo.setOnClickListener {
            try{
                //開啟瀏覽器
                var intent=Intent(Intent.ACTION_VIEW, Uri.parse(binding!!.txtResult.text.toString()))
                startActivity(intent)
            }catch (ex:java.lang.Exception){
                //處理錯誤(例如開啟的不是網址)
                Toast.makeText(this,"發生錯誤，是不是網址有錯？",Toast.LENGTH_SHORT).show()
            }

        }
        //設定複製的監聽式
        binding!!.btnCopy.setOnClickListener {
            //設定剪貼簿
            val clipboardManager=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            //設定複製的來源
            var clipData=ClipData.newPlainText("url", binding!!.txtResult.text)
            //複製到剪貼簿
            clipboardManager.setPrimaryClip(clipData)
        }
        //開啟或新建SQL
        var db=openOrCreateDatabase("db.db", MODE_PRIVATE,null)
        try{
            //若還未建立過表單，則建立
            db.execSQL("CREATE TABLE dbTable (id INTEGER PRIMARY KEY,url TEXT)")
        }catch (ex:Exception){}
        //查詢SQL內的所有值
        var cur=db.rawQuery("SELECT * FROM dbTable",null)
        //將cursor移到第一個（原為-１）
        cur.moveToFirst()
        //定義Vector
        var id=Vector<Int>()
        var name=Vector<String>()
        //將SQL內每一行的值分別加入id及name
        for(i in 0 until cur.count){
            id.add(cur.getInt(0))
            name.add(cur.getString(1))
            cur.moveToNext()
        }
        //設定剪貼簿
        val clipboardManager=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        //設定ListView的Adapter (Custom Adapter)
        listView.adapter=listViewAdpater(this,id,name,db,listView,clipboardManager)
        //設定加入SQL的監聽式
        btn_sql.setOnClickListener {
            //把結果加入SQL
            db.execSQL("INSERT INTO dbTable(url) VALUES('${txt_result.text}')")
            //逐一查詢SQL並加入ListView(與上面的程式碼相同)
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

//設定Custom Adapter
class listViewAdpater(var context: Context,var id:Vector<Int>,var name:Vector<String>,var db:SQLiteDatabase,var listView: ListView,var clipboardManager: ClipboardManager):BaseAdapter(){
    //得到Adapter內Item的個數
    override fun getCount(): Int {
        return id.size
    }
    //得到Adapter內的Item
    override fun getItem(p0: Int): Any {
        return name[p0]
    }
    //得到Item的ID
    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        //定義一個View,View的來源來自R.layout.listview
        var v=LayoutInflater.from(context).inflate(R.layout.listview,null)
        //設定view上面的字
        v.txtId.text=id[p0].toString()
        v.txtName.text=name[p0]
        //設定View上面刪除按鈕的監聽式
        v.btnDel.setOnClickListener {
            //刪除SQL內的資料
            db.execSQL("DELETE FROM dbTable WHERE id=${v.txtId.text}")
            //逐一查詢SQL並加入ListView(與上面的程式碼相同)
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
        //設定View上面複製按鈕的監聽式
        v.btnCopy.setOnClickListener {
            //設定複製的來源
            var clipData=ClipData.newPlainText("url", v.txtName.text)
            //複製到剪貼簿
            clipboardManager.setPrimaryClip(clipData)
        }
        //設定前往的監聽式
        v.btnGo.setOnClickListener {
            try{
                //開啟瀏覽器
                var intent=Intent(Intent.ACTION_VIEW, Uri.parse(v.txtName.text.toString()))
                startActivity(context,intent,null)
            }catch (ex:java.lang.Exception){
                //處理錯誤(例如開啟的不是網址)
                Toast.makeText(context,"發生錯誤，是不是網址有錯？",Toast.LENGTH_SHORT).show()
            }
        }
        //傳回view
        return v
    }

}