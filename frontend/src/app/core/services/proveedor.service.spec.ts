import {
  TestBed
} from '@angular/core/testing';

import {
  provideHttpClient
} from '@angular/common/http';

import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import {
  ProveedorService
} from './proveedor.service';

import {
  Proveedor,
  ProveedorComunidad,
  ProveedorGuardarRequest
} from '../models/proveedor.model';

describe('ProveedorService', () => {

  let service: ProveedorService;

  let httpTesting:
    HttpTestingController;

  beforeEach(() => {

    TestBed.configureTestingModule({
      providers: [
        ProveedorService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service =
      TestBed.inject(
        ProveedorService
      );

    httpTesting =
      TestBed.inject(
        HttpTestingController
      );
  });

  afterEach(() => {

    httpTesting.verify();
  });

  it(
    'debe listar los proveedores del administrador autenticado',
    () => {

      const respuesta: Proveedor[] = [
        proveedor(
          10,
          'Proveedor uno'
        ),
        proveedor(
          11,
          'Proveedor dos'
        )
      ];

      service
        .listar()
        .subscribe(
          resultado => {

            expect(
              resultado
            ).toEqual(
              respuesta
            );
          }
        );

      const peticion =
        httpTesting.expectOne(
          '/api/proveedores'
        );

      expect(
        peticion.request.method
      ).toBe('GET');

      peticion.flush(
        respuesta
      );
    }
  );

  it(
    'debe obtener un proveedor por id',
    () => {

      const respuesta =
        proveedor(
          10,
          'Proveedor uno'
        );

      service
        .obtener(
          10
        )
        .subscribe(
          resultado => {

            expect(
              resultado
            ).toEqual(
              respuesta
            );
          }
        );

      const peticion =
        httpTesting.expectOne(
          '/api/proveedores/10'
        );

      expect(
        peticion.request.method
      ).toBe('GET');

      peticion.flush(
        respuesta
      );
    }
  );

  it(
    'debe crear un proveedor',
    () => {

      const request:
        ProveedorGuardarRequest = {

        nombre:
          'Proveedor nuevo',

        nifCif:
          'B12345678',

        telefono:
          '922111222',

        email:
          'proveedor@prueba.es',

        observaciones:
          'Proveedor de prueba'
      };

      const respuesta =
        proveedor(
          25,
          'Proveedor nuevo'
        );

      service
        .crear(
          request
        )
        .subscribe(
          resultado => {

            expect(
              resultado
            ).toEqual(
              respuesta
            );
          }
        );

      const peticion =
        httpTesting.expectOne(
          '/api/proveedores'
        );

      expect(
        peticion.request.method
      ).toBe('POST');

      expect(
        peticion.request.body
      ).toEqual(
        request
      );

      peticion.flush(
        respuesta
      );
    }
  );

  it(
    'debe actualizar un proveedor',
    () => {

      const request:
        ProveedorGuardarRequest = {

        nombre:
          'Proveedor actualizado',

        nifCif:
          'B12345678',

        telefono:
          '922111222',

        email:
          'proveedor@prueba.es',

        observaciones:
          'Proveedor actualizado',

        activo:
          false
      };

      const respuesta: Proveedor = {
        ...proveedor(
          10,
          'Proveedor actualizado'
        ),
        activo: false
      };

      service
        .actualizar(
          10,
          request
        )
        .subscribe(
          resultado => {

            expect(
              resultado
            ).toEqual(
              respuesta
            );
          }
        );

      const peticion =
        httpTesting.expectOne(
          '/api/proveedores/10'
        );

      expect(
        peticion.request.method
      ).toBe('PUT');

      expect(
        peticion.request.body
      ).toEqual(
        request
      );

      peticion.flush(
        respuesta
      );
    }
  );

  it(
    'debe listar los proveedores asociados a una comunidad',
    () => {

      const respuesta:
        ProveedorComunidad[] = [
        asociacion(
          100,
          10,
          'Proveedor uno',
          373
        )
      ];

      service
        .listarPorComunidad(
          33
        )
        .subscribe(
          resultado => {

            expect(
              resultado
            ).toEqual(
              respuesta
            );
          }
        );

      const peticion =
        httpTesting.expectOne(
          '/api/comunidades/33/proveedores'
        );

      expect(
        peticion.request.method
      ).toBe('GET');

      peticion.flush(
        respuesta
      );
    }
  );

  it(
    'debe asociar un proveedor a una comunidad',
    () => {

      const respuesta =
        asociacion(
          100,
          10,
          'Proveedor uno',
          373
        );

      service
        .asociarAComunidad(
          33,
          10
        )
        .subscribe(
          resultado => {

            expect(
              resultado
            ).toEqual(
              respuesta
            );
          }
        );

      const peticion =
        httpTesting.expectOne(
          '/api/comunidades/33/proveedores/10'
        );

      expect(
        peticion.request.method
      ).toBe('POST');

      expect(
        peticion.request.body
      ).toBeNull();

      peticion.flush(
        respuesta
      );
    }
  );

  function proveedor(
    id: number,
    nombre: string
  ): Proveedor {

    return {
      id,
      nombre,
      nifCif: 'B12345678',
      telefono: '922111222',
      email: 'proveedor@prueba.es',
      observaciones: 'Proveedor de prueba',
      activo: true
    };
  }

  function asociacion(
    asociacionId: number,
    proveedorId: number,
    nombre: string,
    cuentaContableId: number
  ): ProveedorComunidad {

    return {
      asociacionId,
      proveedorId,
      nombre,
      nifCif: 'B12345678',
      telefono: '922111222',
      email: 'proveedor@prueba.es',
      observaciones: 'Proveedor de prueba',
      cuentaContableId,
      proveedorActivo: true,
      asociacionActiva: true
    };
  }
});
