package com.example.trackmyrun.view

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
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
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.trackmyrun.R
import com.example.trackmyrun.databinding.ActivityUploadBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.io.IOException
import java.util.*

class UploadActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    var selectedPicture : Uri? = null
    var selectedBitmap : Bitmap? = null
    private lateinit var db : FirebaseFirestore
    private lateinit var auth : FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var binding: ActivityUploadBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var sharedPreferences: SharedPreferences
    private var trackBoolean : Boolean? = null
    private var selectedLatitude : Double? = null
    private var selectedLongitude : Double? = null
    private var routeDuration : String? = null
    private var routeDistance : String? = null
    private var startLocation : String? = null
    private var endLocation : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        registerLauncher()

        sharedPreferences = this.getSharedPreferences("com.example.trackmyrun", MODE_PRIVATE)
        trackBoolean = false
        selectedLatitude = 0.0
        selectedLongitude = 0.0
        routeDuration = "time"
        routeDistance = "distance"
        startLocation = "start location"
        endLocation  = "end location"

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        auth = Firebase.auth
        db = Firebase.firestore
        storage = Firebase.storage

    }
    override fun onMapReady(googleMap: GoogleMap){
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        //casting
        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener{
            override fun onLocationChanged(location: Location) {
                trackBoolean = sharedPreferences.getBoolean("trackBoolean", false)
                if(trackBoolean == false){
                    val userLocation = LatLng(location.latitude,location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17f))
                    sharedPreferences.edit().putBoolean("trackBoolean",true).apply()
                }

            }
        }
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(binding.root, "Permission needed for location",Snackbar.LENGTH_INDEFINITE).setAction("Give permission"){
                    //request permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            }
            else{
                //request permission
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        else {
            //permission granted
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,500f,locationListener)
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if(lastLocation!= null){
                val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,17f))

            }
            mMap.isMyLocationEnabled = true
        }

    }

    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0))

        selectedLatitude = p0.latitude
        selectedLongitude = p0.longitude
        val selectedLocation = LatLng(p0.latitude,p0.longitude)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,500f,locationListener)
        val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        mMap.isMyLocationEnabled = true
        val path: MutableList<List<LatLng>> = ArrayList()
        val urlDirections = "https://maps.googleapis.com/maps/api/directions/json?origin=${lastLocation?.latitude},${lastLocation?.longitude}&destination=${selectedLocation.latitude},${selectedLocation.longitude}&key=<YOUR_API_KEY>"
        val directionsRequest = object : StringRequest(Method.GET, urlDirections, Response.Listener<String> {
                response ->
            val jsonResponse = JSONObject(response)
            // Get routes
            val routes = jsonResponse.getJSONArray("routes")
            val legs = routes.getJSONObject(0).getJSONArray("legs")
            val steps = legs.getJSONObject(0).getJSONArray("steps")
            routeDistance = legs.getJSONObject(0).getJSONObject("distance").getString("text").toString()
            routeDuration = legs.getJSONObject(0).getJSONObject("duration").getString("text").toString()
            startLocation = legs.getJSONObject(0).getString("start_address").toString()
            endLocation  = legs.getJSONObject(0).getString("end_address").toString()
            for (i in 0 until steps.length()) {
                val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                path.add(PolyUtil.decode(points))
            }
            for (i in 0 until path.size) {
                mMap.addPolyline(PolylineOptions().addAll(path[i]).color(Color.RED))
            }
        }, Response.ErrorListener {
                _ ->
        }){}
        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(directionsRequest)
    }


    fun imageViewClicked(view : View)  {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",
                    View.OnClickListener {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }).show()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)

        }

    }


    fun uploadClicked (view: View) {

        //UUID -> image name
        val uuid = UUID.randomUUID()
        val imageName = "$uuid.jpg"

        val storage = Firebase.storage
        val reference = storage.reference
        val imagesReference = reference.child("images").child(imageName)

        if (selectedPicture != null) {
            imagesReference.putFile(selectedPicture!!).addOnSuccessListener { taskSnapshot ->

                val uploadedPictureReference = storage.reference.child("images").child(imageName)
                uploadedPictureReference.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    println(downloadUrl)

                    val postMap = hashMapOf<String,Any>()
                    postMap.put("downloadUrl",downloadUrl)
                    postMap.put("userEmail",auth.currentUser!!.email.toString())
                    postMap.put("comment",binding.uploadCommentText.text.toString())
                    postMap.put("date",Timestamp.now())
                    postMap.put("distance", routeDistance!!)
                    postMap.put("duration", routeDuration!!)
                    postMap.put("start_location", startLocation!!)
                    postMap.put("end_location", endLocation!!)

                    db.collection( "Posts").add(postMap).addOnCompleteListener{task ->

                        if (task.isComplete && task.isSuccessful) {
                            //back
                            finish()

                        }

                    }.addOnFailureListener{exception ->
                        Toast.makeText(applicationContext,exception.localizedMessage,Toast.LENGTH_LONG).show()
                    }


                }

            }

        }


    }

    private fun registerLauncher() {
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    selectedPicture = intentFromResult.data
                    try {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(
                                this@UploadActivity.contentResolver,
                                selectedPicture!!
                            )
                            selectedBitmap = ImageDecoder.decodeBitmap(source)
                            binding.uploadImageView.setImageBitmap(selectedBitmap)
                        } else {
                            selectedBitmap = MediaStore.Images.Media.getBitmap(
                                this@UploadActivity.contentResolver,
                                selectedPicture
                            )
                            binding.uploadImageView.setImageBitmap(selectedBitmap)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { result ->
            if (result) {
                //permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if(lastLocation!= null){
                        val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,17f))
                    }
                    mMap.isMyLocationEnabled = true

                }

            } else {
                //permission denied
                Toast.makeText(this@UploadActivity, "Permisson needed!", Toast.LENGTH_LONG).show()
            }
        }
    }

}
