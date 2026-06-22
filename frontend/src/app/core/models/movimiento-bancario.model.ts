export interface MovimientoBancario {
  id: number;
  comunidadId: number;
  fechaOperacion: string;
  fechaValor: string;
  importe: number;
  signo: string;
  concepto: string;
  conceptoCompleto: string | null;
  referenciaBancaria: string | null;
  procesado: boolean;
  conciliado: boolean;
  documentoExtra: string | null;
}
