import ExpoPrinterModule from "./ExpoPrinterModule";
import { Platform } from "react-native";

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

  disconnect() {
    return ExpoPrinterModule.disconnect();
  },
};

export default Printer;