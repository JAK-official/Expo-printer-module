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
        val labelHeightMm = 150.0

        // Set up media, gap, and clear buffer
        p.sizeMm(labelWidthMm, labelHeightMm)
                .gapMm(4.0, 0.0) // <-- ESSENTIAL for gap labels
                .reference(0, 0) // origin at top‑left
                .direction(TSPLConst.DIRECTION_FORWARD)
                .density(5) // print density (0‑15)
                .cls()

        // Added in order to fix the problem of getting a wrong print if the first printed paper is
        // directly teared off
        connection?.sendData("SET TEAR ON\r\n".toByteArray(Charsets.US_ASCII))

        // Convert mm to dots at 203 DPI
        val dpi = 203
        val widthDots = (labelWidthMm / 25.4 * dpi).toInt()
        val heightDots = (labelHeightMm / 25.4 * dpi).toInt()
        val margin = 20 // inner margin in dots

        // Printable area
        val top = margin
        val bottom = heightDots - margin
        val printableHeight = bottom - top

        // Bottom half is reserved for the barcode row (row 4)
        val row4Height = printableHeight / 2

        // Top half is split equally between rows 1, 2 and 3
        val topHalfHeight = printableHeight - row4Height
        val rowHeight = topHalfHeight / 3

        // Row boundaries
        val row1Top = top
        val row1Bottom = row1Top + rowHeight

        val row2Top = row1Bottom
        val row2Bottom = row2Top + rowHeight

        val row3Top = row2Bottom
        val row3Bottom = row3Top + rowHeight

        val row4Top = row3Bottom
        val row4Bottom = bottom

        // Text positions
        val yProductLabel = row1Top + 15
        val yProductValue = row1Top + 45
        val yProductLine2 = row1Top + 80

        val yProdDateLabel = row1Top + 15
        val yProdDateValue = row1Top + 45

        val yLineTitle = row2Top + 15
        val yLineValue = row2Top + 45

        val yCasesLabel = row3Top + 15
        val yCasesValue = row3Top + 45
        val yPalletLabel = row3Top + 15
        val yPalletValue = row3Top + 45

        // Center of row 4
        val row4CenterY = (row4Top + row4Bottom) / 2

        val yBarcodeLabel = row4Top + 20
        val barcodeHeight = 150
        // Vertically center only the barcode within row 4
        val yBarcode = row4Top + (row4Height - barcodeHeight) / 2

        // ---- Columns ----
        val col1 = margin + 10
        val col2 = widthDots / 2 + 10
        val midX = widthDots / 2

        // ---- Outer border ----
        p.box(margin, margin, widthDots - 2 * margin, heightDots - 2 * margin, 3)

        // Horizontal dividers
        p.bar(margin, row1Bottom, widthDots - 2 * margin, 2)
        p.bar(margin, row2Bottom, widthDots - 2 * margin, 2)
        p.bar(margin, row3Bottom, widthDots - 2 * margin, 2)

        // Top section (Product / Production Date)
        p.bar(midX, row1Top, 2, row1Bottom - row1Top)

        // Bottom section (Cases / Pallet)
        p.bar(midX, row3Top, 2, row3Bottom - row3Top)

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
        val boxCenterX = widthDots / 2

        // Approximate Code 128 width
        val barcodeWidth = (11 * barcodeValue.length + 35) * narrow

        // Center inside the printable area (inside the border)
        val barcodeStartX = margin + ((widthDots - 2 * margin) - barcodeWidth) / 2 + 40

        p.barcode(
                barcodeStartX,
                yBarcode,
                TSPLConst.CODE_TYPE_128,
                barcodeHeight,
                TSPLConst.READABLE_CENTER,
                TSPLConst.ROTATION_0,
                narrow,
                wide,
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
