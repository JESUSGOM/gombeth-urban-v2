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
  Gasto
} from '../../../../core/models/gasto.model';

import {
  ComunidadStateService
} from '../../../../core/state/comunidad-state.service';

import {
  GastosList
} from './gastos-list';

import {
  vi
} from 'vitest';

describe('GastosList', () => {

  let component: GastosList;
  let fixture: ComponentFixture<GastosList>;

  let httpTesting:
    HttpTestingController;

  let comunidadState:
    ComunidadStateService;

  beforeEach(async () => {

    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [
        GastosList
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture =
      TestBed.createComponent(
        GastosList
      );

    component =
      fixture.componentInstance;

    httpTesting =
      TestBed.inject(
        HttpTestingController
      );

    comunidadState =
      TestBed.inject(
        ComunidadStateService
      );
  });

  afterEach(() => {

    httpTesting.verify();

    localStorage.clear();
  });

  it(
    'no debe contar como contabilizado un gasto que ya está pagado',
    () => {

      const contabilizado: Gasto = {
        id: 1,
        concepto: 'Gasto contabilizado',
        fechaFactura: '2026-09-01',
        importeTotal: 100,
        numeroFactura: 'F-1',
        proveedor: 'Proveedor 1',
        comunidadId: 18,
        cuentaGastoId: 1,
        fechaPago: null,
        pagado: false,
        numeroAsiento: 'A-1',
        rutaPdf: null
      };

      const pagado: Gasto = {
        id: 2,
        concepto: 'Gasto pagado',
        fechaFactura: '2026-09-02',
        importeTotal: 200,
        numeroFactura: 'F-2',
        proveedor: 'Proveedor 2',
        comunidadId: 18,
        cuentaGastoId: 1,
        fechaPago: '2026-09-10',
        pagado: true,
        numeroAsiento: 'A-2',
        rutaPdf: null
      };

      component.gastos = [
        contabilizado,
        pagado
      ];

      expect(
        component.numeroContabilizados
      ).toBe(1);

      expect(
        component.numeroPagados
      ).toBe(1);
    }
  );

  it(
    'debe cancelar la petición anterior al cambiar de comunidad',
    () => {

      component.ngOnInit();

      comunidadState.setComunidad({
        id: 18,
        nombre: 'Comunidad A'
      });

      const peticionA =
        httpTesting.expectOne(
          request =>
            request.url === '/api/gastos'
            && request.params.get(
              'comunidadId'
            ) === '18'
        );

      expect(
        peticionA.cancelled
      ).toBe(false);

      comunidadState.setComunidad({
        id: 33,
        nombre: 'Comunidad B'
      });

      expect(
        peticionA.cancelled
      ).toBe(true);

      const peticionB =
        httpTesting.expectOne(
          request =>
            request.url === '/api/gastos'
            && request.params.get(
              'comunidadId'
            ) === '33'
        );

      const gastoB: Gasto = {
        id: 33,
        concepto: 'Gasto comunidad B',
        fechaFactura: '2026-09-16',
        importeTotal: 217.71,
        numeroFactura: 'B-1',
        proveedor: 'Proveedor B',
        comunidadId: 33,
        cuentaGastoId: 1,
        fechaPago: null,
        pagado: false,
        numeroAsiento: 'ASIENTO-B',
        rutaPdf: null
      };

      peticionB.flush([
        gastoB
      ]);

      expect(
        component.comunidadId
      ).toBe(33);

      expect(
        component.nombreComunidad
      ).toBe('Comunidad B');

      expect(
        component.gastos.length
      ).toBe(1);

      expect(
        component.gastos[0].id
      ).toBe(33);

      expect(
        component.gastos[0].comunidadId
      ).toBe(33);
    }
  );

  it(
    'debe bloquear el pago desde la interfaz durante la convivencia',
    () => {

      const gasto: Gasto = {
        id: 25,
        concepto: 'Gasto contabilizado',
        fechaFactura: '2026-09-17',
        importeTotal: 23.45,
        numeroFactura: 'TEST-2B-20260917',
        proveedor: 'PRUEBA PASO 2B',
        comunidadId: 33,
        cuentaGastoId: 1957,
        fechaPago: null,
        pagado: false,
        numeroAsiento: 'GASTO-25-ASIENTO-4',
        rutaPdf: null
      };

      expect(
        component.pagosHabilitadosEnInterfaz
      ).toBe(false);

      expect(
        component.puedePagar(gasto)
      ).toBe(false);

      expect(
        component.motivoNoPagar(gasto)
      ).toBe(
        component.avisoPagos
      );

      component.pagarGasto(
        gasto
      );

      expect(
        component.errorOperacion
      ).toBe(
        component.avisoPagos
      );

      expect(
        component.mensajeOperacion
      ).toBe('');

      expect(
        component.procesandoGastoId
      ).toBeNull();

      httpTesting.expectNone(
        '/api/gastos/25/pagar'
      );
    }
  );

  it(
    'debe bloquear deshacer pago desde la interfaz durante la convivencia',
    () => {

      const gasto: Gasto = {
        id: 22,
        concepto: 'Limpieza mes febrero 2026',
        fechaFactura: '2026-02-28',
        importeTotal: 82.82,
        numeroFactura: '37',
        proveedor: 'María Bueviaje Martín',
        comunidadId: 17,
        cuentaGastoId: 1862,
        fechaPago: '2026-03-10',
        pagado: true,
        numeroAsiento: 'FRA-E8AD54A3',
        rutaPdf: null
      };

      expect(
        component.pagosHabilitadosEnInterfaz
      ).toBe(false);

      expect(
        component.puedeDeshacerPago(gasto)
      ).toBe(false);

      expect(
        component.motivoNoDeshacerPago(
          gasto
        )
      ).toBe(
        component.avisoPagos
      );

      component.deshacerPagoGasto(
        gasto
      );

      expect(
        component.errorOperacion
      ).toBe(
        component.avisoPagos
      );

      expect(
        component.mensajeOperacion
      ).toBe('');

      expect(
        component.procesandoGastoId
      ).toBeNull();

      httpTesting.expectNone(
        '/api/gastos/22/deshacer-pago'
      );
    }
  );

  it(
    'debe adjuntar un PDF valido al gasto',
    () => {

      const gasto: Gasto = {
        id: 25,
        concepto: 'Gasto prueba PDF',
        fechaFactura: '2026-09-17',
        importeTotal: 23.45,
        numeroFactura: 'PDF-25',
        proveedor: 'Proveedor prueba',
        comunidadId: 33,
        cuentaGastoId: 1957,
        fechaPago: null,
        pagado: false,
        numeroAsiento: null,
        rutaPdf: null
      };

      component.gastos = [
        gasto
      ];

      const archivo =
        new File(
          [
            '%PDF-1.7 factura prueba'
          ],
          'factura-prueba.pdf',
          {
            type: 'application/pdf'
          }
        );

      component.adjuntarPdf(
        gasto,
        archivo
      );

      expect(
        component.procesandoPdfGastoId
      ).toBe(25);

      const peticion =
        httpTesting.expectOne(
          '/api/gastos/25/pdf'
        );

      expect(
        peticion.request.method
      ).toBe('POST');

      expect(
        peticion.request.body
        instanceof FormData
      ).toBe(true);

      const formData =
        peticion.request.body as FormData;

      expect(
        formData.get(
          'file'
        )
      ).toBe(
        archivo
      );

      const actualizado: Gasto = {
        ...gasto,
        rutaPdf:
          '1776847724991_factura-prueba.pdf'
      };

      peticion.flush(
        actualizado
      );

      expect(
        component.procesandoPdfGastoId
      ).toBeNull();

      expect(
        component.gastos[0].rutaPdf
      ).toBe(
        '1776847724991_factura-prueba.pdf'
      );

      expect(
        component.mensajeOperacion
      ).toBe(
        'PDF de la factura adjuntado correctamente.'
      );

      expect(
        component.errorOperacion
      ).toBe('');
    }
  );

  it(
    'debe rechazar un fichero que no sea PDF',
    () => {

      const gasto: Gasto = {
        id: 25,
        concepto: 'Gasto prueba',
        fechaFactura: '2026-09-17',
        importeTotal: 23.45,
        numeroFactura: 'TEST-25',
        proveedor: 'Proveedor prueba',
        comunidadId: 33,
        cuentaGastoId: 1957,
        fechaPago: null,
        pagado: false,
        numeroAsiento: null,
        rutaPdf: null
      };

      const archivo =
        new File(
          [
            'contenido no pdf'
          ],
          'factura.txt',
          {
            type: 'text/plain'
          }
        );

      component.adjuntarPdf(
        gasto,
        archivo
      );

      expect(
        component.errorOperacion
      ).toBe(
        'Solo se pueden adjuntar archivos PDF.'
      );

      expect(
        component.mensajeOperacion
      ).toBe('');

      expect(
        component.procesandoPdfGastoId
      ).toBeNull();

      httpTesting.expectNone(
        '/api/gastos/25/pdf'
      );
    }
  );

  it(
    'no debe permitir sustituir un PDF ya asociado',
    () => {

      const gasto: Gasto = {
        id: 23,
        concepto: 'Gasto histórico',
        fechaFactura: '2026-04-22',
        importeTotal: 100,
        numeroFactura: 'FAT-2026-054412',
        proveedor: 'ATENCO ENERGIA SL',
        comunidadId: 18,
        cuentaGastoId: 1,
        fechaPago: null,
        pagado: false,
        numeroAsiento: null,
        rutaPdf:
          '1776847724991_FAT-2026-054412.pdf'
      };

      const archivo =
        new File(
          [
            '%PDF-1.7 sustitucion'
          ],
          'nuevo.pdf',
          {
            type: 'application/pdf'
          }
        );

      expect(
        component.tienePdf(
          gasto
        )
      ).toBe(true);

      expect(
        component.puedeAdjuntarPdf(
          gasto
        )
      ).toBe(false);

      component.adjuntarPdf(
        gasto,
        archivo
      );

      expect(
        component.errorOperacion
      ).toBe(
        'El gasto ya tiene un PDF asociado. '
        + 'Durante la convivencia no se permite sustituirlo.'
      );

      expect(
        component.procesandoPdfGastoId
      ).toBeNull();

      httpTesting.expectNone(
        '/api/gastos/23/pdf'
      );
    }
  );

  it(
    'no debe intentar abrir un PDF si el gasto no tiene documento',
    () => {

      const gasto: Gasto = {
        id: 22,
        concepto: 'Gasto sin PDF',
        fechaFactura: '2026-02-28',
        importeTotal: 82.82,
        numeroFactura: '37',
        proveedor: 'Proveedor histórico',
        comunidadId: 17,
        cuentaGastoId: 1862,
        fechaPago: null,
        pagado: false,
        numeroAsiento: 'FRA-E8AD54A3',
        rutaPdf: null
      };

      component.verPdf(
        gasto
      );

      expect(
        component.errorOperacion
      ).toBe(
        'El gasto no tiene un PDF asociado.'
      );

      expect(
        component.mensajeOperacion
      ).toBe('');

      expect(
        component.procesandoPdfGastoId
      ).toBeNull();

      httpTesting.expectNone(
        '/api/gastos/22/pdf'
      );
    }
  );

  it(
    'debe eliminar un gasto pendiente confirmado',
    () => {

      const gasto: Gasto = {
        id: 25,
        concepto: 'Gasto pendiente',
        fechaFactura: '2026-09-17',
        importeTotal: 23.45,
        numeroFactura: 'TEST-25',
        proveedor: 'Proveedor prueba',
        comunidadId: 33,
        cuentaGastoId: 1957,
        fechaPago: null,
        pagado: false,
        numeroAsiento: null,
        rutaPdf: '1776847724992_factura-prueba.pdf'
      };

      component.gastos = [
        gasto
      ];

      component.aplicarFiltros();

      const confirmSpy =
        vi.spyOn(
          window,
          'confirm'
        ).mockReturnValue(
          true
        );

      component.eliminarGasto(
        gasto
      );

      expect(
        confirmSpy
      ).toHaveBeenCalled();

      expect(
        component.procesandoGastoId
      ).toBe(25);

      const peticion =
        httpTesting.expectOne(
          '/api/gastos/25'
        );

      expect(
        peticion.request.method
      ).toBe(
        'DELETE'
      );

      peticion.flush(
        null
      );

      expect(
        component.procesandoGastoId
      ).toBeNull();

      expect(
        component.gastos.length
      ).toBe(0);

      expect(
        component.mensajeOperacion
      ).toBe(
        'Gasto eliminado correctamente.'
      );

      expect(
        component.errorOperacion
      ).toBe('');

      confirmSpy.mockRestore();
    }
  );
});
