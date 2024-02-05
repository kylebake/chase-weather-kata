package com.chaseweatherkata.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream

class BitmapHelper {
    fun decodeStream(inputStream: InputStream): Bitmap {
        return BitmapFactory.decodeStream(inputStream)
    }
}