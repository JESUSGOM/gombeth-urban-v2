export interface Recibo {
  id: number;
  estado: string;
  fechaCobroBanco: string | null;
  fechaEmision: string;
  importe: number;
  comunidadId: number;
  movimientoBancarioId: number | null;
  vecinoId: number;
  cuotaPresupuestoId: number | null;
  pagadoAcumulado: number;
  concepto: string | null;
  tipoRemesa: string | null;
  etiquetaExtra: string | null;
}
