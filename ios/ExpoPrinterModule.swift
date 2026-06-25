import ExpoModulesCore

public class ExpoPrinterModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ExpoPrinter")

    Function("getVersion") {
      return "1.0.0"
    }
  }
}
