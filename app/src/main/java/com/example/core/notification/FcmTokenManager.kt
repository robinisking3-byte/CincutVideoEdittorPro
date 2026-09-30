package com.example.core.notification

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Manages Firebase Cloud Messaging registration tokens, topic subscriptions,
 * and user association for the CineCut ecosystem.
 */
class FcmTokenManager(private val context: Context) {

    private val tag = "FcmTokenManager"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs = context.getSharedPreferences(CineCutMessagingService.PREFS_NAME, Context.MODE_PRIVATE)

    private val _fcmToken = MutableStateFlow<String?>(
        prefs.getString(CineCutMessagingService.KEY_FCM_TOKEN, null)
    )
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    init {
        fetchCurrentToken()
    }

    /**
     * Fetch current device FCM token on initialization
     */
    fun fetchCurrentToken() {
        if (FirebaseApp.getApps(context).isEmpty()) return
        scope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                if (!token.isNullOrBlank()) {
                    prefs.edit().putString(CineCutMessagingService.KEY_FCM_TOKEN, token).apply()
                    _fcmToken.value = token
                    Log.d(tag, "FCM registration token fetched: $token")
                }
            } catch (e: Exception) {
                Log.w(tag, "Notice: FCM token fetch: ${e.message}")
            }
        }
    }

    /**
     * Associate the active user with their current FCM token in Firestore
     * and subscribe to common creator topics.
     */
    fun registerUserFcmToken(userId: String) {
        if (userId.isBlank() || FirebaseApp.getApps(context).isEmpty()) return
        scope.launch {
            try {
                val token = try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (_: Exception) {
                    prefs.getString(CineCutMessagingService.KEY_FCM_TOKEN, null)
                }

                if (!token.isNullOrBlank()) {
                    _fcmToken.value = token
                    prefs.edit().putString(CineCutMessagingService.KEY_FCM_TOKEN, token).apply()

                    // Update user document with token
                    val tokenData = hashMapOf(
                        "fcmToken" to token,
                        "fcmUpdatedAt" to System.currentTimeMillis()
                    )
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .set(tokenData, SetOptions.merge())
                        .await()
                    Log.d(tag, "Successfully bound FCM token to user: $userId")
                }

                // Subscribe to community announcements & festival campaigns
                subscribeToTopic("cinecut_community")
                subscribeToTopic("festival_offers")
            } catch (e: Exception) {
                Log.w(tag, "Notice: Error registering user FCM token: ${e.message}")
            }
        }
    }

    /**
     * Subscribe this device to an FCM topic
     */
    fun subscribeToTopic(topic: String) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnSuccessListener {
                    Log.d(tag, "Subscribed to FCM topic: $topic")
                }
                .addOnFailureListener { e ->
                    Log.w(tag, "Failed to subscribe to topic '$topic': ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(tag, "Notice: Topic subscribe error: ${e.message}")
        }
    }

    /**
     * Unsubscribe from an FCM topic
     */
    fun unsubscribeFromTopic(topic: String) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnSuccessListener {
                    Log.d(tag, "Unsubscribed from FCM topic: $topic")
                }
        } catch (e: Exception) {
            Log.w(tag, "Notice: Topic unsubscribe error: ${e.message}")
        }
    }
}
