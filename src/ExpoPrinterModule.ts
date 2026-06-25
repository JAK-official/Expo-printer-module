import { NativeModule, requireNativeModule } from 'expo';

declare class ExpoPrinterModule extends NativeModule {
  getVersion(): string;
}

export default requireNativeModule<ExpoPrinterModule>('ExpoPrinter');
