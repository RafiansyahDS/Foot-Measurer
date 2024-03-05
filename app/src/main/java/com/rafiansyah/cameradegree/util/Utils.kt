package com.rafiansyah.cameradegree.util

import android.app.Application
import android.graphics.*
import android.media.Image
import com.rafiansyah.cameradegree.R
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.util.Base64


private const val FILENAME_FORMAT = "_dd-MMM-yyyy-hh_mm_ss"

val timeStamp: String = SimpleDateFormat(
    FILENAME_FORMAT,
    Locale.US
).format(System.currentTimeMillis())

fun createFile(application: Application,degree:Int,distance:Int): File {
    val mediaDir = application.externalMediaDirs.firstOrNull()?.let {
        File(it, application.resources.getString(R.string.app_name)).apply { mkdirs() }
    }

    val outputDirectory = if (
        mediaDir != null && mediaDir.exists()
    ) mediaDir else application.filesDir

    return File(outputDirectory, "${degree}_${distance}cm$timeStamp.jpg")
}

fun rotateBitmap(bitmap: Bitmap, isBackCamera: Boolean = false): Bitmap {
    val matrix = Matrix()
    return if (isBackCamera) {
        matrix.postRotate(90f)
        Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    } else {
        matrix.postRotate(-90f)
        matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
}

fun convertYuvToBitmap(image: Image): Bitmap {
    val yBuffer = image.planes[0].buffer // Y plane
    val uBuffer = image.planes[1].buffer // U plane
    val vBuffer = image.planes[2].buffer // V plane

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    // Copy Y plane
    yBuffer.get(nv21, 0, ySize)

    // Copy VU plane (interleaved)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
    val imageBytes = out.toByteArray()

    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun resizeBitmap(bitmap: Bitmap): Bitmap {
    var image = bitmap
    val maxHeight = 1440
    val maxWidth = 1080
    return if (maxHeight > 0 && maxWidth > 0) {
        val width = image.width
        val height = image.height
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
        var finalWidth = maxWidth
        var finalHeight = maxHeight
        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }
        image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
        image
    } else {
        image
    }
}
fun convertBitmapToJpg(bitmap: Bitmap): RequestBody {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
}

fun convert64toIMG(base64S: String) : Bitmap{
    val decodedBytes = Base64.decode(base64S, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
}