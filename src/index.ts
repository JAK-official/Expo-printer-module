// Reexport the native module. On web, it will be resolved to ExpoPrinterModule.web.ts
// and on native platforms to ExpoPrinterModule.ts
export { default } from './ExpoPrinterModule';
export * from './ExpoPrinter.types';
