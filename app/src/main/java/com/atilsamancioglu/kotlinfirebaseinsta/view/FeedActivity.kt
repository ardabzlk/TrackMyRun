package com.example.trackmyrun.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackmyrun.*
import com.example.trackmyrun.adapter.FeedRecyclerAdapter
import com.example.trackmyrun.databinding.ActivityFeedBinding
import com.example.trackmyrun.model.Post
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.collections.ArrayList

class FeedActivity : AppCompatActivity() {

    private lateinit var auth : FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private lateinit var binding: ActivityFeedBinding

    val postArrayList : ArrayList<Post> = ArrayList()
    var adapter : FeedRecyclerAdapter? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.options_menu,menu)

        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.add_post) {
            //Upload Activity
            val intent = Intent(applicationContext, UploadActivity::class.java)
            startActivity(intent)

        } else if (item.itemId == R.id.logout) {
            //Logout

            auth.signOut()
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()

        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        getDataFromFirestore()


        //recyclerview

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FeedRecyclerAdapter(postArrayList)
        binding.recyclerView.adapter = adapter

    }


    fun getDataFromFirestore() {

        db.collection("Posts").orderBy("date",Query.Direction.DESCENDING).addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Toast.makeText(applicationContext,exception.localizedMessage,Toast.LENGTH_LONG).show()
            } else {

               if (snapshot != null) {
                   if (!snapshot.isEmpty) {

                      postArrayList.clear()

                       val documents = snapshot.documents
                       for (document in documents) {
                           val comment = document.get("comment") as String
                           val useremail = document.get("userEmail") as String
                           val downloadUrl = document.get("downloadUrl") as String
                           val routeDuration = document.get("duration") as String
                           val routeDistance = document.get("distance") as String
                           val startLocation = document.get("start_location") as String
                           val endLocation = document.get("end_location") as String
                           val timestamp = document.get("date") as Timestamp
                           val date = timestamp.toDate()


                           val post = Post(useremail,comment, downloadUrl, routeDuration,routeDistance,startLocation,endLocation,
                               date
                           )
                           postArrayList.add(post)
                       }
                       adapter!!.notifyDataSetChanged()

                   }
               }

            }
        }

    }
}
