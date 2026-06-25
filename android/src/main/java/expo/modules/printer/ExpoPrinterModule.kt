package expo.modules.printer

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoPrinterModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("ExpoPrinter")

    Function("getVersion") {
      "1.0.0"
    }
  }
}