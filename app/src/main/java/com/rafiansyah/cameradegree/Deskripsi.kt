package com.rafiansyah.cameradegree

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rafiansyah.cameradegree.databinding.ActivityDeskripsiBinding
import com.rafiansyah.cameradegree.recyclerview.RvAdapter
import com.rafiansyah.cameradegree.retrofit.api.ApiConfig
import com.rafiansyah.cameradegree.retrofit.response.PrediksiResponseItem
import com.rafiansyah.cameradegree.util.convertBitmapToJpg
import com.rafiansyah.cameradegree.util.resizeBitmap
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos


class Deskripsi : AppCompatActivity() {
    private lateinit var binding: ActivityDeskripsiBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeskripsiBinding.inflate(layoutInflater)
        val degree = intent.getIntExtra(DEGREETAG, 0)
        val distance = intent.getDoubleExtra(DISTANCETAG, 0.0)
        val image = intent.getByteArrayExtra(ITEMTAG)
        if (image != null) {
            val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
            val jpgImageData = convertBitmapToJpg(resizeBitmap(bitmap))
            val jpgmultipart: MultipartBody.Part = MultipartBody.Part.createFormData(
                "photo",
                "image.jpg",
                jpgImageData
            )

            val apiService = ApiConfig().getApiService()
            val upload = apiService.uploadImage(jpgmultipart)
            setProgressBar(true)
            upload.enqueue(object : Callback<ArrayList<PrediksiResponseItem>> {
                override fun onResponse(
                    call: Call<ArrayList<PrediksiResponseItem>>,
                    response: Response<ArrayList<PrediksiResponseItem>>
                ) {
                    val responseBody = response.body()
                    if (response.isSuccessful){
                        setProgressBar(false)
                        if (!responseBody.isNullOrEmpty()) {
                            handleView(responseBody, degree, distance)
                        }else{
                            Toast.makeText(applicationContext, resources.getString(R.string.nofound), Toast.LENGTH_SHORT).show()
                            toArActivity()
                        }
                    }else{
                        setProgressBar(false)
                        Toast.makeText(applicationContext, resources.getString(R.string.reterror), Toast.LENGTH_SHORT).show()
                        toArActivity()
                    }
                }

                override fun onFailure(call: Call<ArrayList<PrediksiResponseItem>>, t: Throwable) {
                    toArActivity()
                    Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
                }

            })
        }
        setContentView(binding.root)
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun handleView(item: ArrayList<PrediksiResponseItem>, degree: Int, distance: Double){
        val horizontalLayoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPrediksi.layoutManager = horizontalLayoutManager
        binding.rvPrediksi.adapter = RvAdapter(item,distance)
        var highest: Double = 0.0
        item.forEach {
            if (highest == 0.0 || it.panjang > highest) {
                highest = it.panjang
            }
        }
        binding.tvRealSize.text = "UKURAN = ${rumus(degree, highest, distance)}cm"
        binding.tvDegree.text = degree.toString()
        binding.tvJarak.text = distance.toString()
    }

    private fun rumus(degree: Int, panjang: Double, jarak: Double): Double{
        val focal_length = 34.7 //tergantung spesifikasi device
        val realsize = (panjang/31 * jarak) / focal_length
        val normaldegree = Math.toRadians(90.0 - degree)
        val lambert = cos(normaldegree)
        val normalsize = realsize / lambert
        return normalsize
    }

    inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? = when {
        SDK_INT >= 33 -> getParcelableArrayList(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
    }

    inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? = when {
        SDK_INT >= 33 -> getParcelableArrayListExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
    }

    private fun setProgressBar(visible: Boolean){
        if (visible){
            binding.loadingBox.visibility = View.VISIBLE
        }else{
            binding.loadingBox.visibility = View.INVISIBLE
        }
    }

    private fun toArActivity(){
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    companion object{
        const val ITEMTAG = "ITEM"
        const val DEGREETAG = "degree"
        const val DISTANCETAG = "distance"
    }
}