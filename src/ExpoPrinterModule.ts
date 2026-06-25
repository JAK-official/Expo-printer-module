import { NativeModule, requireNativeModule } from "expo";

import { XpTt426bLabel } from "./ExpoPrinter.types";

declare class ExpoPrinterModule extends NativeModule {
  getVersion(): string;

  hasBluetoothPermissions(): boolean;

  getRequiredPermissions(): string[];

  connect(address: string): Promise<string>;

  isConnected(): boolean;

  printTest(): void;

  disconnect(): boolean;

  printXpTt426bLabel(data: XpTt426bLabel): Promise<void>;
  
}

export default requireNativeModule<ExpoPrinterModule>("ExpoPrinter");