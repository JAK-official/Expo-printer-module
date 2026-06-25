package expo.modules.printer

import net.posprinter.*
import android.os.Build
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

import android.util.Log

class PrinterService(
  private val context: android.content.Context
) {

  private var connection: IDeviceConnection? = null
  private var printer: TSPLPrinter? = null
  private var connected = false


  private fun hasBluetoothPermissions(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      // Before Android 12, only BLUETOOTH permission is needed
      return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.BLUETOOTH
      ) == PackageManager.PERMISSION_GRANTED
    }
    
    // Android 12+: Need both BLUETOOTH_SCAN and BLUETOOTH_CONNECT
    return (ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.BLUETOOTH_SCAN
    ) == PackageManager.PERMISSION_GRANTED &&
    ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED)
  }


  suspend fun connect(mac: String): String {

    disconnect()

    POSConnect.init(context)

    val device =
      POSConnect.createDevice(
        POSConnect.DEVICE_TYPE_BLUETOOTH
      )

    return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->

      device.connect(
        mac,
        object : IConnectListener {

          override fun onStatus(
            code: Int,
            connectInfo: String?,
            message: String?
          ) {

            if (code == POSConnect.CONNECT_SUCCESS) {

              connection = device
              printer = TSPLPrinter(device)
              connected = true

              continuation.resume(
                "Connected to $mac",
                null
              )

            } else if (continuation.isActive) {

              continuation.resumeWith(
                Result.failure(
                  Exception(message ?: "Connection failed")
                )
              )
            }
          }
        }
      )
    }
  }


  fun isConnected(): Boolean {
    return connected
  }


  fun printTest() {

    val p = printer
        ?: throw Exception("Printer not connected")

    p.sizeMm(60.0, 30.0)
        .density(10)
        .reference(0, 0)
        .direction(TSPLConst.DIRECTION_FORWARD)
        .cls()
        .text(
        10,
        10,
        TSPLConst.FNT_8_12,
        2,
        2,
        "HELLO FROM EXPO"
        )
        .print()
    }


  fun disconnect() {
        try {
            connection?.close()
        } catch (_: Exception) {
        }

        connection = null
        printer = null
        connected = false
    }
}