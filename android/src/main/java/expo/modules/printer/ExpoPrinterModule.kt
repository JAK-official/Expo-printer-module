package expo.modules.printer

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import android.os.Build
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class ExpoPrinterModule : Module() {

  private lateinit var printerService: PrinterService

  override fun definition() = ModuleDefinition {

    Name("ExpoPrinter")

    OnCreate {
      printerService = PrinterService(appContext.reactContext!!)
    }

    Function("getVersion") {
      "1.0.0"
    }

    Function("hasBluetoothPermissions") {
      hasBluetoothPermissions()
    }

    Function("getRequiredPermissions") {
      getRequiredBluetoothPermissions()
    }

    AsyncFunction("connect") { address: String, promise ->
      GlobalScope.launch {
        try {
          val result = printerService.connect(address)
          promise.resolve(result)
        } catch (e: Exception) {
          promise.reject("CONNECT_ERROR", e.message ?: "Unknown error", e)
        }
      }
    }

    Function("isConnected") {
      printerService.isConnected()
    }

    Function("printTest") {
      printerService.printTest()
    }

    Function("disconnect") {
      printerService.disconnect()
    }
  }

  private fun hasBluetoothPermissions(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      return ContextCompat.checkSelfPermission(
        appContext.reactContext!!,
        android.Manifest.permission.BLUETOOTH
      ) == PackageManager.PERMISSION_GRANTED
    }

    return (ContextCompat.checkSelfPermission(
      appContext.reactContext!!,
      android.Manifest.permission.BLUETOOTH_SCAN
    ) == PackageManager.PERMISSION_GRANTED &&
    ContextCompat.checkSelfPermission(
      appContext.reactContext!!,
      android.Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED)
  }

  private fun getRequiredBluetoothPermissions(): List<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      listOf(
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_CONNECT
      )
    } else {
      listOf(android.Manifest.permission.BLUETOOTH)
    }
  }
}