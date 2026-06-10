export interface CuotaPresupuesto {
  id: number;
  revisionId?: number;       // referencia a la revisión de presupuesto
  version?: string;          // V1, V2, ...
  comunidadId: number;
  vecinoId: number;
  nombre: string;
  vivienda: string;
  anio: number;
  descripcion: string;
  mesInicio?: number;
  mesFin?: number;
  coeficiente: number;
  importeAnual: number;
  importeMensual: number;
  motivoRevision?: string;
  estado: string;
  fechaGeneracion: string;
}
