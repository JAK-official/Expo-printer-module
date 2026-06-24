import { registerWebModule, NativeModule } from 'expo';

class ExpoPrinterModule extends NativeModule<{}> {}

export default registerWebModule(ExpoPrinterModule, 'ExpoPrinterModule');
