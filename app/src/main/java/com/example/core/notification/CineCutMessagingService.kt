package com.example.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Production Firebase Cloud Messaging Service for CineCut.
 * Handles device registration token lifecycle, background push notifications,
 * and collaborative multi-user alerts.
 */
class CineCutMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "CineCutFCM"
        const val CHANNEL_ID = "cinecut_notifications_channel"
        const val CHANNEL_NAME = "CineCut Studio Notifications"
        const val CHANNEL_DESC = "Notifications for project collaboration, render progress, CineCoins, and admin announcements."
        const val PREFS_NAME = "cinecut_firebase_prefs"
        const val KEY_FCM_TOKEN = "fcm_registration_token"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESC
                    enableLights(true)
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Registration Token received: $token")

        // 1. Cache token locally in SharedPreferences
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()

        // 2. Sync token to cloud Firestore if user is currently authenticated
        syncTokenToCloud(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "CineCut Studio Alert"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "New activity in your CineCut workspace."

        val type = remoteMessage.data["type"] ?: "SYSTEM"
        val metadata = remoteMessage.data

        showNotification(title, body, type, metadata)
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        data: Map<String, String>
    ) {
        createNotificationChannel(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", type)
            for ((key, value) in data) {
                putExtra("fcm_$key", value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun syncTokenToCloud(token: String) {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) return
            val user = FirebaseAuth.getInstance().currentUser ?: return
            val uid = user.uid

            val tokenData = hashMapOf(
                "fcmToken" to token,
                "fcmUpdatedAt" to System.currentTimeMillis()
            )

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(tokenData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully synced FCM token to Firestore for user $uid")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync FCM token to Firestore: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.w(TAG, "Notice during FCM token sync: ${e.message}")
        }
    }
}
