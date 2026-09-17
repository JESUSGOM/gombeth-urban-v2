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
});
