package com.codebuzz.app.bookcric.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codebuzz.app.bookcric.game.MatchConfig
import com.codebuzz.app.bookcric.game.Player
import com.codebuzz.app.bookcric.online.ConnectionStatus
import com.codebuzz.app.bookcric.online.NearbyEndpoint
import com.codebuzz.app.bookcric.ui.OnlineLobby
import com.codebuzz.app.bookcric.ui.theme.BookCricketTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/** Runtime permissions Nearby Connections needs to find and talk to the other phone. */
private val NEARBY_PERMISSIONS = listOf(
    Manifest.permission.BLUETOOTH_ADVERTISE,
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.NEARBY_WIFI_DEVICES
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnlineLobbyScreen(
    lobby: OnlineLobby,
    onHost: () -> Unit,
    onJoin: () -> Unit,
    onConnect: (NearbyEndpoint) -> Unit,
    onCancel: () -> Unit,
    onStartMatch: (MatchConfig) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onLeave)

    var permissionsRequested by remember { mutableStateOf(false) }
    val permissions = rememberMultiplePermissionsState(NEARBY_PERMISSIONS) { permissionsRequested = true }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Play Online",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Playing as ${lobby.playerName}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (!permissions.allPermissionsGranted) {
                PermissionPrompt(
                    permanentlyDenied = permissionsRequested && !permissions.shouldShowRationale,
                    onRequest = { permissions.launchMultiplePermissionRequest() }
                )
            } else {
                when (val status = lobby.status) {
                    ConnectionStatus.Idle -> HostOrJoin(notice = null, onHost = onHost, onJoin = onJoin)
                    is ConnectionStatus.Failed -> HostOrJoin(notice = status.reason, onHost = onHost, onJoin = onJoin)
                    is ConnectionStatus.Disconnected ->
                        HostOrJoin(notice = "${status.peerName} left the game.", onHost = onHost, onJoin = onJoin)
                    ConnectionStatus.Advertising -> Waiting(
                        message = "Waiting for a friend to join…\nAsk them to open Play Online and tap Join.",
                        onCancel = onCancel
                    )
                    is ConnectionStatus.Discovering -> HostList(
                        hosts = status.hosts,
                        onConnect = onConnect,
                        onCancel = onCancel
                    )
                    is ConnectionStatus.Connecting -> Waiting(
                        message = "Connecting to ${status.peerName}…",
                        onCancel = onCancel
                    )
                    is ConnectionStatus.Connected -> if (status.isHost) {
                        HostMatchSetup(
                            hostName = lobby.playerName,
                            guestName = status.peerName,
                            onStartMatch = onStartMatch
                        )
                    } else {
                        Waiting(
                            message = "Connected to ${status.peerName}.\nWaiting for them to start the match…",
                            onCancel = null
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onLeave,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Leave")
        }
    }
}

@Composable
private fun PermissionPrompt(permanentlyDenied: Boolean, onRequest: () -> Unit) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "To find your friend's phone, BookCric needs the Nearby devices permission.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (permanentlyDenied) {
            Button(onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            }) {
                Text("Open Settings")
            }
        } else {
            Button(onClick = onRequest) {
                Text("Allow")
            }
        }
    }
}

@Composable
private fun HostOrJoin(notice: String?, onHost: () -> Unit, onJoin: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (notice != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = notice,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        Text(
            text = "One phone hosts the match, the other joins it.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onHost,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Host a Match")
        }
        FilledTonalButton(
            onClick = onJoin,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Join a Match")
        }
    }
}

@Composable
private fun Waiting(message: String, onCancel: (() -> Unit)?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (onCancel != null) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun HostList(
    hosts: List<NearbyEndpoint>,
    onConnect: (NearbyEndpoint) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(
                text = "Looking for matches nearby…",
                style = MaterialTheme.typography.titleMedium
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(hosts, key = { it.id }) { host ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConnect(host) }
                ) {
                    ListItem(
                        headlineContent = { Text(host.name, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Tap to join") },
                        leadingContent = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null) }
                    )
                }
            }
        }
        TextButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}

/** The host picks the settings; the guest's phone gets them when the match starts. */
@Composable
private fun HostMatchSetup(
    hostName: String,
    guestName: String,
    onStartMatch: (MatchConfig) -> Unit
) {
    var battingFirst by remember { mutableStateOf(Player.ONE) }
    var oversLimit by remember { mutableStateOf<Int?>(1) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Connected to $guestName",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        MatchOptions(
            playerOneLabel = hostName,
            playerTwoLabel = guestName,
            battingFirst = battingFirst,
            onBattingFirstChange = { battingFirst = it },
            oversLimit = oversLimit,
            onOversLimitChange = { oversLimit = it }
        )
        Button(
            onClick = {
                onStartMatch(
                    MatchConfig(
                        playerOneName = hostName,
                        playerTwoName = guestName,
                        battingFirst = battingFirst,
                        oversLimit = oversLimit
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Start Match")
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun OnlineLobbyScreenPreview() {
    BookCricketTheme {
        HostList(
            hosts = listOf(NearbyEndpoint("a", "Player 482"), NearbyEndpoint("b", "Riya")),
            onConnect = {},
            onCancel = {}
        )
    }
}
