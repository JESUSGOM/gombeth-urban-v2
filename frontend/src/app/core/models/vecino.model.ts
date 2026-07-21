export interface Vecino {
  id: number;
  comunidadId: number;
  nombre: string;
  vivienda?: string;
  nif?: string;
  iban?: string;
  bic?: string;
  email?: string;
  telefono1?: string;
  telefono2?: string;
  telefono3?: string;
  direccion?: string;
  poblacion?: string;
  provincia?: string;
  codigoPostal?: string;
  paisCod?: string;
  referenciaMandato?: string;
  fechaMandato?: string | null;
  direccionNotificacion?: string;
  rutaMandatoFirmado?: string;
  cuentaContableId?: number;
  cuentaContable?: string;
  coeficiente?: number;
  domiciliado?: boolean;
  envioDigital?: boolean;
  activo?: boolean;
  notas?: string;
}
