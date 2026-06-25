package expo.modules.printer

import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import net.posprinter.*

class PrinterService(private val context: android.content.Context) {

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

        val device = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)

        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            device.connect(
                    mac,
                    object : IConnectListener {

                        override fun onStatus(code: Int, connectInfo: String?, message: String?) {

                            if (code == POSConnect.CONNECT_SUCCESS) {

                                connection = device
                                printer = TSPLPrinter(device)
                                connected = true

                                continuation.resume("Connected to $mac", null)
                            } else if (continuation.isActive) {

                                continuation.resumeWith(
                                        Result.failure(Exception(message ?: "Connection failed"))
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

        val p = printer ?: throw Exception("Printer not connected")

        p.sizeMm(60.0, 30.0)
                .density(10)
                .reference(0, 0)
                .direction(TSPLConst.DIRECTION_FORWARD)
                .cls()
                .text(10, 10, TSPLConst.FNT_8_12, 2, 2, "HELLO FROM EXPO")
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
        val p = printer ?: throw Exception("Printer not connected")

        // --- CORRECTED: 203 DPI, 100mm width (matching your "800 dots" paper) ---
        val DPI = 203
        val labelWidthMm = 100.0 // Matches your ~800 dots paper
        val labelHeightMm = 180.0

        p.sizeMm(labelWidthMm, labelHeightMm)
                .gapMm(0.0, 0.0) // Set to 2.0 or 3.0 if using gap labels
                .reference(
                        0,
                        0
                ) // Prints exactly at x=0. If you still see left empty space, change to
                // reference(20, 0) to nudge right.
                .direction(TSPLConst.DIRECTION_FORWARD)
                .density(10)
                .cls()

        // Calculate exact dots for 100mm at 203 DPI
        val labelWidthDots = (labelWidthMm / 25.4 * DPI).toInt() // ~800 dots!
        val margin = 20
        val col1_x = margin + 20
        val col2_x = labelWidthDots / 2 + margin // Middle split (approx 410)

        // Row Y coordinates (in dots, adjusted for 203 DPI)
        val row1_y = 20
        val row2_y = 160
        val row3_y = 200
        val row4_y = 280
        val row5_y = 380 // Barcode row

        // --- OUTER BORDER (Now properly fills the 100mm width) ---
        p.box(
                margin,
                margin,
                labelWidthDots - (margin * 2), // ~780 dots
                780,
                3
        )

        // --- HORIZONTAL DIVIDERS ---
        p.bar(margin, row2_y - 10, labelWidthDots - (margin * 2), 2)
        p.bar(margin, row3_y + 40, labelWidthDots - (margin * 2), 2)
        p.bar(margin, row5_y - 10, labelWidthDots - (margin * 2), 2)

        // --- VERTICAL DIVIDER (Middle line) ---
        p.bar(col2_x, margin, 2, row2_y - 20) // Split top section
        p.bar(col2_x, row3_y + 50, 2, 80) // Split bottom section

        // --- LEFT COLUMN ---
        p.text(col1_x, row1_y, TSPLConst.FNT_8_12, 2, 2, "Product:")
        p.text(col1_x, row1_y + 40, TSPLConst.FNT_8_12, 2, 2, productName)
        p.text(col1_x, row1_y + 90, TSPLConst.FNT_8_12, 2, 2, productLine2)
        p.text(col1_x, row1_y + 140, TSPLConst.FNT_8_12, 2, 2, productLine3)

        // --- RIGHT COLUMN ---
        p.text(col2_x + 20, row1_y, TSPLConst.FNT_8_12, 2, 2, "Prod Date:")
        p.text(col2_x + 20, row1_y + 40, TSPLConst.FNT_8_12, 2, 2, productionDate)

        // --- FULL WIDTH ROW ---
        p.text(col1_x, row2_y + 10, TSPLConst.FNT_8_12, 2, 2, lineTitle)
        p.text(col1_x, row2_y + 50, TSPLConst.FNT_8_12, 2, 2, lineValue)

        // --- TWO COLUMN ROW ---
        p.text(col1_x, row3_y + 60, TSPLConst.FNT_8_12, 2, 2, "Cases:")
        p.text(col1_x, row3_y + 100, TSPLConst.FNT_8_12, 2, 2, casesOnPallet)
        p.text(col2_x + 10, row3_y + 60, TSPLConst.FNT_8_12, 2, 2, "Pallet #:")
        p.text(col2_x + 10, row3_y + 100, TSPLConst.FNT_8_12, 2, 2, palletNo)

        p.text(col1_x, row5_y - 20, TSPLConst.FNT_8_12, 2, 2, "Pallet (CODE 128)")

        // --- BARCODE (Perfectly centered using exact width calculation for Code 128) ---
        // Code 128 width formula: (11 * characters + 35) * narrow_width
        val narrow = 3
        val barcodeWidth = (11 * barcodeValue.length + 35) * narrow
        val barcodeStartX = (labelWidthDots - barcodeWidth) / 2

        p.barcode(
                barcodeStartX,
                row5_y + 10,
                TSPLConst.CODE_TYPE_128,
                120,
                TSPLConst.READABLE_CENTER,
                TSPLConst.ROTATION_0,
                narrow, // narrow
                3, // wide
                barcodeValue
        )

        p.print(1)
    }

    fun disconnect() {
        try {
            connection?.close()
        } catch (_: Exception) {}

        connection = null
        printer = null
        connected = false
    }
}
