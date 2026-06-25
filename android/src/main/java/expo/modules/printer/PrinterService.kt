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

    fun printXpTt426bLabel(
        productName: String,
        productLine2: String,
        productLine3: String,
        productionDate: String,
        lineTitle: String,
        lineValue: String,
        casesOnPallet: String,
        palletNo: String,
        barcodeValue: String
        ) {

        val p = printer
            ?: throw Exception("Printer not connected")

        p.sizeMm(76.0, 100.0)
            .gapMm(0.0, 0.0)
            .reference(0, 0)
            .direction(TSPLConst.DIRECTION_FORWARD)
            .density(10)
            .cls()

            .text(10, 10, TSPLConst.FNT_8_12, 1, 1, "Product:")
            .text(10, 30, TSPLConst.FNT_8_12, 1, 1, productName)
            .text(10, 50, TSPLConst.FNT_8_12, 1, 1, productLine2)
            .text(10, 70, TSPLConst.FNT_8_12, 1, 1, productLine3)

            .text(260, 10, TSPLConst.FNT_8_12, 1, 1, "Production Date:")
            .text(260, 30, TSPLConst.FNT_8_12, 1, 1, productionDate)

            .text(10, 110, TSPLConst.FNT_8_12, 1, 1, lineTitle)
            .text(10, 130, TSPLConst.FNT_8_12, 1, 1, lineValue)

            .text(10, 170, TSPLConst.FNT_8_12, 1, 1, "Cases on pallet:")
            .text(10, 190, TSPLConst.FNT_8_12, 1, 1, casesOnPallet)

            .text(260, 170, TSPLConst.FNT_8_12, 1, 1, "Pallet no:")
            .text(260, 190, TSPLConst.FNT_8_12, 1, 1, palletNo)

            .barcode(
            60,
            230,
            TSPLConst.CODE_TYPE_128,
            80,
            TSPLConst.READABLE_CENTER,
            TSPLConst.ROTATION_0,
            2,
            2,
            barcodeValue
            )

            .text(
            10,
            330,
            TSPLConst.FNT_8_12,
            1,
            1,
            barcodeValue
            )

            .print(1)
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