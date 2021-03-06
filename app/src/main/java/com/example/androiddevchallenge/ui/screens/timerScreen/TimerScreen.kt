/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge.ui.screens.timerScreen

import android.annotation.SuppressLint
import android.app.Notification.FLAG_AUTO_CANCEL
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.androiddevchallenge.MainActivity
import com.example.androiddevchallenge.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.math.absoluteValue
import kotlin.math.ceil

const val MAX_HOURS = 100
const val MAX_MINUTES = 60
const val MAX_SECONDS = 60

const val INIT_LAZY_LIST_HOUR_INDEX = 5499
const val INIT_LAZY_LIST_MINUTE_INDEX = 5459
const val INIT_LAZY_LIST_SECOND_INDEX = 5459

const val CHANNEL_ID = "CHANNEL_ID"

object JobAndScope {
    val job = Job()
    val scope = CoroutineScope(job + Dispatchers.Main)
}

@ExperimentalAnimationApi
@Composable
fun TimerScreen() {
    val hourState = rememberLazyListState(INIT_LAZY_LIST_HOUR_INDEX)
    val minuteState = rememberLazyListState(INIT_LAZY_LIST_MINUTE_INDEX)
    val secondState = rememberLazyListState(INIT_LAZY_LIST_SECOND_INDEX)

    var hours by remember { mutableStateOf("00") }
    var minutes by remember { mutableStateOf("00") }
    var seconds by remember { mutableStateOf("00") }

    val (hasStarted, setHaStarted) = remember { mutableStateOf(false) }
    val (hasReset, setHasReset) = remember { mutableStateOf(true) }
    var remainingTime by remember { mutableStateOf(0) }

    if (!hasStarted && hasReset) {
        hours = timerDisplayValue(hourState, MAX_HOURS)
        minutes = timerDisplayValue(minuteState, MAX_MINUTES)
        seconds = timerDisplayValue(secondState, MAX_SECONDS)
    }

    val inMilliSeconds =
        (
            (hours.toLong() * 60 * 60 * 1000) +
                (minutes.toLong() * 60 * 1000) +
                (seconds.toLong() * 1000)
            )

    val context = LocalContext.current

    val timer: (Long) -> Unit = { fullTime ->

        var mutableFullTime = fullTime
        if (!hasReset) mutableFullTime = remainingTime.toLong()

        JobAndScope.scope.launch {
            withTimeout(mutableFullTime) {
                val startTime = System.currentTimeMillis()
                while (isActive) {
                    val milliseconds =
                        (mutableFullTime - (System.currentTimeMillis() - startTime))
                    remainingTime = milliseconds.toInt()

                    val rawSeconds = (ceil(milliseconds / 1000f).toInt() - 1)
                    seconds = (rawSeconds % 60).toString().padStart(2, '0')
                    minutes = ((rawSeconds / 60) % 60).toString().padStart(2, '0')
                    hours = ((rawSeconds / 60) / 60).toString().padStart(2, '0')

                    // on time out do the following:
                    if (rawSeconds == 0) {
                        setHaStarted(false)
                        createNotificationChannel(context = context)
                        displayNotification(context = context)
                    }

                    delay(1000)
                }
            }
        }
    }

    val showCounterWheels = !hasStarted && hasReset

    Column(
        modifier = Modifier
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            AnimatedVisibility(visible = !showCounterWheels) {
                CounterClock(
                    hours = hours,
                    minutes = minutes,
                    seconds = seconds,
                )
            }

            AnimatedVisibility(visible = showCounterWheels) {
                CounterInputWheels(
                    hourState = hourState,
                    minuteState = minuteState,
                    secondState = secondState,
                )
            }

            Spacer(modifier = Modifier.height(60.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(.8f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StartButton(
                    timer = timer,
                    hasStarted = hasStarted,
                    setHasStarted = setHaStarted,
                    setHasReseted = setHasReset,
                    remainingTime = inMilliSeconds,
                    clickLabel = "clicked start button",
                    enabled = inMilliSeconds != 0L,
                )

                ResetButton(
                    text = "CANCEL",
                    color = Color.LightGray,
                    onClick = {
                        setHaStarted(false)
                        JobAndScope.job.cancelChildren()
                        setHasReset(true)
                    },
                    clickLabel = "clicked reset button"
                )
            }
        }
    }
}

@Composable
fun CounterInputWheels(
    hourState: LazyListState,
    minuteState: LazyListState,
    secondState: LazyListState,
) {

    animateToClosestItem(hourState)
    animateToClosestItem(minuteState)
    animateToClosestItem(secondState)

    Column(
        modifier = Modifier
            .height(160.dp)
            .fillMaxWidth(.7f),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Hours")
            Text(text = "Minutes")
            Text(text = "Seconds")
        }
        Row(
            modifier = Modifier
                .height(150.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HourWheel(hourState)
            MinuteWheel(minuteState)
            SecondWheel(secondState)
        }
    }
}

fun createNotificationChannel(context: Context) {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    with(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timer Notification Channel"
            val descriptionText = "Timer Notification Channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@SuppressLint("UnspecifiedImmutableFlag")
fun displayNotification(context: Context) {
    with(context) {
//        val randomId = (0..1000000).random()
        val randomId = 0 // for single notification, use same id
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0,
            openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.timer_icon)
            .setContentTitle("Timer")
            .setContentText("time is up :)")
            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
            .setContentIntent(openAppPendingIntent)
//            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
        builder.build()

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(
                randomId,
                builder.build().apply {
                    flags = FLAG_AUTO_CANCEL
                }
            )
        }
    }
}

@Composable
fun CounterClock(
    hours: String = "00",
    minutes: String = "00",
    seconds: String = "00",
) {
    Column(
        modifier = Modifier
            .height(160.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "$hours:$minutes:$seconds",
            fontSize = 70.sp,
        )
    }
}

@Composable
fun StartButton(
    clickLabel: String = "clicked start button",
    remainingTime: Long = 10_000,
    timer: (Long) -> Unit,
    hasStarted: Boolean,
    setHasStarted: (Boolean) -> Unit,
    setHasReseted: (Boolean) -> Unit,
    enabled: Boolean = false,
) {
    val color by animateColorAsState(if (hasStarted) Color.Red else Color(0xFF008600))
    val text = if (hasStarted) "STOP" else "START"
    val onClick = {
        setHasStarted(!hasStarted)
        if (hasStarted) {
            JobAndScope.job.cancelChildren()
            setHasReseted(false)
        } else {
            timer(remainingTime)
        }
    }

    Button(
        text = text,
        color = color,
        clickLabel = clickLabel,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
fun ResetButton(
    text: String = "Start",
    color: Color = Color.Green,
    clickLabel: String = "clicked start button",
    onClick: () -> Unit,
) {
    Button(
        text = text,
        color = color,
        clickLabel = clickLabel,
        onClick = onClick,
    )
}

@Composable
fun Button(
    text: String = "Start",
    color: Color = Color.Green,
    clickLabel: String = "clicked start button",
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(120.dp)
            .border(
                width = 2.dp,
                color = color,
                shape = CircleShape
            )
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(
                enabled = enabled,
                onClickLabel = clickLabel,
                onClick = onClick
            )
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = text,
            textAlign = TextAlign.Center,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

// private fun resetClock(lazyState: LazyListState, initValue: Int) {
//    val scope = CoroutineScope(Dispatchers.Main)
//    scope.launch {
//        lazyState.animateScrollToItem(initValue)
//    }
// }

private fun animateToClosestItem(lazyState: LazyListState) {
    val offset = lazyState.firstVisibleItemScrollOffset
    val currentItem = lazyState.firstVisibleItemIndex
    val scope = CoroutineScope(Dispatchers.Main)

    if (!lazyState.isScrollInProgress) {
        scope.launch {
            when {
                offset > 0 -> lazyState.animateScrollToItem(currentItem)
                offset < 174 -> lazyState.animateScrollToItem(currentItem)
                else -> {
                }
            }
        }
    }
}

private fun timerDisplayValue(lazyListState: LazyListState, value: Int): String {
    val indexValue = (lazyListState.firstVisibleItemIndex + 1) % value
    return indexValue.toString().padStart(2, '0')
}

@Composable
fun HourWheel(
    hourState: LazyListState,
) {
    val hours = addPadding(MAX_HOURS)
    TimerLazyColumn(
        lazyListState = hourState,
        list = hours,
    )
}

@Composable
fun MinuteWheel(
    minuteState: LazyListState,
) {
    val minutes = addPadding(MAX_MINUTES)
    TimerLazyColumn(
        lazyListState = minuteState,
        list = minutes,
    )
}

@Composable
fun SecondWheel(
    secondState: LazyListState,
) {
    val seconds = addPadding(MAX_SECONDS)
    TimerLazyColumn(
        lazyListState = secondState,
        list = seconds,
    )
}

private fun addPadding(
    range: Int,
) = (0..10000).map { it % range }.map { it.toString().padStart(2, '0') }

@Composable
fun TimerText(
    text: String = "00",
    isCurrentItem: Boolean,
) {

    val animatedColor by animateColorAsState(
        if (isCurrentItem) MaterialTheme.colors.onBackground else MaterialTheme.colors.onBackground.copy(alpha = .2f)
    )
    val animatedFontSize by animateIntAsState(if (isCurrentItem) 40 else 35)

    Text(
        modifier = Modifier
            .size(50.dp),
        text = text,
        color = animatedColor,
        fontSize = animatedFontSize.absoluteValue.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun TimerLazyColumn(
    lazyListState: LazyListState,
    list: List<String>
) {
    LazyColumn(
        state = lazyListState,
    ) {
        itemsIndexed(list) { index, item ->
            TimerText(
                text = item,
                isCurrentItem = lazyListState.firstVisibleItemIndex == index - 1
            )
        }
    }
}
