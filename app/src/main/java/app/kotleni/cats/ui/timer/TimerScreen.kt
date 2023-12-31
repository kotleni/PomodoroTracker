package app.kotleni.cats.ui.timer

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import app.kotleni.cats.TimerState
import app.kotleni.cats.toTimeString
import java.util.Timer
import kotlin.concurrent.schedule

enum class TimerStage {
    WORK, BREAK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(rootNavController: NavHostController, timerId: Int) {
    val viewModel: TimerViewModel = hiltViewModel()

    val timer by viewModel.timer.collectAsState()

    val currentSeconds by viewModel.currentSeconds.collectAsState()
    val timerStage by viewModel.timerStage.collectAsState()
    val timerState by viewModel.timerState.collectAsState()

    var isShowWarningDialog by remember { mutableStateOf(false) }

    val currentStartTime = (if(timerStage == TimerStage.WORK) timer?.workTime else timer?.shortBreakTime) ?: 1
    val animatedPercentage by animateFloatAsState(
        targetValue = (currentSeconds.toFloat() / currentStartTime.toFloat()) * 100f,
        animationSpec = tween(),
        label = "animatedPercentage"
    )
    val timeName = if(timerStage == TimerStage.WORK) "Work time" else "Break time"

    if(isShowWarningDialog) {
        TimerExitWarningDialog(
            onPositive = {
                viewModel.resetServiceIsNotStarted()
                rootNavController.popBackStack()
            },
            onNegative = { isShowWarningDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = timer?.name ?: "Timer")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if(timerState == TimerState.PAUSED) {
                            isShowWarningDialog = true
                        } else {
                            viewModel.resetServiceIsNotStarted()
                            rootNavController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = ""
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val lineColorBg = MaterialTheme.colorScheme.onSecondary
            val lineColorFg = MaterialTheme.colorScheme.secondary

            Box(modifier = Modifier, contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    val canvasSize = size.minDimension
                    val radius = canvasSize / 2.5f
                    val centerX = size.width / 2
                    val centerY = size.height / 2

                    // Calculate the start and sweep angles
                    val startAngle = 0f
                    val sweepAngle = (360f / 100f) * animatedPercentage //180f * (percentage / 100f)

                    drawArc(
                        color = lineColorBg,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(20f)  // Adjust the stroke width as needed
                    )

                    drawArc(
                        color = lineColorFg,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(20f)  // Adjust the stroke width as needed
                    )
                }
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentSeconds.toTimeString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = TextUnit(36f, TextUnitType.Sp)
                    )
                    Text(
                        text = timeName,
                        fontSize = TextUnit(24f, TextUnitType.Sp)
                    )
                }
            }

            Row {
                when(timerState) {
                    TimerState.STOPPED -> {
                        Button(modifier = Modifier.padding(8.dp), onClick = {
                            viewModel.start()
                        }) {
                            Text(text = "Start")
                        }
                    }
                    TimerState.PAUSED -> {
                        Button(modifier = Modifier.padding(8.dp), onClick = {
                            viewModel.resume()
                        }) {
                            Text(text = "Resume")
                        }
                    }
                    TimerState.STARTED -> {
                        Button(modifier = Modifier.padding(8.dp), onClick = {
                            viewModel.pause()
                        }) {
                            Text(text = "Pause")
                        }
                    }
                }

                Button(modifier = Modifier.padding(8.dp), onClick = {
                    viewModel.nextTimerStage()
                }) {
                    Text(text = "Skip")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if(timer != null) {
                Column {
                    SuggestionChip(onClick = { }, label = {
                        Text(text = "Total work time: ${timer!!.totalWorkTime / 60} min")
                    })
                    SuggestionChip(onClick = { }, label = {
                        Text(text = "Total break time: ${timer!!.totalBreakTime / 60} min")
                    })
                }
            }
        }
    }

    LaunchedEffect(key1 = timerId) {
        //viewModel.bindToService()
        viewModel.loadTimer(timerId.toLong())
    }
}