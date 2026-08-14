export interface Presupuesto {
  id: number;
  cuentaId: number;
  cuentaCodigo: string;
  cuentaDescripcion: string;
  anio: number;
  importe: number;
  metodoReparto: 'COEFICIENTE' | 'PARTES_IGUALES';
  aplicaTodos: boolean;
  vecinoIds: number[];
  numeroPropietariosAfectados: number | null;
}
