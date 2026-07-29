export interface Norma43MovimientoPreview {
  fechaOperacion: string | null;
  fechaValor: string | null;
  signo: string;
  tipo: 'DEBE' | 'HABER' | 'DESCONOCIDO';
  importe: number;
  concepto: string;
  conceptoCompleto: string | null;
  referenciaBancaria: string | null;
  documentoExtra: string | null;
}

export interface Norma43Previsualizacion {
  comunidadId: number;
  nombreFichero: string;
  numeroMovimientos: number;
  totalDebe: number;
  totalHaber: number;
  fechaInicial: string | null;
  fechaFinal: string | null;
  movimientos: Norma43MovimientoPreview[];
}
