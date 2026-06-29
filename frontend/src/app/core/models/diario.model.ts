export interface Diario {
  id: number;
  comunidadId: number;
  ejercicio: number;
  numeroAsiento: number;
  fecha: string;
  concepto: string;
  origen: string;
  origenId: number;
  estado: string;
  usuarioId: number | null;
  fechaCreacion: string;
}

export interface MovimientoDiario {
  id: number;
  concepto: string;
  debe: number;
  haber: number;
  fecha: string;
  numeroAsiento: string;
  comunidadId: number;
  cuentaId: number;
  codigoCuenta?: string;
  nombreCuenta?: string;
}

export interface DiarioDetalle {
  asiento: Diario;
  movimientos: MovimientoDiario[];
  totalDebe: number;
  totalHaber: number;
  cuadrado: boolean;
}
