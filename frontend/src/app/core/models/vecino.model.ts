export interface Comunidad {
  id?: number;

  nombre: string;
  direccion?: string;
  poblacion?: string;
  codigoPostal?: string;
  provincia?: string;
  paiscod?: string;

  nifCif?: string;
  identificadorAcreedor: string;
  sufijo?: string;

  iban: string;
  bic?: string;

  tipoReparto?: 'PARTES_IGUALES' | 'COEFICIENTE';

  activa?: boolean;
}
