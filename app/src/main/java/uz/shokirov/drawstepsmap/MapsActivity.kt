package uz.shokirov.drawstepsmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.github.florent37.runtimepermission.kotlin.askPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.Task
import uz.shokirov.cash.MySharedPreferences
import uz.shokirov.service.LocationService
import java.util.concurrent.TimeUnit

private const val TAG = "MapsActivity"

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        val workRequest: WorkRequest =
            PeriodicWorkRequestBuilder<LocationService>(15, TimeUnit.MINUTES)
                .build()
        var btn = findViewById<Button>(R.id.btnstart)
        btn.setOnClickListener {
            WorkManager.getInstance(this)
                .enqueue(workRequest)
        }
        btn.setOnLongClickListener {
            startActivity(Intent(this, ListActivity::class.java))
            true
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Add a marker in Sydney and move the camera
        myMethod()
        currentLocation()
        MySharedPreferences.init(this)
        var listPolyline = MySharedPreferences.obektString
        val polyline = mMap.addPolyline(
            PolylineOptions().clickable(true)
                .addAll(listPolyline)
                .color(Color.GREEN)
        )
        if (listPolyline.size >= 1) {
            polyline.points = listPolyline
            Log.d(TAG, "onMapReady: $listPolyline")
        }

    }

    @SuppressLint("MissingPermission")
    private fun currentLocation() {
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        val locationTask: Task<Location> = fusedLocationProviderClient.lastLocation
        locationTask.addOnSuccessListener { it: Location ->
            if (it != null) {
                var currentLatLng = LatLng(it.latitude, it.longitude)
                mMap.addMarker(
                    MarkerOptions()
                        .position(currentLatLng)
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 35f))
            }
        }
    }

    fun myMethod() {
        askPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) {
            //all permissions already granted or just granted

        }.onDeclined { e ->
            if (e.hasDenied()) {

                AlertDialog.Builder(this)
                    .setMessage("Please accept our permissions")
                    .setPositiveButton("yes") { dialog, which ->
                        e.askAgain();
                    } //ask again
                    .setNegativeButton("no") { dialog, which ->
                        dialog.dismiss();
                    }
                    .show();
            }

            if (e.hasForeverDenied()) {
                //the list of forever denied permissions, user has check 'never ask again'

                // you need to open setting manually if you really need it
                e.goToSettings();
            }
        }

    }

}