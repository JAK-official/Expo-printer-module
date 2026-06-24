import { NativeModule, requireNativeModule } from 'expo';

declare class ExpoPrinterModule extends NativeModule<{}> {}

export default requireNativeModule<ExpoPrinterModule>('ExpoPrinter');
