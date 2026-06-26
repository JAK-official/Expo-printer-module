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
            productName: String, // e.g., "Jumbo Diet"
            productLine2: String, // e.g., "Vanilla Chocolate"
            productLine3: String, // (unused in your design – you can omit or merge)
            productionDate: String, // e.g., "02/03/2026"
            lineTitle: String, // e.g., "Line"
            lineValue: String, // e.g., "Line 1"
            casesOnPallet: String, // e.g., "120"
            palletNo: String, // e.g., "2"
            barcodeValue: String // e.g., "P000000001"
    ) {
        val p = printer ?: throw Exception("Printer not connected")

        // Label dimensions (in mm) – adjust to your actual label size
        val labelWidthMm = 100.0
        val labelHeightMm = 180.0

        // Set up media, gap, and clear buffer
        p.sizeMm(labelWidthMm, labelHeightMm)
                .gapMm(3.0, 0.0) // <-- ESSENTIAL for gap labels
                .reference(0, 0) // origin at top‑left
                .direction(TSPLConst.DIRECTION_FORWARD)
                .density(10) // print density (0‑15)
                .cls()

        // Convert mm to dots at 203 DPI
        val dpi = 203
        val widthDots = (labelWidthMm / 25.4 * dpi).toInt()
        val heightDots = (labelHeightMm / 25.4 * dpi).toInt()
        val margin = 20 // inner margin in dots

        // ---- Row Y positions (in dots) ----
        val yProductLabel = 20
        val yProductValue = 50
        val yProductLine2 = 90 // second line of product name
        val yProdDateLabel = 20
        val yProdDateValue = 50
        val yLineTitle = 130
        val yLineValue = 160
        val yCasesLabel = 200
        val yCasesValue = 230
        val yPalletLabel = 200
        val yPalletValue = 230
        val yBarcodeLabel = 280
        val yBarcode = 300

        // ---- Columns ----
        val col1 = margin + 10 // left column start
        val col2 = widthDots / 2 + 10 // right column start

        // ---- Outer border ----
        p.box(margin, margin, widthDots - 2 * margin, heightDots - 2 * margin, 3)

        // ---- Horizontal dividers ----
        p.bar(margin, yLineTitle - 20, widthDots - 2 * margin, 2)
        p.bar(margin, yCasesLabel - 20, widthDots - 2 * margin, 2)
        p.bar(margin, yBarcodeLabel - 20, widthDots - 2 * margin, 2)

        // ---- Vertical divider (between left and right columns) ----
        val midX = widthDots / 2
        p.bar(midX, margin, 2, yLineTitle - 20 - margin) // top section
        p.bar(
                midX,
                yCasesLabel - 20,
                2,
                (yBarcodeLabel - 20) - (yCasesLabel - 20)
        ) // bottom section

        // ====== 1. LEFT COLUMN: Product ======
        p.text(col1, yProductLabel, TSPLConst.FNT_8_12, 2, 2, "Product")
        p.text(col1, yProductValue, TSPLConst.FNT_8_12, 2, 2, productName)
        p.text(col1, yProductLine2, TSPLConst.FNT_8_12, 2, 2, productLine2) // second line

        // ====== 2. RIGHT COLUMN: Production Date ======
        p.text(col2, yProdDateLabel, TSPLConst.FNT_8_12, 2, 2, "Production Date")
        p.text(col2, yProdDateValue, TSPLConst.FNT_8_12, 2, 2, productionDate)

        // ====== 3. FULL WIDTH: Line ======
        p.text(col1, yLineTitle, TSPLConst.FNT_8_12, 2, 2, lineTitle)
        p.text(col1, yLineValue, TSPLConst.FNT_8_12, 2, 2, lineValue)

        // ====== 4. TWO COLUMNS: Cases & Pallet ======
        p.text(col1, yCasesLabel, TSPLConst.FNT_8_12, 2, 2, "Cases on pallet")
        p.text(col1, yCasesValue, TSPLConst.FNT_8_12, 2, 2, casesOnPallet)

        p.text(col2, yPalletLabel, TSPLConst.FNT_8_12, 2, 2, "Pallet No.")
        p.text(col2, yPalletValue, TSPLConst.FNT_8_12, 2, 2, palletNo)

        // ====== 5. BARCODE ======
        p.text(col1, yBarcodeLabel, TSPLConst.FNT_8_12, 2, 2, "Pallet (CODE 128)")

        // Center the barcode horizontally
        val narrow = 3
        val wide = 3 // for Code 128, wide = narrow (it's a 1D barcode with fixed ratio)
        val barcodeHeight = 120 // height in dots
        val barcodeWidth = (11 * barcodeValue.length + 35) * narrow // approximate width
        val barcodeStartX = (widthDots - barcodeWidth) / 2

        p.barcode(
                barcodeStartX,
                yBarcode,
                TSPLConst.CODE_TYPE_128,
                barcodeHeight,
                TSPLConst.READABLE_CENTER, // show human‑readable text below
                TSPLConst.ROTATION_0,
                narrow,
                wide,
                barcodeValue
        )

        // ---- Print one label ----
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
