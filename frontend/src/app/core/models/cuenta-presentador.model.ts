export interface CuentaPresentador {
  id: number;
  alias: string;
  banco: string | null;
  identificadorPresentador: string;
  nifCif: string | null;
  sufijo: string | null;
  iban: string | null;
  bic: string | null;
  activa: boolean;
  observaciones: string | null;
}

export interface CuentaPresentadorRequest {
  alias: string;
  banco: string | null;
  identificadorPresentador: string;
  nifCif: string | null;
  sufijo: string | null;
  iban: string | null;
  bic: string | null;
  activa: boolean;
  observaciones: string | null;
}