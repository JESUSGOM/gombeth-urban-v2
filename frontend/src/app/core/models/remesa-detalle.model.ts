export interface RemesaLineaConceptoDetalle {
  id: number;
  descripcion: string;
  importe: number;
  orden: number;
  agrupadoEnUltimaLinea: boolean;
}

export interface RemesaLineaDetalle {
  id: number;
  vecinoId: number;
  vecino: string;
  reciboContableId: number | null;
  importe: number;
  domiciliado: boolean;
  incluidoSepa: boolean;
  pdfGenerado: boolean;
  emailEnviado: boolean;
  conceptos: RemesaLineaConceptoDetalle[];
}

export interface RemesaDetalle {
  id: number;
  comunidadId: number;
  comunidad: string;
  fechaCreacion: string;
  fechaCobro: string | null;
  estado: string;
  esquemaSepa: string;
  totalImporte: number;
  totalDomiciliado: number;
  totalNoDomiciliado: number;
  numeroRecibos: number;
  nombreArchivo: string | null;
  lineas: RemesaLineaDetalle[];
}
