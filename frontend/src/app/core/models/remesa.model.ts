export interface Remesa {
  id: number;
  comunidadId: number;
  identificadorFichero: string;
  fechaCreacion: string;
  totalImporte: number;
  numeroRecibos: number;
  nombreArchivo: string;
  estado: string;
  tipoRemesa: string;
  fechaCobro: string;
  esquemaSepa: string;
  totalDomiciliado: number;
  totalNoDomiciliado: number;
  observaciones: string;
}
