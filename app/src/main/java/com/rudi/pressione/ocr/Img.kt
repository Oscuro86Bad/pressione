package com.rudi.pressione.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

object Img {

    /** Carica una foto ridimensionandola per l'OCR (max ~1600px, ingrandita se troppo piccola). */
    fun carica(file: File, maxLato: Int = 1600): Bitmap? {
        val limiti = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, limiti)
        if (limiti.outWidth <= 0 || limiti.outHeight <= 0) return null

        var campione = 1
        while (maxOf(limiti.outWidth, limiti.outHeight) / (campione * 2) >= maxLato) campione *= 2

        val bmp = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = campione }
        ) ?: return null

        val rotazione = try {
            ExifInterface(file.absolutePath).rotationDegrees
        } catch (_: Exception) {
            0
        }
        val ruotata = if (rotazione != 0) {
            Bitmap.createBitmap(
                bmp, 0, 0, bmp.width, bmp.height,
                Matrix().apply { postRotate(rotazione.toFloat()) }, true
            )
        } else bmp

        // I display dei misuratori fotografati da vicino possono risultare piccoli: ingrandiamo
        val minLato = minOf(ruotata.width, ruotata.height)
        return if (minLato < 700) {
            val scala = minOf(2200f / minLato, 3f)
            Bitmap.createScaledBitmap(
                ruotata,
                (ruotata.width * scala).toInt(),
                (ruotata.height * scala).toInt(),
                true
            )
        } else ruotata
    }
}
