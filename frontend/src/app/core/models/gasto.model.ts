export interface Gasto {
  id: number;
  concepto: string | null;
  fechaFactura: string | null;
  importeTotal: number | null;
  numeroFactura: string | null;
  proveedor: string | null;
  comunidadId: number;
  cuentaGastoId: number | null;
  fechaPago: string | null;
  pagado: boolean | null;
  numeroAsiento: string | null;
  rutaPdf: string | null;
}
