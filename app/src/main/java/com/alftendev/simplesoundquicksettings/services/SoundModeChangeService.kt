package com.alftendev.simplesoundquicksettings.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.alftendev.simplesoundquicksettings.MainActivity
import com.alftendev.simplesoundquicksettings.R
import com.alftendev.simplesoundquicksettings.utils.Utils

class SoundModeChangeService : Service() {

    companion object {
        private const val CHANNEL_ID = "sound_mode_changes"
        private const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()

        try {
            if (Utils.isDoNotDisturbPermissionGranted(this)) {
                changeSoundMode()
            }

            TileService.requestListeningState(
                this,
                ComponentName(this, SoundTile::class.java)
            )
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    // AGP 8.13 does not associate the specialUse type with this service, even
    // though the merged and packaged manifests retain the declaration.
    @SuppressLint("ForegroundServiceType")
    private fun startInForeground() {
        createNotificationChannel()
        val notification = createNotification()

        val foregroundServiceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            foregroundServiceType
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.sound_mode_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_notifications_active_24)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.sound_mode_notification_text))
            .setContentIntent(openAppIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun changeSoundMode() {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager

        audio.ringerMode = when (audio.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE

            AudioManager.RINGER_MODE_VIBRATE -> {
                audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
                AudioManager.RINGER_MODE_SILENT
            }

            AudioManager.RINGER_MODE_SILENT -> AudioManager.RINGER_MODE_NORMAL

            else -> return
        }
    }
}
