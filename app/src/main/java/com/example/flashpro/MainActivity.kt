package com.example.flashpro

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import android.view.View
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private val CAMERA_REQUEST = 100
    private var isFlashOn = false
    private lateinit var flashBtn: ImageButton
    private lateinit var flashBtnCard: MaterialCardView
    private lateinit var frequencySeekBar: SeekBar
    private lateinit var seekBarTitle: TextView
    private lateinit var minValueText: TextView
    private lateinit var maxValueText: TextView
    private lateinit var seekBarCard: CardView

    private val handler = Handler(Looper.getMainLooper())
    private var flashRunnable: Runnable? = null
    private var isFlashing = false
    private var blinkInterval: Long = 500   // default 500ms
    private var flashMode = "NORMAL" // NORMAL or RHYTHM

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var callSwitch: Switch

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var messageSwitch: Switch
    private lateinit var notificationManager: NotificationManager

    // Callback to listen for torch mode changes (system-wide)
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            if (cameraId == this@MainActivity.cameraId) {
                isFlashOn = enabled
                // Only update UI automatically in NORMAL mode to avoid flickering in RHYTHM mode
                if (flashMode == "NORMAL") {
                    updateButtonImage(flashBtn, flashBtnCard)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Handle the Drawer layout and NavigationView
        drawerLayout = findViewById(R.id.main)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val menuIcon = findViewById<ImageView>(R.id.menuIcon)

        // Handle Menu Icon Click
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.rating -> toast("Rating soon")
                R.id.privacy -> toast("Privacy Soon")
                R.id.share -> toast("Sharing Soon")
                R.id.exit -> finish()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        //Handle Camera Services
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.firstOrNull()

        flashBtnCard = findViewById(R.id.torch_btn)
        flashBtn = findViewById(R.id.torchBtnImage)

        // Register the callback to sync with system flash state
        cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))

        flashBtn.setOnClickListener {
            if (hasCameraPermission()) {
                if (flashMode == "NORMAL") {
                    toggleFlashLight(!isFlashOn)
                } else {
                    // Rhythm mode logic: Start or Stop based on current state
                    if (isFlashing) {
                        stopRhythmFlash()
                    } else {
                        startRhythmFlash()
                    }
                }
            } else {
                requestCameraPermission()
            }
        }

        val flashModeBtn: CardView = findViewById(R.id.flash_mode_card)
        flashModeBtn.setOnClickListener {
            showFlashModeDialog()
        }

        //Handling Seekbar for Brightness and Speed
        frequencySeekBar = findViewById(R.id.seekBar)
        seekBarTitle = findViewById(R.id.textView9)
        minValueText = findViewById(R.id.min_value)
        maxValueText = findViewById(R.id.max_value)
        seekBarCard = findViewById(R.id.seekbar_card)

        updateSeekBarUI()

        frequencySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (flashMode == "NORMAL") {
                    if (isFlashOn) {
                        updateBrightness(progress)
                    }
                } else {
                    // convert progress to delay (inverse relationship: more progress = less delay)
                    blinkInterval = (1000 - progress * 9L).coerceAtLeast(50L)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        //Handle Notification Services
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        callSwitch = findViewById(R.id.call_switch)
        messageSwitch = findViewById(R.id.sms_switch)

        callSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!hasDndPermission()) {
                if (isChecked) {
                    buttonView.isChecked = false
                    requestDndPermission()
                }
                return@setOnCheckedChangeListener
            }

            // Note: Android's setInterruptionFilter applies to the entire device.
            if (isChecked) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                toast("Calls Muted")
            } else {
                // Only disable DND if the other switch is also off
                if (!messageSwitch.isChecked) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
                toast("Calls Allowed")
            }
        }

        messageSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!hasDndPermission()) {
                if (isChecked) {
                    buttonView.isChecked = false
                    requestDndPermission()
                }
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                toast("Messages Muted")
            } else {
                // Only disable DND if the other switch is also off
                if (!callSwitch.isChecked) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
                toast("Messages Allowed")
            }
        }

        val appSelectionCard: CardView = findViewById(R.id.app_selection_card)
        appSelectionCard.setOnClickListener {
            toast("Feature Coming Soon")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateSeekBarUI() {
        if (flashMode == "NORMAL") {
            val isSupported = isBrightnessControlSupported()
            if (isSupported) {
                seekBarCard.visibility = View.VISIBLE
                seekBarTitle.text = "Flash Brightness"
                minValueText.text = "Low"
                maxValueText.text = "High"
                frequencySeekBar.max = 10 
                frequencySeekBar.progress = 5
            } else {
                seekBarCard.visibility = View.GONE
            }
        } else {
            // Rhythm mode always shows the seekbar for speed
            seekBarCard.visibility = View.VISIBLE
            seekBarTitle.text = "Flashing Speed"
            minValueText.text = "0 ms"
            maxValueText.text = "1000 ms"
            frequencySeekBar.max = 100
            frequencySeekBar.progress = 20
        }
    }
    
    private fun isBrightnessControlSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return try {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId!!)
                val maxLevel = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                maxLevel > 1
            } catch (e: Exception) {
                false
            }
        }
        return false
    }

    private fun updateBrightness(level: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId!!)
                val maxLevel = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                
                if (maxLevel > 1) {
                    // Scale progress level (0-10) to camera max level
                    val scaledLevel = (level * maxLevel / 10).coerceIn(1, maxLevel)
                    cameraManager.turnOnTorchWithStrengthLevel(cameraId!!, scaledLevel)
                } else {
                    cameraManager.setTorchMode(cameraId!!, true)
                }
            } catch (e: Exception) {
                cameraManager.setTorchMode(cameraId!!, true)
            }
        } else {
            // Strength control requires API 33+
            cameraManager.setTorchMode(cameraId!!, true)
        }
    }

    private fun toggleFlashLight(state: Boolean) {
        try {
            if (state && flashMode == "NORMAL" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                updateBrightness(frequencySeekBar.progress)
            } else {
                cameraManager.setTorchMode(cameraId ?: return, state)
            }
        } catch (e: Exception) {
            toast("Flash Light not Supported")
        }
    }

    private fun updateButtonImage(button: ImageButton, cardView: MaterialCardView) {
        if (isFlashOn || isFlashing) {
            button.setImageResource(R.drawable.power_on)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cardView.outlineSpotShadowColor = ContextCompat.getColor(this, R.color.elevate_color_on)
                cardView.outlineAmbientShadowColor = ContextCompat.getColor(this, R.color.elevate_color_on)
            }
        } else {
            button.setImageResource(R.drawable.power_off)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cardView.outlineSpotShadowColor = ContextCompat.getColor(this, R.color.elevate_color)
                cardView.outlineAmbientShadowColor = ContextCompat.getColor(this, R.color.elevate_color)
            }
        }
    }

    private fun hasCameraPermission(): Boolean = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String?>, grantResults: IntArray, deviceId: Int
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

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.unregisterTorchCallback(torchCallback)
    }

    override fun onPause() {
        super.onPause()
        if (isFlashing) stopRhythmFlash()
    }

    private fun startRhythmFlash() {
        if (isFlashing) return
        val id = cameraId ?: run {
            toast("Flash not available")
            return
        }
        isFlashing = true
        // Set UI to ON state once when starting rhythm
        updateButtonImage(flashBtn, flashBtnCard)

        flashRunnable = object : Runnable {
            var currentTorchState = false
            override fun run() {
                try {
                    currentTorchState = !currentTorchState
                    cameraManager.setTorchMode(id, currentTorchState)
                    handler.postDelayed(this, blinkInterval)
                } catch (e: Exception) {
                    stopRhythmFlash()
                    toast("Flash error")
                }
            }
        }
        handler.post(flashRunnable!!)
    }

    private fun stopRhythmFlash() {
        flashRunnable?.let { handler.removeCallbacks(it) }
        try {
            cameraId?.let { cameraManager.setTorchMode(it, false) }
        } catch (_: Exception) {}
        isFlashing = false
        isFlashOn = false
        // Reset UI to OFF state
        updateButtonImage(flashBtn, flashBtnCard)
    }

    private fun showFlashModeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_flash_mode, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val normalRadio = dialogView.findViewById<RadioButton>(R.id.normal_mode)
        val rhythmRadio = dialogView.findViewById<RadioButton>(R.id.rhythm_mode)

        // Set initial state based on current flashMode
        if (flashMode == "RHYTHM") rhythmRadio.isChecked = true else normalRadio.isChecked = true

        normalRadio.setOnClickListener {
            if (flashMode != "NORMAL") {
                stopRhythmFlash()
                if (isFlashOn) toggleFlashLight(false)
                flashMode = "NORMAL"
                updateSeekBarUI()
                toast("Normal Mode Selected")
            }
            dialog.dismiss()
        }

        rhythmRadio.setOnClickListener {
            if (flashMode != "RHYTHM") {
                stopRhythmFlash()
                if (isFlashOn) toggleFlashLight(false)
                flashMode = "RHYTHM"
                updateSeekBarUI()
                toast("Rhythm Mode Selected")
                // Reset UI to OFF state since we stopped everything
                updateButtonImage(flashBtn, flashBtnCard)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun hasDndPermission(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    private fun requestDndPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        startActivity(intent)
        toast("Please allow Do Not Disturb access")
    }

    fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
