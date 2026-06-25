import ExpoPrinterModule from "./ExpoPrinterModule";
import { Platform } from "react-native";
import { XpTt426bLabel } from "./ExpoPrinter.types";

export const Printer = {
  getVersion() {
    return ExpoPrinterModule.getVersion();
  },

  hasBluetoothPermissions() {
    if (Platform.OS !== "android") {
      return true;
    }
    return ExpoPrinterModule.hasBluetoothPermissions();
  },

  connect(address: string) {
    return ExpoPrinterModule.connect(address);
  },

  isConnected() {
    return ExpoPrinterModule.isConnected();
  },

  printTest() {
    return ExpoPrinterModule.printTest();
  },

  printXpTt426bLabel(data: XpTt426bLabel) {
    return ExpoPrinterModule.printXpTt426bLabel(data);
  },

  disconnect() {
    return ExpoPrinterModule.disconnect();
  },
};

export default Printer;