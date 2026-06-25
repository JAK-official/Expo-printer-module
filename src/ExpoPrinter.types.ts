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
