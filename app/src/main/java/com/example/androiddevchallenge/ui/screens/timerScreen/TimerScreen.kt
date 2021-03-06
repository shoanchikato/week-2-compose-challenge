package com.example.androiddevchallenge.ui.screens.timerScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlin.math.absoluteValue
import kotlin.math.ceil

const val MAX_HOURS = 100
const val MAX_MINUTES = 60
const val MAX_SECONDS = 60

const val INIT_LAZY_LIST_HOUR_INDEX = 5499
const val INIT_LAZY_LIST_MINUTE_INDEX = 5459
const val INIT_LAZY_LIST_SECOND_INDEX = 5459

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
        ((hours.toLong() * 60 * 60 * 1000)
                + (minutes.toLong() * 60 * 1000)
                + (seconds.toLong() * 1000))

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
                    if (rawSeconds == 0) setHaStarted(false)
                    delay(1000)
                }
            }
        }
    }

    var showCounterWheels = !hasStarted && hasReset

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
    ){
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

private fun resetClock(lazyState: LazyListState, initValue: Int) {
    val scope = CoroutineScope(Dispatchers.Main)
    scope.launch {
        lazyState.animateScrollToItem(initValue)
    }
}

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
        if (isCurrentItem) Color.Black else Color.Black.copy(alpha = .2f)
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