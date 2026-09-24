export interface Proveedor {
  id: number;
  nombre: string;
  nifCif: string | null;
  telefono: string | null;
  email: string | null;
  observaciones: string | null;
  activo: boolean;
}

export interface ProveedorGuardarRequest {
  nombre: string;
  nifCif: string | null;
  telefono: string | null;
  email: string | null;
  observaciones: string | null;
  activo?: boolean | null;
}

export interface ProveedorComunidad {
  asociacionId: number;
  proveedorId: number;
  nombre: string;
  nifCif: string | null;
  telefono: string | null;
  email: string | null;
  observaciones: string | null;
  cuentaContableId: number;
  proveedorActivo: boolean;
  asociacionActiva: boolean;
}
