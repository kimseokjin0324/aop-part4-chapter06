package fastcampus.aop.part4.aop_part4_chapter06.appWidget

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import fastcampus.aop.part4.aop_part4_chapter06.R
import fastcampus.aop.part4.aop_part4_chapter06.data.Repository
import fastcampus.aop.part4.aop_part4_chapter06.data.models.airquality.Grade
import kotlinx.coroutines.launch
import java.lang.Exception

class SimpleAirQualityWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        ContextCompat.startForegroundService(
            context!!,
            Intent(context, UpdateWidgetService::class.java)
        )
    }

    class UpdateWidgetService : LifecycleService() {
        override fun onCreate() {
            super.onCreate()
            createChannelIfNeeded() //-API Level 26 이상에서는 CHANNEL 생성
            startForeground(
                NOTIFICATION_ID,
                createNotification()
            )

            //-> 이후 onStartCommand가 실행

        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

            //위치정보 가져오기
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                // 권한없을 경우
                val updateViews = RemoteViews(packageName, R.layout.widget_simple).apply {
                    setTextViewText(
                        R.id.resultTextView,
                        "권한없음"

                    )
                }
                updateWidget(updateViews)
                stopSelf()
                return super.onStartCommand(intent, flags, startId)
            }


            LocationServices.getFusedLocationProviderClient(this).lastLocation
                .addOnSuccessListener { location ->
                    // 코루틴 비동기처리
                    lifecycleScope.launch {
                        //- 예외처리 (성공시는  update 실패시 -> 그냥 에러안나고 유지하도록 )
                        try {
                            val nearbyMonitoringStation = Repository.getNearbyMonitoringStation(
                                location.latitude,
                                location.longitude
                            )
                            val measuredValue =
                                Repository.getLatestAirQualityData(nearbyMonitoringStation!!.stationName!!)

                            val updateViews =
                                RemoteViews(packageName, R.layout.widget_simple).apply {
                                    setViewVisibility(R.id.labelTextView, View.VISIBLE)
                                    setViewVisibility(R.id.gradeLabelTextView, View.VISIBLE)

                                    val currentGrade = (measuredValue?.khaiGrade ?: Grade.UNKNOWN)

                                    setTextViewText(R.id.resultTextView, currentGrade.emoji)
                                    setTextViewText(R.id.gradeLabelTextView, currentGrade.label)
                                }

                            updateWidget(updateViews)
                        } catch (exception: Exception) {
                            exception.printStackTrace()
                        } finally {
                            stopSelf()
                        }
                    }

                }

            return super.onStartCommand(intent, flags, startId)

        }

        override fun onDestroy() {
            super.onDestroy()
            stopForeground(true)
        }

        private fun createChannelIfNeeded() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)?.createNotificationChannel(
                    NotificationChannel(
                        WIDGET_REFRESH_CHANNEL_ID,
                        "위젯 갱신 채널",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }

        private fun createNotification(): Notification = NotificationCompat.Builder(this)
            .setChannelId(WIDGET_REFRESH_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_refresh_24)
            .build()

        private fun updateWidget(updateViews: RemoteViews) {
            val widgetProvider = ComponentName(this, SimpleAirQualityWidgetProvider::class.java)

            AppWidgetManager.getInstance(this).updateAppWidget(widgetProvider, updateViews)
        }
    }

    companion object {
        private const val WIDGET_REFRESH_CHANNEL_ID = "WIDGET_REFRESH_CHANNEL_ID"
        private const val NOTIFICATION_ID = 101
    }
}