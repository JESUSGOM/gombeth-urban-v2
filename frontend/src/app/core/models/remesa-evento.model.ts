export interface RemesaEvento {
  id: number;
  remesaId: number;
  comunidadId: number;
  usuarioId: number | null;
  tipoEvento: string;
  estadoAnterior: string | null;
  estadoNuevo: string | null;
  formato: string | null;
  nombreArchivo: string | null;
  fechaEvento: string;
  detalle: string | null;
}
