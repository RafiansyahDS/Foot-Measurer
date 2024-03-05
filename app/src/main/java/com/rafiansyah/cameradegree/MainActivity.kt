package com.rafiansyah.cameradegree

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.*
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.ux.ArFragment
import com.rafiansyah.cameradegree.Deskripsi.Companion.DEGREETAG
import com.rafiansyah.cameradegree.Deskripsi.Companion.DISTANCETAG
import com.rafiansyah.cameradegree.Deskripsi.Companion.ITEMTAG
import com.rafiansyah.cameradegree.databinding.ActivityMainBinding
import com.rafiansyah.cameradegree.util.PixelGridView
import com.rafiansyah.cameradegree.util.convertYuvToBitmap
import com.rafiansyah.cameradegree.util.resizeBitmap
import com.rafiansyah.cameradegree.util.rotateBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {
    private lateinit var sensorManager: SensorManager
    private lateinit var  accelerometer: Sensor
    lateinit var binding: ActivityMainBinding
    private var degree : Int = 0
    private var clicked : Boolean = false
    private lateinit var arFragment : ArFragment
    private var anchor : Anchor? = null
    private var isTracking = false
    private var isHitting = false
    private var distanceInCm : Double? = null
    private lateinit var session: Session

    private val sensorEventListener = object : SensorEventListener{
        override fun onSensorChanged(event: SensorEvent) {
            val z = event.values[2]

            val k = 90/9.8
            val zdeg = round(abs(z * k)).toInt()
            degree = zdeg
            binding.degree.text = "$degree°"
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        arFragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        checkCameraPermission()
        checkARAvailable()
        getSurfaceDistanceFromCamera()
        notifySurfaceScan()
        binding.captureImage.setOnClickListener{
            takePhoto()
        }

        binding.btnShowDot.setOnClickListener {
            clicked = !clicked
            if (clicked){
                binding.centerDot.visibility = View.VISIBLE
            }else{
                binding.centerDot.visibility = View.INVISIBLE
            }
        }

        binding.edtGridLine.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                //Perform Code
                val gCount = binding.edtGridLine.let { return@let if (it.text.isNullOrEmpty()) 1 else {
                    if (it.text.toString().toInt() <= 0){
                        return@let 1
                    }
                    else{
                        return@let it.text.toString().toInt()
                    }
                }}
                binding.holderGrid.removeAllViews()
                createGrid(gCount)
                return@OnKeyListener true
            }
            false
        })
        setContentView(view)
    }

    override fun onResume() {
        super.onResume()
        session = Session(applicationContext)
        //binding.captureImage.setOnClickListener {
//            //tes rumus
//            val takenDegrees = 90 * PI / 180
//            val distance = 80
//            val objectSizeOnImage = 11.5
//            val const90 = 90 * PI / 180
//            val focalLength = 34
//            Log.e("HASIL RUMUS", "${distance*objectSizeOnImage/focalLength* cos(const90-takenDegrees)}")
        //}
        session.setARMode()
        sensorManager.registerListener(sensorEventListener,accelerometer,SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorEventListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        session.close()
    }

    private fun checkCameraPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.INTERNET),
                PERMISSION_REQUEST_CAMERA)
        }
    }

    private fun createGrid(Gcount: Int){
        binding.holderGrid.addView(PixelGridView(this, Gcount))
    }

    private fun checkARAvailable(){
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            Handler(Looper.getMainLooper()).postDelayed({
                checkARAvailable()
            }, 200)
        }
        binding.mArButton.text = if (isARCoreSupportedAndUpToDate()) "Sup" else ""
        if (availability.isSupported) {
            binding.mArButton.visibility = View.VISIBLE
            binding.mArButton.isEnabled = true
        } else { // The device is unsupported or unknown.
            binding.mArButton.visibility = View.INVISIBLE
            binding.mArButton.isEnabled = false
        }
    }

    // Verify that ARCore is installed and using the current version.
    private fun isARCoreSupportedAndUpToDate(): Boolean {
        return when (ArCoreApk.getInstance().checkAvailability(this)) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                try {
                    // Request ARCore installation or update if needed.
                    when (ArCoreApk.getInstance().requestInstall(this, true)) {
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                            Log.i("ARcore", "ARCore installation requested.")
                            false
                        }
                        ArCoreApk.InstallStatus.INSTALLED -> true
                    }
                } catch (e: UnavailableException) {
                    Log.e("ARcore", "ARCore not installed", e)
                    false
                }
            }

            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE ->
                // This device is not supported for AR.
                false

            ArCoreApk.Availability.UNKNOWN_CHECKING -> {
                // ARCore is checking the availability with a remote query.
                // This function should be called again after waiting 200 ms to determine the query result.
                false
            }
            ArCoreApk.Availability.UNKNOWN_ERROR, ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                // There was an error checking for AR availability. This may be due to the device being offline.
                // Handle the error appropriately.
                false
            }
        }
    }


    private fun notifySurfaceScan(){
        arFragment.planeDiscoveryController.hide()
        arFragment.planeDiscoveryController.setInstructionView(null)
        arFragment.arSceneView.planeRenderer.isEnabled = false
    }

    private fun Session.setARMode(){
        val config = Config(this)
        pause()
        config.focusMode = Config.FocusMode.AUTO
        resume()
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        configure(config)
        arFragment.arSceneView.setupSession(this)
    }


    private fun getSurfaceDistanceFromCamera(){
        arFragment.arSceneView.scene.addOnUpdateListener {
            val trackingState = arFragment.arSceneView.arFrame?.camera?.trackingState

            isTracking = trackingState == TrackingState.TRACKING
        }

        // Set an onDrawListener to check if the screen is hitting a plane
        arFragment.arSceneView.scene.addOnUpdateListener {
            if (!isTracking) {
                return@addOnUpdateListener
            }

            val frame = arFragment.arSceneView.arFrame

            if (frame != null) {
                // Perform a hit test at the center of the screen
                val hits = frame.hitTest(arFragment.arSceneView.width / 2f, arFragment.arSceneView.height / 2f)

                // If a hit is found, place an anchor at the hit point
                if (hits.isNotEmpty()) {
                    isHitting = true

                    val hit = hits[0]

                    anchor = arFragment.arSceneView.session?.createAnchor(hit.hitPose)

                } else {
                    isHitting = false
                }
            }
            arFragment.arSceneView.scene.addOnUpdateListener SearchDistance@{
                if (!isTracking || !isHitting || anchor == null) {
                    return@SearchDistance
                }

                val cameraPose = arFragment.arSceneView.arFrame?.camera?.pose
                val anchorPose = anchor?.pose

                if (cameraPose != null && anchorPose != null) {
                    val cameraTranslation = cameraPose.extractTranslation()
                    val anchorTranslation = anchorPose.extractTranslation()

                    // Calculate the distance between the camera and the anchor in meters
                    val distance = sqrt(
                        (cameraTranslation.translation[0] - anchorTranslation.translation[0]).toDouble().pow(2.0) +
                        (cameraTranslation.translation[1] - anchorTranslation.translation[1]).toDouble().pow(2.0) +
                        (cameraTranslation.translation[2] - anchorTranslation.translation[2]).toDouble().pow(2.0)
                    )
                    binding.distance.text = "%.2f meter".format(distance)
                    distanceInCm = (distance * 100)
                }
            }
        }
    }

    private fun takePhoto(){
        if (distanceInCm != null && isHitting) {
            val image = arFragment.arSceneView.arFrame!!.acquireCameraImage()
            val bitmap = rotateBitmap(convertYuvToBitmap(image),true)
            deskripsiIntent(bitmap)


            //SAVE IMAGE
//            val imageFileName = createFile(application, degree, distanceInCm!!)
//            try {
//                val outputStream = FileOutputStream(imageFileName)
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//                outputStream.close()
//
//            } catch (e: Exception) {
//                Log.e("File Output Error:", e.message.toString())
//            }
//            val savedUri = Uri.fromFile(imageFileName)
//            Toast.makeText(applicationContext, "Saved in $savedUri", Toast.LENGTH_SHORT).show()
            image.close()
        }
    }



    private fun deskripsiIntent(item: Bitmap){
        val deskripsi = Intent(this, Deskripsi::class.java)
        deskripsi.putExtra(DEGREETAG,degree)
        deskripsi.putExtra(DISTANCETAG, distanceInCm)
        val stream = ByteArrayOutputStream()
        item.compress(Bitmap.CompressFormat.PNG,100,stream)
        val gambar = stream.toByteArray()
        deskripsi.putExtra(ITEMTAG, gambar)
        startActivity(deskripsi)
    }

    companion object{
        const val PERMISSION_REQUEST_CAMERA = 1
    }
}