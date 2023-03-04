package com.example.artbookkotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.artbookkotlin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var artList: ArrayList<Art>
    private lateinit var artAdapter: ArtAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        val view=binding.root
        setContentView(view)

        artList=ArrayList<Art>() //data List

        //adapter
        artAdapter=ArtAdapter(artList)
        binding.recyclerView.layoutManager=LinearLayoutManager(this)
        binding.recyclerView.adapter=artAdapter

        dbFun() //database
    }

    //database
    private fun dbFun(){

        try{

            val database=this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)

            val cursor=database.rawQuery("SELECT * FROM arts",null)
            val artTitleIx=cursor.getColumnIndex("artTitle")
            val idIx=cursor.getColumnIndex("id")

            while (cursor.moveToNext()){
                val title=cursor.getString(artTitleIx)
                val id=cursor.getInt(idIx)
                val art=Art(title,id)
                artList.add(art)
            }

            artAdapter.notifyDataSetChanged()

        } catch (e: Exception){
            e.printStackTrace()
        }

    }

    //menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //inflater
        val menuInflater=menuInflater
        menuInflater.inflate(R.menu.main_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId==R.id.add_Art){
            val intent= Intent(this@MainActivity,DetailsActivity::class.java)
            intent.putExtra("info","new")
            startActivity(intent)
        }

        return super.onOptionsItemSelected(item)
    }

}