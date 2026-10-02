package com.codebuzz.app.bookcric.online

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** A phone found while browsing for matches. */
data class NearbyEndpoint(val id: String, val name: String)

/** Where the link to the other phone stands. Drives the online lobby screen. */
sealed interface ConnectionStatus {
    data object Idle : ConnectionStatus
    data object Advertising : ConnectionStatus
    data class Discovering(val hosts: List<NearbyEndpoint>) : ConnectionStatus
    data class Connecting(val peerName: String) : ConnectionStatus
    data class Connected(val peerName: String, val isHost: Boolean) : ConnectionStatus
    data class Disconnected(val peerName: String) : ConnectionStatus
    data class Failed(val reason: String) : ConnectionStatus
}

/**
 * One peer-to-peer link between two phones over Google Nearby Connections (Bluetooth / Wi-Fi,
 * no server). One phone [host]s and advertises; the other [join]s, discovers it and connects.
 *
 * Nearby delivers BYTES payloads reliably and in order, which the match sync relies on.
 * All callbacks arrive on the main thread.
 */
class NearbySession(context: Context) {

    private val client = Nearby.getConnectionsClient(context.applicationContext)

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _messages = Channel<NetMessage>(Channel.UNLIMITED)

    /** Messages from the other phone, in the order they were sent. */
    val messages: Flow<NetMessage> = _messages.receiveAsFlow()

    private var localName = ""
    private var isHost = false
    private var peerId: String? = null
    private val peerNames = mutableMapOf<String, String>()
    private val found = linkedMapOf<String, NearbyEndpoint>()

    /** Advertise this phone as a match others can join. */
    fun host(name: String) {
        stop()
        localName = name
        isHost = true
        _status.value = ConnectionStatus.Advertising
        client.startAdvertising(
            name,
            SERVICE_ID,
            lifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        ).addOnFailureListener(::fail)
    }

    /** Look for hosts nearby; they show up in [ConnectionStatus.Discovering.hosts]. */
    fun join(name: String) {
        stop()
        localName = name
        isHost = false
        _status.value = ConnectionStatus.Discovering(emptyList())
        client.startDiscovery(
            SERVICE_ID,
            discoveryCallback,
            DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        ).addOnFailureListener(::fail)
    }

    /** Ask a discovered host to connect. */
    fun connectTo(endpoint: NearbyEndpoint) {
        _status.value = ConnectionStatus.Connecting(endpoint.name)
        client.requestConnection(localName, endpoint.id, lifecycleCallback)
            .addOnFailureListener(::fail)
    }

    fun send(message: NetMessage) {
        val id = peerId ?: return
        client.sendPayload(id, Payload.fromBytes(NetMessage.encode(message)))
    }

    /** Drop the link and stop advertising/discovery. The other phone sees a disconnect. */
    fun stop() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        peerId = null
        peerNames.clear()
        found.clear()
        _status.value = ConnectionStatus.Idle
    }

    private fun fail(error: Exception) {
        val code = (error as? ApiException)?.statusCode
        val reason = when (code) {
            ConnectionsStatusCodes.STATUS_RADIO_ERROR -> "Turn on Bluetooth and Wi-Fi, then try again."
            ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_SCAN,
            ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_ADVERTISE,
            ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_CONNECT,
            ConnectionsStatusCodes.MISSING_PERMISSION_NEARBY_WIFI_DEVICES -> "Nearby devices permission is needed to play online."
            ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_COARSE_LOCATION,
            ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_FINE_LOCATION -> "Location permission is needed to find nearby phones."
            null -> error.message ?: "Something went wrong."
            else -> "Couldn't connect (${ConnectionsStatusCodes.getStatusCodeString(code)})."
        }
        stop()
        _status.value = ConnectionStatus.Failed(reason)
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (_status.value !is ConnectionStatus.Discovering) return
            found[endpointId] = NearbyEndpoint(endpointId, info.endpointName)
            _status.value = ConnectionStatus.Discovering(found.values.toList())
        }

        override fun onEndpointLost(endpointId: String) {
            found.remove(endpointId)
            if (_status.value is ConnectionStatus.Discovering) {
                _status.value = ConnectionStatus.Discovering(found.values.toList())
            }
        }
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Point-to-point: once we have an opponent, or are mid-handshake with one, nobody
            // else gets in.
            if (peerId != null || peerNames.isNotEmpty()) {
                client.rejectConnection(endpointId)
                return
            }
            peerNames[endpointId] = info.endpointName
            _status.value = ConnectionStatus.Connecting(info.endpointName)
            client.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            // A latecomer we rejected reports here too; it must not disturb the real connection.
            if (endpointId !in peerNames) return
            val name = peerNames[endpointId] ?: "Opponent"
            if (resolution.status.isSuccess) {
                peerId = endpointId
                client.stopAdvertising()
                client.stopDiscovery()
                _status.value = ConnectionStatus.Connected(name, isHost)
            } else if (isHost) {
                // Keep waiting for someone else to join.
                peerNames.remove(endpointId)
                _status.value = ConnectionStatus.Advertising
            } else {
                stop()
                _status.value = ConnectionStatus.Failed("$name didn't accept the connection.")
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (endpointId != peerId) return
            val name = peerNames[endpointId] ?: "Opponent"
            stop()
            _status.value = ConnectionStatus.Disconnected(name)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (endpointId != peerId) return
            val bytes = payload.asBytes() ?: return
            NetMessage.decode(bytes)?.let { _messages.trySend(it) }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    companion object {
        /** Versioned so phones running an incompatible protocol never see each other. */
        private const val SERVICE_ID = "com.codebuzz.app.bookcric.online.v1"
        private val STRATEGY = Strategy.P2P_POINT_TO_POINT
    }
}
