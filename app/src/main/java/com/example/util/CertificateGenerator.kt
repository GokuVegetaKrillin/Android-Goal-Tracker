package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.GoalProgressCalculator
import com.example.data.model.GoalEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CertificateGenerator {

    fun generateCertificateBitmap(
        goal: GoalEntity,
        totalLoggedSeconds: Long,
        achieverName: String = "Goal Champion"
    ): Bitmap {
        val width = 1400
        val height = 980
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background - warm luxury parchment
        val bgPaint = Paint().apply {
            color = Color.parseColor("#FCFAF5")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Outer ornate border (Deep Navy)
        val outerBorderPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.STROKE
            strokeWidth = 14f
        }
        canvas.drawRect(30f, 30f, width - 30f, height - 30f, outerBorderPaint)

        // Inner ornate border (Gold)
        val goldBorderPaint = Paint().apply {
            color = Color.parseColor("#D97706")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(45f, 45f, width - 45f, height - 45f, goldBorderPaint)

        // Corner accents
        val cornerPaint = Paint().apply {
            color = Color.parseColor("#B45309")
            style = Paint.Style.FILL
        }
        val cornerSize = 25f
        canvas.drawRect(45f, 45f, 45f + cornerSize, 45f + cornerSize, cornerPaint)
        canvas.drawRect(width - 45f - cornerSize, 45f, width - 45f, 45f + cornerSize, cornerPaint)
        canvas.drawRect(45f, height - 45f - cornerSize, 45f + cornerSize, height - 45f, cornerPaint)
        canvas.drawRect(width - 45f - cornerSize, height - 45f - cornerSize, width - 45f, height - 45f, cornerPaint)

        // Title: CERTIFICATE OF COMPLETION
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 52f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.15f
        }
        canvas.drawText("CERTIFICATE OF COMPLETION", width / 2f, 150f, titlePaint)

        // Subtitle line
        val sublinePaint = Paint().apply {
            color = Color.parseColor("#D97706")
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(width / 2f - 240f, 175f, width / 2f + 240f, 175f, sublinePaint)

        // Presentation text
        val presentPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("THIS CERTIFICATE IS PROUDLY PRESENTED FOR OUTSTANDING DEDICATION TO", width / 2f, 240f, presentPaint)

        // Achiever Name
        val namePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 54f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(achieverName, width / 2f, 320f, namePaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 2f
        }
        canvas.drawLine(width / 2f - 350f, 350f, width / 2f + 350f, 350f, dividerPaint)

        // Text: For accomplishing the goal of
        canvas.drawText("FOR MASTERING AND SUCCESSFULLY COMPLETING THE TIME TARGET FOR", width / 2f, 410f, presentPaint)

        // Goal Name Highlight Box
        val goalHighlightPaint = Paint().apply {
            color = try {
                Color.parseColor(goal.colorHex)
            } catch (e: Exception) {
                Color.parseColor("#8B5CF6")
            }
            alpha = 30
            style = Paint.Style.FILL
        }
        val goalBox = RectF(width / 2f - 420f, 450f, width / 2f + 420f, 540f)
        canvas.drawRoundRect(goalBox, 16f, 16f, goalHighlightPaint)

        val goalStrokePaint = Paint().apply {
            color = try {
                Color.parseColor(goal.colorHex)
            } catch (e: Exception) {
                Color.parseColor("#8B5CF6")
            }
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(goalBox, 16f, 16f, goalStrokePaint)

        val goalNamePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 44f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(goal.name.uppercase(Locale.getDefault()), width / 2f, 510f, goalNamePaint)

        // Details: Total time and completion date
        val durationFormatted = GoalProgressCalculator.formatDuration(totalLoggedSeconds)
        val targetFormatted = GoalProgressCalculator.formatDuration(goal.targetSeconds)
        val detailPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Total Time Invested: $durationFormatted  |  Goal Target: $targetFormatted (${goal.recurrenceType.displayName})", width / 2f, 610f, detailPaint)

        // Seal / Gold Ribbon Badge in Bottom Center
        val sealPaint = Paint().apply {
            color = Color.parseColor("#D97706")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(width / 2f, 760f, 65f, sealPaint)

        val sealInnerPaint = Paint().apply {
            color = Color.parseColor("#FEF3C7")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(width / 2f, 760f, 55f, sealInnerPaint)

        val sealTextPaint = Paint().apply {
            color = Color.parseColor("#92400E")
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("VERIFIED", width / 2f, 755f, sealTextPaint)
        canvas.drawText("SUCCESS", width / 2f, 778f, sealTextPaint)

        // Date and Signature Lines
        val dateString = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(
            Date(goal.completedAt ?: System.currentTimeMillis())
        )

        val footerTextPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Left signature / Date
        canvas.drawLine(150f, 820f, 420f, 820f, dividerPaint)
        canvas.drawText(dateString, 285f, 810f, footerTextPaint)
        canvas.drawText("DATE OF COMPLETION", 285f, 850f, footerTextPaint)

        // Right signature / App Stamp
        canvas.drawLine(width - 420f, 820f, width - 150f, 820f, dividerPaint)
        canvas.drawText("Goal Tracker Official", width - 285f, 810f, footerTextPaint)
        canvas.drawText("AUTHENTICATED TRACKER", width - 285f, 850f, footerTextPaint)

        return bitmap
    }

    fun saveCertificateToDevice(context: Context, bitmap: Bitmap, goalName: String): Uri? {
        val sanitizedName = goalName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val filename = "Certificate_${sanitizedName}_${System.currentTimeMillis()}.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GoalTracker")
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                return uri
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "GoalTracker").apply { mkdirs() }
            val file = File(appDir, filename)
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            return Uri.fromFile(file)
        }
        return null
    }
}
