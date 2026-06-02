export interface Recibo {
  id?: number;

  vecinoId?: number;

  comunidadId?: number;

  concepto?: string;

  importe?: number;

  fechaVencimiento?: string;

  estado?: string;
}
