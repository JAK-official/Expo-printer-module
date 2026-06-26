// Define your exported module types here.
export type PrinterDevice = {
  id: string;
  name?: string;
};

export type PrinterStatus = {
  connected: boolean;
};

export type PrintTextOptions = {
  text: string;
};

export type XpTt426bLabel = {
  productName: string;
  productLine2: string;
  productLine3: string;
  productionDate: string;
  lineTitle: string;
  lineValue: string;
  casesOnPallet: string;
  palletNo: string;
  barcodeValue: string;
};