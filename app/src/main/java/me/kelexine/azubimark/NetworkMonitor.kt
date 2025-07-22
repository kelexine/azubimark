package me.kelexine.azubimark

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Monitors network connectivity status and provides real-time updates
 * to the application for better user experience during offline scenarios
 */
class NetworkMonitor private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: NetworkMonitor? = null
        
        fun getInstance(context: Context): NetworkMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Network connection states
     */
    enum class NetworkState {
        CONNECTED_WIFI,
        CONNECTED_MOBILE,
        CONNECTED_OTHER,
        DISCONNECTED,
        UNKNOWN
    }
    
    /**
     * Network connection quality
     */
    enum class ConnectionQuality {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        NO_CONNECTION
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState> = _networkState
    
    private val _connectionQuality = MutableLiveData<ConnectionQuality>()
    val connectionQuality: LiveData<ConnectionQuality> = _connectionQuality
    
    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    /**
     * Start monitoring network connectivity
     */
    fun startMonitoring() {
        // Initialize with current state
        updateNetworkState()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    CoroutineScope(Dispatchers.Main).launch {
                        updateNetworkState()
                    }
                }
                
                override fun onLost(network: Network) {
                    super.onLost(network)
                    CoroutineScope(Dispatchers.Main).launch {
                        updateNetworkState()
                    }
                }
                
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    CoroutineScope(Dispatchers.Main).launch {
                        updateNetworkState()
                        updateConnectionQuality(networkCapabilities)
                    }
                }
            }
            
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()
                
            networkCallback?.let { callback ->
                connectivityManager.registerNetworkCallback(request, callback)
            }
        } else {
            // For older Android versions, we'll need to use different approach
            // This is simplified for the scope of this implementation
            updateNetworkState()
        }
    }
    
    /**
     * Stop monitoring network connectivity
     */
    fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback?.let { callback ->
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                } catch (e: Exception) {
                    // Ignore if already unregistered
                }
            }
        }
        networkCallback = null
    }
    
    /**
     * Check if device is currently connected to internet
     */
    fun isCurrentlyConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            activeNetworkInfo?.isConnected == true
        }
    }
    
    /**
     * Get current network type
     */
    fun getCurrentNetworkType(): NetworkState {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            return when {
                capabilities == null -> NetworkState.DISCONNECTED
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState.CONNECTED_WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkState.CONNECTED_MOBILE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkState.CONNECTED_OTHER
                else -> NetworkState.UNKNOWN
            }
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return when {
                activeNetworkInfo?.isConnected != true -> NetworkState.DISCONNECTED
                activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI -> NetworkState.CONNECTED_WIFI
                activeNetworkInfo.type == ConnectivityManager.TYPE_MOBILE -> NetworkState.CONNECTED_MOBILE
                else -> NetworkState.CONNECTED_OTHER
            }
        }
    }
    
    /**
     * Check if connected to WiFi
     */
    fun isConnectedToWiFi(): Boolean {
        return getCurrentNetworkType() == NetworkState.CONNECTED_WIFI
    }
    
    /**
     * Check if connected to mobile data
     */
    fun isConnectedToMobile(): Boolean {
        return getCurrentNetworkType() == NetworkState.CONNECTED_MOBILE
    }
    
    /**
     * Check if connection is metered (mobile data or metered WiFi)
     */
    fun isConnectionMetered(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.isActiveNetworkMetered
        } else {
            // Assume mobile connections are metered for older versions
            isConnectedToMobile()
        }
    }
    
    /**
     * Update network state and notify observers
     */
    private fun updateNetworkState() {
        val currentState = getCurrentNetworkType()
        val connected = isCurrentlyConnected()
        
        _networkState.value = currentState
        _isConnected.value = connected
    }
    
    /**
     * Update connection quality based on network capabilities
     */
    private fun updateConnectionQuality(capabilities: NetworkCapabilities) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val quality = when {
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> ConnectionQuality.NO_CONNECTION
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    // For WiFi, we can assume good quality unless we detect otherwise
                    ConnectionQuality.GOOD
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    // For cellular, quality depends on signal strength and technology
                    evaluateCellularQuality(capabilities)
                }
                else -> ConnectionQuality.FAIR
            }
            
            _connectionQuality.value = quality
        } else {
            // For older versions, make a basic assumption
            val quality = when (getCurrentNetworkType()) {
                NetworkState.CONNECTED_WIFI -> ConnectionQuality.GOOD
                NetworkState.CONNECTED_MOBILE -> ConnectionQuality.FAIR
                NetworkState.CONNECTED_OTHER -> ConnectionQuality.FAIR
                NetworkState.DISCONNECTED -> ConnectionQuality.NO_CONNECTION
                NetworkState.UNKNOWN -> ConnectionQuality.POOR
            }
            
            _connectionQuality.value = quality
        }
    }
    
    /**
     * Evaluate cellular connection quality
     */
    private fun evaluateCellularQuality(capabilities: NetworkCapabilities): ConnectionQuality {
        // This is a simplified evaluation
        // In a real-world scenario, you might want to check signal strength,
        // network type (2G, 3G, 4G, 5G), and other factors
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Check if it's a high-speed connection
            when {
                capabilities.linkDownstreamBandwidthKbps > 10000 -> ConnectionQuality.EXCELLENT
                capabilities.linkDownstreamBandwidthKbps > 5000 -> ConnectionQuality.GOOD
                capabilities.linkDownstreamBandwidthKbps > 1000 -> ConnectionQuality.FAIR
                else -> ConnectionQuality.POOR
            }
        } else {
            // Default assumption for older versions
            ConnectionQuality.FAIR
        }
    }
    
    /**
     * Get user-friendly network status description
     */
    fun getNetworkStatusDescription(): String {
        val state = getCurrentNetworkType()
        val connected = isCurrentlyConnected()
        
        return when {
            !connected -> "No internet connection"
            state == NetworkState.CONNECTED_WIFI -> "Connected via WiFi"
            state == NetworkState.CONNECTED_MOBILE -> {
                if (isConnectionMetered()) {
                    "Connected via mobile data (metered)"
                } else {
                    "Connected via mobile data"
                }
            }
            state == NetworkState.CONNECTED_OTHER -> "Connected via other network"
            else -> "Connection status unknown"
        }
    }
    
    /**
     * Get recommended behavior based on connection state
     */
    fun getRecommendedBehavior(): NetworkBehaviorRecommendation {
        val connected = isCurrentlyConnected()
        val metered = isConnectionMetered()
        val quality = _connectionQuality.value ?: ConnectionQuality.UNKNOWN
        
        return when {
            !connected -> NetworkBehaviorRecommendation.OFFLINE_MODE
            metered && quality == ConnectionQuality.POOR -> NetworkBehaviorRecommendation.CONSERVATIVE_DATA_USAGE
            metered -> NetworkBehaviorRecommendation.MODERATE_DATA_USAGE
            quality == ConnectionQuality.EXCELLENT -> NetworkBehaviorRecommendation.FULL_FEATURES
            else -> NetworkBehaviorRecommendation.NORMAL_USAGE
        }
    }
    
    /**
     * Network behavior recommendations
     */
    enum class NetworkBehaviorRecommendation {
        FULL_FEATURES,          // High-speed, unlimited connection
        NORMAL_USAGE,           // Good connection, normal features
        MODERATE_DATA_USAGE,    // Metered connection, be conscious of data
        CONSERVATIVE_DATA_USAGE, // Poor/metered connection, minimize data usage
        OFFLINE_MODE            // No connection, offline features only
    }
}