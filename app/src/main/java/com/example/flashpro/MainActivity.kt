package com.example.flashpro

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private val CAMERA_REQUEST = 100
    private var isFlashOn = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.main)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val menuIcon = findViewById<ImageView>(R.id.menuIcon)

        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.rating -> {
                    toast("Rating soon")
                }

                R.id.privacy -> {
                    toast("Privacy Soon")
                }

                R.id.notification -> {
                    toast("Notification Soon")
                }

                R.id.share -> {
                    toast("Sharing Soon")
                }

                R.id.exit -> {
                    finish()
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START)

            true
        }

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.firstOrNull()

        val flashBtnCard = findViewById<MaterialCardView>(R.id.torch_btn)
        val flashBtn = findViewById<ImageButton>(R.id.torchBtnImage)
        updateButtonImage(flashBtn, flashBtnCard)

        flashBtn.setOnClickListener {
            if (hasCameraPermission()) {

                isFlashOn = !isFlashOn
                toggleFlashLight(isFlashOn)
                updateButtonImage(flashBtn, flashBtnCard)
            } else {
                requestCameraPermission()
            }
        }

    }

    private fun toggleFlashLight(state: Boolean) {
        try {
//            if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){  //SDK also greater tha 24
//            }
            cameraManager.setTorchMode(cameraId ?: return, state)
        } catch (e: Exception) {
            toast("Flash Light not Supported")
        }
    }

    private fun updateButtonImage(button: ImageButton, cardView: MaterialCardView) {
        if (isFlashOn) {
            button.setImageResource(R.mipmap.power_on)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cardView.outlineSpotShadowColor =
                    ContextCompat.getColor(this, R.color.elevate_color_on)

                cardView.outlineAmbientShadowColor =
                    ContextCompat.getColor(this, R.color.elevate_color_on)
            }
            toast("Flash Turned ON")
        } else {
            button.setImageResource(R.mipmap.power_off)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cardView.outlineSpotShadowColor =
                    ContextCompat.getColor(this, R.color.elevate_color)

                cardView.outlineAmbientShadowColor =
                    ContextCompat.getColor(this, R.color.elevate_color)
            }
            toast("Flash Turned OFF")
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        if (requestCode == CAMERA_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toast("Permission Granted")
            } else {
                toast("Permission Required!")
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
    }

    fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}