export interface ValidacionRemesa {
  remesaId: number;
  valida: boolean;
  errores: number;
  mensajes: string[];
}
