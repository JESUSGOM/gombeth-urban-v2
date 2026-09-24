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
  Gasto
} from '../models/gasto.model';

import {
  GastoService
} from './gasto.service';

describe('GastoService', () => {

  let service: GastoService;

  let httpTesting:
    HttpTestingController;

  beforeEach(() => {

    TestBed.configureTestingModule({
      providers: [
        GastoService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service =
      TestBed.inject(
        GastoService
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
    'debe enviar la fecha de pago al registrar un pago',
    () => {

      const respuesta: Gasto = {
        id: 25,
        concepto: 'Gasto prueba',
        fechaFactura: '2026-09-17',
        importeTotal: 23.45,
        numeroFactura: 'TEST-25',
        proveedor: 'Proveedor prueba',
        comunidadId: 33,
        cuentaGastoId: 1957,
        fechaPago: '2026-09-18',
        pagado: true,
        numeroAsiento: 'GASTO-25-ASIENTO-4',
        rutaPdf: null
      };

      service
        .pagar(
          25,
          '2026-09-18'
        )
        .subscribe(gasto => {

          expect(
            gasto.pagado
          ).toBe(true);

          expect(
            gasto.fechaPago
          ).toBe('2026-09-18');
        });

      const peticion =
        httpTesting.expectOne(
          request =>
            request.url
            === '/api/gastos/25/pagar'
            && request.params.get(
              'fechaPago'
            ) === '2026-09-18'
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

  it(
    'debe permitir registrar un pago sin enviar fecha',
    () => {

      service
        .pagar(
          25
        )
        .subscribe();

      const peticion =
        httpTesting.expectOne(
          request =>
            request.url
            === '/api/gastos/25/pagar'
            && !request.params.has(
              'fechaPago'
            )
        );

      expect(
        peticion.request.method
      ).toBe('POST');

      expect(
        peticion.request.body
      ).toBeNull();

      peticion.flush({
        id: 25
      });
    }
  );

  it(
    'debe enviar la fecha de anulacion al deshacer un pago',
    () => {

      const respuesta: Gasto = {
        id: 22,
        concepto: 'Gasto histórico',
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

      service
        .deshacerPago(
          22,
          '2026-09-18'
        )
        .subscribe(gasto => {

          expect(
            gasto.pagado
          ).toBe(false);

          expect(
            gasto.fechaPago
          ).toBeNull();
        });

      const peticion =
        httpTesting.expectOne(
          request =>
            request.url
            === '/api/gastos/22/deshacer-pago'
            && request.params.get(
              'fechaAnulacion'
            ) === '2026-09-18'
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

  it(
    'debe permitir deshacer un pago sin enviar fecha de anulacion',
    () => {

      service
        .deshacerPago(
          22
        )
        .subscribe();

      const peticion =
        httpTesting.expectOne(
          request =>
            request.url
            === '/api/gastos/22/deshacer-pago'
            && !request.params.has(
              'fechaAnulacion'
            )
        );

      expect(
        peticion.request.method
      ).toBe('POST');

      expect(
        peticion.request.body
      ).toBeNull();

      peticion.flush({
        id: 22
      });
    }
  );

  it(
    'debe subir el PDF de una factura como multipart form data',
    () => {

      const contenido =
        new Blob(
          [
            '%PDF-1.7 factura prueba'
          ],
          {
            type: 'application/pdf'
          }
        );

      const archivo =
        new File(
          [
            contenido
          ],
          'factura-prueba.pdf',
          {
            type: 'application/pdf'
          }
        );

      const respuesta: Gasto = {
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
        numeroAsiento: 'GASTO-25-ASIENTO-4',
        rutaPdf: '1776847724991_factura-prueba.pdf'
      };

      service
        .subirPdf(
          25,
          archivo
        )
        .subscribe(gasto => {

          expect(
            gasto.rutaPdf
          ).toBe(
            '1776847724991_factura-prueba.pdf'
          );
        });

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

      /*
       * No debemos establecer Content-Type manualmente.
       * El navegador añadirá multipart/form-data con
       * el boundary correcto.
       */
      expect(
        peticion.request.headers.has(
          'Content-Type'
        )
      ).toBe(false);

      peticion.flush(
        respuesta
      );
    }
  );

  it(
    'debe recuperar el PDF de una factura como blob',
    () => {

      let resultado:
        Blob | undefined;

      service
        .obtenerPdf(
          23
        )
        .subscribe(blob => {

          resultado =
            blob;
        });

      const peticion =
        httpTesting.expectOne(
          '/api/gastos/23/pdf'
        );

      expect(
        peticion.request.method
      ).toBe('GET');

      expect(
        peticion.request.responseType
      ).toBe('blob');

      const pdf =
        new Blob(
          [
            '%PDF-1.4 factura historica'
          ],
          {
            type: 'application/pdf'
          }
        );

      peticion.flush(
        pdf
      );

      expect(
        resultado
      ).toBeInstanceOf(
        Blob
      );

      expect(
        resultado?.type
      ).toBe(
        'application/pdf'
      );
    }
  );

  it(
    'debe enviar una factura al endpoint OCR',
    () => {

      const archivo =
        new File(
          [
            '%PDF-1.7 factura OCR'
          ],
          'factura.pdf',
          {
            type: 'application/pdf'
          }
        );

      const respuesta = {
        proveedor:
          'ATENCO ENERGIA SL',
        fechaFactura:
          '2026-09-15',
        importeTotal:
          217.71,
        numeroFactura:
          'FAT-2026-054412',
        ocrAplicado:
          true,
        advertencias: [
          'El PDF no contiene texto suficiente; se ha utilizado OCR.'
        ]
      };

      service
        .analizarFactura(
          33,
          archivo
        )
        .subscribe(resultado => {

          expect(
            resultado.proveedor
          ).toBe(
            'ATENCO ENERGIA SL'
          );

          expect(
            resultado.fechaFactura
          ).toBe(
            '2026-09-15'
          );

          expect(
            resultado.importeTotal
          ).toBe(
            217.71
          );

          expect(
            resultado.numeroFactura
          ).toBe(
            'FAT-2026-054412'
          );

          expect(
            resultado.ocrAplicado
          ).toBe(true);
        });

      const peticion =
        httpTesting.expectOne(
          request =>
            request.url
            === '/api/gastos/ocr'
            &&
            request.params.get(
              'comunidadId'
            ) === '33'
        );

      expect(
        peticion.request.method
      ).toBe(
        'POST'
      );

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

      peticion.flush(
        respuesta
      );
    }
  );

  it(
    'debe eliminar un gasto pendiente',
    () => {

      service
        .eliminar(
          25
        )
        .subscribe();

      const peticion =
        httpTesting.expectOne(
          '/api/gastos/25'
        );

      expect(
        peticion.request.method
      ).toBe(
        'DELETE'
      );

      expect(
        peticion.request.body
      ).toBeNull();

      peticion.flush(
        null
      );
    }
  );
});
