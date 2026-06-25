import { NativeModule, requireNativeModule } from "expo";

declare class ExpoPrinterModule extends NativeModule {
  getVersion(): string;

  hasBluetoothPermissions(): boolean;

  getRequiredPermissions(): string[];

  connect(address: string): Promise<string>;

  isConnected(): boolean;

  printTest(): void;

  disconnect(): boolean;
}

export default requireNativeModule<ExpoPrinterModule>("ExpoPrinter");