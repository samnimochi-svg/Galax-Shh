package com.example.tasker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.MyApplicationTheme

class TaskerEditActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read existing selection if editing an existing task
        val previousBundle = intent?.getBundleExtra(TaskerConstants.EXTRA_BUNDLE)
        val initialAction = previousBundle?.getString(TaskerConstants.KEY_ACTION)
            ?: TaskerConstants.VAL_TOGGLE

        setContent {
            MyApplicationTheme {
                TaskerConfigScreen(
                    initialAction = initialAction,
                    onSave = { selectedAction, blurb ->
                        val resultBundle = Bundle().apply {
                            putString(TaskerConstants.KEY_ACTION, selectedAction)
                        }
                        val resultIntent = Intent().apply {
                            putExtra(TaskerConstants.EXTRA_BUNDLE, resultBundle)
                            putExtra(TaskerConstants.EXTRA_BLURB, blurb)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskerConfigScreen(
    initialAction: String,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var selectedAction by remember { mutableStateOf(initialAction) }

    val options = listOf(
        Triple(
            TaskerConstants.VAL_TOGGLE,
            stringResource(R.string.tasker_action_toggle_title),
            stringResource(R.string.tasker_action_toggle_desc)
        ),
        Triple(
            TaskerConstants.VAL_MUTE_ON,
            stringResource(R.string.tasker_action_mute_on_title),
            stringResource(R.string.tasker_action_mute_on_desc)
        ),
        Triple(
            TaskerConstants.VAL_MUTE_OFF,
            stringResource(R.string.tasker_action_mute_off_title),
            stringResource(R.string.tasker_action_mute_off_desc)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tasker_config_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.tasker_cancel))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.tasker_select_action_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                options.forEach { (actionKey, title, description) ->
                    val isSelected = selectedAction == actionKey
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { selectedAction = actionKey },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedAction = actionKey }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val blurb = when (selectedAction) {
                        TaskerConstants.VAL_MUTE_ON -> context.getString(R.string.tasker_action_mute_on_title)
                        TaskerConstants.VAL_MUTE_OFF -> context.getString(R.string.tasker_action_mute_off_title)
                        else -> context.getString(R.string.tasker_action_toggle_title)
                    }
                    onSave(selectedAction, blurb)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.tasker_btn_save))
            }
        }
    }
}
