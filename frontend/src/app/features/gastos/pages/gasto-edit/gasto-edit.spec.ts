import {
  ComponentFixture,
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
  provideRouter
} from '@angular/router';

import {
  GastoEdit
} from './gasto-edit';

describe('GastoEdit', () => {

  let component: GastoEdit;
  let fixture: ComponentFixture<GastoEdit>;

  let httpTesting:
    HttpTestingController;

  beforeEach(async () => {

    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [
        GastoEdit
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture =
      TestBed.createComponent(
        GastoEdit
      );

    component =
      fixture.componentInstance;

    httpTesting =
      TestBed.inject(
        HttpTestingController
      );
  });

  afterEach(() => {

    httpTesting.verify();

    localStorage.clear();
  });

  it(
    'no debe guardar un gasto sin proveedor',
    () => {

      component.gastoForm.setValue({
        comunidadId: 18,
        concepto: 'Factura de prueba',
        fechaFactura: '2026-09-16',
        importeTotal: 100,
        numeroFactura: 'F-001',
        proveedor: '',
        cuentaGastoId: 1
      });

      component.guardar();

      expect(
        component.error
      ).toBe(
        'El proveedor es obligatorio.'
      );

      expect(
        component.guardando
      ).toBe(false);

      httpTesting.expectNone(
        '/api/gastos'
      );
    }
  );

  it(
    'debe enviar un POST al crear un gasto válido',
    () => {

      component.gastoForm.setValue({
        comunidadId: 18,
        concepto: '  Factura electricidad  ',
        fechaFactura: '2026-09-16',
        importeTotal: 125.50,
        numeroFactura: '  F-002  ',
        proveedor: '  Proveedor prueba  ',
        cuentaGastoId: 1
      });

      component.guardar();

      const peticion =
        httpTesting.expectOne(
          '/api/gastos'
        );

      expect(
        peticion.request.method
      ).toBe('POST');

      expect(
        peticion.request.body
      ).toEqual({
        comunidadId: 18,
        concepto: 'Factura electricidad',
        fechaFactura: '2026-09-16',
        importeTotal: 125.50,
        numeroFactura: 'F-002',
        proveedor: 'Proveedor prueba',
        cuentaGastoId: 1
      });

      expect(
        component.guardando
      ).toBe(true);

      peticion.flush({
        id: 100,
        comunidadId: 18,
        concepto: 'Factura electricidad',
        fechaFactura: '2026-09-16',
        importeTotal: 125.50,
        numeroFactura: 'F-002',
        proveedor: 'Proveedor prueba',
        cuentaGastoId: 1,
        fechaPago: null,
        pagado: false,
        numeroAsiento: null,
        rutaPdf: null
      });

      expect(
        component.guardando
      ).toBe(false);

      expect(
        component.mensaje
      ).toBe(
        'Gasto creado correctamente.'
      );
    }
  );

  it(
    'debe enviar un PUT al actualizar un gasto existente',
    () => {

      component.gastoId = 23;

      component.gastoForm.setValue({
        comunidadId: 33,
        concepto: '  Gasto actualizado  ',
        fechaFactura: '2026-09-16',
        importeTotal: 217.71,
        numeroFactura: '  FAC-23  ',
        proveedor: '  Proveedor actualizado  ',
        cuentaGastoId: 1
      });

      component.guardar();

      const peticion =
        httpTesting.expectOne(
          '/api/gastos/23'
        );

      expect(
        peticion.request.method
      ).toBe('PUT');

      expect(
        peticion.request.body
      ).toEqual({
        comunidadId: 33,
        concepto: 'Gasto actualizado',
        fechaFactura: '2026-09-16',
        importeTotal: 217.71,
        numeroFactura: 'FAC-23',
        proveedor: 'Proveedor actualizado',
        cuentaGastoId: 1
      });

      peticion.flush({
        id: 23,
        comunidadId: 33,
        concepto: 'Gasto actualizado',
        fechaFactura: '2026-09-16',
        importeTotal: 217.71,
        numeroFactura: 'FAC-23',
        proveedor: 'Proveedor actualizado',
        cuentaGastoId: 1,
        fechaPago: null,
        pagado: false,
        numeroAsiento: null,
        rutaPdf: null
      });

      expect(
        component.guardando
      ).toBe(false);

      expect(
        component.mensaje
      ).toBe(
        'Gasto actualizado correctamente.'
      );
    }
  );

  it(
    'debe bloquear la edición de un gasto contabilizado',
    () => {

      component.gastoId = 23;

      component.cargarGasto();

      const peticionGasto =
        httpTesting.expectOne(
          '/api/gastos/23'
        );

      expect(
        peticionGasto.request.method
      ).toBe('GET');

      peticionGasto.flush({
        id: 23,
        comunidadId: 33,
        concepto: 'Gasto contabilizado',
        fechaFactura: '2026-09-16',
        importeTotal: 217.71,
        numeroFactura: 'FAC-23',
        proveedor: 'Proveedor prueba',
        cuentaGastoId: 1,
        fechaPago: null,
        pagado: false,
        numeroAsiento: 'ASIENTO-23',
        rutaPdf: null
      });

      const peticionCuentas =
        httpTesting.expectOne(
          '/api/cuentas-contables/comunidad/33'
        );

      expect(
        peticionCuentas.request.method
      ).toBe('GET');

      peticionCuentas.flush([]);

      expect(
        component.bloqueado
      ).toBe(true);

      expect(
        component.motivoBloqueo
      ).toContain(
        'Este gasto está contabilizado.'
      );

      component.guardar();

      httpTesting.expectNone(
        '/api/gastos/23'
      );
    }
  );

  it(
    'debe bloquear la edición de un gasto pagado',
    () => {

      component.gastoId = 24;

      component.cargarGasto();

      const peticionGasto =
        httpTesting.expectOne(
          '/api/gastos/24'
        );

      expect(
        peticionGasto.request.method
      ).toBe('GET');

      peticionGasto.flush({
        id: 24,
        comunidadId: 33,
        concepto: 'Gasto pagado',
        fechaFactura: '2026-09-16',
        importeTotal: 100,
        numeroFactura: 'FAC-24',
        proveedor: 'Proveedor prueba',
        cuentaGastoId: 1,
        fechaPago: '2026-09-16',
        pagado: true,
        numeroAsiento: 'ASIENTO-24',
        rutaPdf: null
      });

      const peticionCuentas =
        httpTesting.expectOne(
          '/api/cuentas-contables/comunidad/33'
        );

      peticionCuentas.flush([]);

      expect(
        component.bloqueado
      ).toBe(true);

      expect(
        component.motivoBloqueo
      ).toContain(
        'Este gasto está pagado.'
      );

      component.guardar();

      httpTesting.expectNone(
        '/api/gastos/24'
      );
    }
  );

  it(
    'debe mostrar el mensaje del backend ante un conflicto 409',
    () => {

      component.gastoId = 25;

      component.gastoForm.setValue({
        comunidadId: 33,
        concepto: 'Gasto con conflicto',
        fechaFactura: '2026-09-16',
        importeTotal: 150,
        numeroFactura: 'FAC-25',
        proveedor: 'Proveedor prueba',
        cuentaGastoId: 1
      });

      component.guardar();

      const peticion =
        httpTesting.expectOne(
          '/api/gastos/25'
        );

      expect(
        peticion.request.method
      ).toBe('PUT');

      peticion.flush(
        {
          detail:
            'El gasto está contabilizado y no puede editarse.'
        },
        {
          status: 409,
          statusText: 'Conflict'
        }
      );

      expect(
        component.guardando
      ).toBe(false);

      expect(
        component.error
      ).toBe(
        'El gasto está contabilizado y no puede editarse.'
      );
    }
  );

  it(
    'debe cargar los datos del gasto en el formulario reactivo',
    () => {

      component.gastoId = 26;

      component.cargarGasto();

      const peticionGasto =
        httpTesting.expectOne(
          '/api/gastos/26'
        );

      peticionGasto.flush({
        id: 26,
        comunidadId: 33,
        concepto: 'Factura electricidad',
        fechaFactura: '2026-09-17',
        importeTotal: 125.50,
        numeroFactura: 'FAC-26',
        proveedor: 'Proveedor prueba',
        cuentaGastoId: 7,
        fechaPago: null,
        pagado: false,
        numeroAsiento: null,
        rutaPdf: null
      });

      const peticionCuentas =
        httpTesting.expectOne(
          '/api/cuentas-contables/comunidad/33'
        );

      peticionCuentas.flush([]);

      expect(
        component.gastoForm.getRawValue()
      ).toEqual({
        comunidadId: 33,
        concepto: 'Factura electricidad',
        fechaFactura: '2026-09-17',
        importeTotal: 125.50,
        numeroFactura: 'FAC-26',
        proveedor: 'Proveedor prueba',
        cuentaGastoId: 7
      });
    }
  );

  it(
    'debe crear un gasto usando los valores del formulario reactivo',
    () => {

      component.ngOnInit();

      component.gastoForm.setValue({
        comunidadId: 18,
        concepto: '  Factura desde Reactive Forms  ',
        fechaFactura: '2026-09-17',
        importeTotal: 175.25,
        numeroFactura: '  RF-001  ',
        proveedor: '  Proveedor Reactive  ',
        cuentaGastoId: 7
      });

      component.guardar();

      const peticion =
        httpTesting.expectOne(
          '/api/gastos'
        );

      expect(
        peticion.request.method
      ).toBe('POST');

      expect(
        peticion.request.body
      ).toEqual({
        comunidadId: 18,
        concepto:
          'Factura desde Reactive Forms',
        fechaFactura:
          '2026-09-17',
        importeTotal:
          175.25,
        numeroFactura:
          'RF-001',
        proveedor:
          'Proveedor Reactive',
        cuentaGastoId:
          7
      });

      peticion.flush({
        id: 101,
        comunidadId: 18,
        concepto:
          'Factura desde Reactive Forms',
        fechaFactura:
          '2026-09-17',
        importeTotal:
          175.25,
        numeroFactura:
          'RF-001',
        proveedor:
          'Proveedor Reactive',
        cuentaGastoId:
          null,
        fechaPago: null,
        pagado: false,
        numeroAsiento: null,
        rutaPdf: null
      });

      expect(
        component.guardando
      ).toBe(false);

      expect(
        component.mensaje
      ).toBe(
        'Gasto creado correctamente.'
      );
    }
  );

  it(
    'no debe guardar un gasto sin cuenta contable',
    () => {

      component.gastoForm.setValue({
        comunidadId: 18,
        concepto: 'Gasto sin cuenta',
        fechaFactura: '2026-09-17',
        importeTotal: 100,
        numeroFactura: 'SIN-CUENTA',
        proveedor: 'Proveedor prueba',
        cuentaGastoId: null
      });

      component.guardar();

      expect(
        component.error
      ).toBe(
        'La cuenta contable es obligatoria.'
      );

      expect(
        component.guardando
      ).toBe(false);

      httpTesting.expectNone(
        '/api/gastos'
      );
    }
  );
});
