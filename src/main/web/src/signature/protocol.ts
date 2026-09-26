export type SignatureParameter = {
  label: string | [number, number];
  documentation?: string;
};

export type SignatureInfo = {
  label: string;
  documentation?: string;
  parameters: SignatureParameter[];
};

export type SignaturePopupState = {
  requestId: string;
  uri: string;
  version: number;
  signatures: SignatureInfo[];
  activeSignature: number;
  activeParameter: number;
  argValues: string[];
  anchor: { left: number; top: number };
};
