package com.example.artbookkotlin

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.artbookkotlin.databinding.ActivityDetailsBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream

class DetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailsBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var selectedBitmap: Bitmap
    private lateinit var database: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityDetailsBinding.inflate(layoutInflater)
        val view=binding.root
        setContentView(view)

        registerLauncher()
        database=this.openOrCreateDatabase("Arts", Context.MODE_PRIVATE,null)

        //intent
        val intent=intent
        val info=intent.getStringExtra("info")
        if(info.equals("new")){
            binding.artTitleText.setText("")
            binding.explanationText.setText("")
            binding.dateText.setText("")
            binding.saveButon.visibility=View.VISIBLE
            binding.imageView.setImageResource(R.drawable.ic_launcher_background)
        } else {
            binding.saveButon.visibility=View.INVISIBLE
            val selectedId=intent.getIntExtra("id",1)

            val cursor=database.rawQuery("SELECT * FROM arts WHERE id=?", arrayOf(selectedId.toString()))
            val artTitleIx=cursor.getColumnIndex("artTitle")
            val explanationIx=cursor.getColumnIndex("explanation")
            val dateIx=cursor.getColumnIndex("date")
            val imageIx=cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.artTitleText.setText(cursor.getString(artTitleIx))
                binding.explanationText.setText(cursor.getString(explanationIx))
                binding.dateText.setText(cursor.getString(dateIx))

                val byteArray=cursor.getBlob(imageIx)
                val bitmap=BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }
            cursor.close()

        }

    }

    fun save(view: View){

        val artTitle=binding.artTitleText.text.toString()
        val explanation=binding.explanationText.text.toString()
        val date=binding.dateText.text.toString()

        if(selectedBitmap!=null){
            val smallBitmap=makeSmallerBitmap(selectedBitmap,300)

            val outputStream=ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray=outputStream.toByteArray()

            try {

                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artTitle VARCHAR,explanation VARCHAR,date VARCHAR,image BLOB)")
                val sqlString="INSERT INTO arts (artTitle,explanation,date,image) VALUES (?,?,?,?)"
                val statement=database.compileStatement(sqlString)
                statement.bindString(1,artTitle)
                statement.bindString(2,explanation)
                statement.bindString(3,date)
                statement.bindBlob(4,byteArray)
                statement.execute()

            } catch (e: Exception){
                e.printStackTrace()
            }

            //intent to main
            val intent=Intent(this@DetailsActivity,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

    }

    fun selectImage(view: View){

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
            //Android 33+ --> READ_MEDIA_IMAGES

            if(ContextCompat.checkSelfPermission(this@DetailsActivity,android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this@DetailsActivity,android.Manifest.permission.READ_MEDIA_IMAGES)){
                    //rationale
                    Snackbar.make(view,"Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permisson", View.OnClickListener {
                        //request permission
                        permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                    }).show()

                } else {
                    //request permission
                    permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                }

            } else {
                val intentToGallery= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }

        } else {

            //Android 32- --> READ_EXTERNAL_STORAGE

            if(ContextCompat.checkSelfPermission(this@DetailsActivity,android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this@DetailsActivity,android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                    //rationale
                    Snackbar.make(view,"Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permisson", View.OnClickListener {
                        //request permission
                        permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    }).show()

                } else {
                    //request permission
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }


            } else {
                val intentToGallery= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }

        }

    }

    private fun registerLauncher(){

        activityResultLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if(result.resultCode== RESULT_OK){

                val intentFromResult=result.data
                if(intentFromResult != null){

                    val imageData=intentFromResult.data
                    if(imageData != null){

                        try {

                            if (Build.VERSION.SDK_INT>=28){
                                val source= ImageDecoder.createSource(this@DetailsActivity.contentResolver,imageData)
                                selectedBitmap= ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            } else {
                                selectedBitmap=MediaStore.Images.Media.getBitmap(contentResolver,imageData)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }

                        } catch (e: Exception){
                            e.printStackTrace()
                        }

                    }

                }

            }

        }

        permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                //permission granted
                val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            } else{
                //permission denied
                Toast.makeText(this@DetailsActivity,"Permission Needed!",Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun makeSmallerBitmap(image: Bitmap,maxSize: Int) : Bitmap{

        var width=image.width
        var height=image.height
        var bitmapRatio: Double= width.toDouble()/height.toDouble()

        if(bitmapRatio>1){
            //landscape
            width=maxSize
            val scaledHeight=width/bitmapRatio
            height=scaledHeight.toInt()

        } else {
            //portrait
            height=maxSize
            val scaledWidth=height*bitmapRatio
            width=scaledWidth.toInt()

        }

        return Bitmap.createScaledBitmap(image,width,height,true)
    }

}