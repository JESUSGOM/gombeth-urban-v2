export interface PresupuestoRevision {
  id: number;
  comunidadId: number;
  anio: number;
  version: number;
  mesInicio: number;
  mesFin: number;
  importeRevision: number;
  estado: string;
  motivoRevision: string;
  fechaGeneracion: string;
}
