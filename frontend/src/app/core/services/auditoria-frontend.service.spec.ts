import {
  DOCUMENT
} from '@angular/common';

import {
  provideHttpClient
} from '@angular/common/http';

import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';

import {
  TestBed
} from '@angular/core/testing';

import {
  vi
} from 'vitest';

import {
  Router
} from '@angular/router';

import {
  EMPTY
} from 'rxjs';

import {
  AuditoriaFrontendService
} from './auditoria-frontend.service';

import {
  AuthService
} from './auth.service';

import {
  ComunidadStateService
} from '../state/comunidad-state.service';

describe(
  'AuditoriaFrontendService',
  () => {

    let service:
      AuditoriaFrontendService;

    let httpTesting:
      HttpTestingController;

    let errorListener:
      EventListener | null;

    let rejectionListener:
      EventListener | null;

    const authServiceMock = {
      getUsuario:
        vi.fn()
    };

    const comunidadStateMock = {
      getComunidad:
        vi.fn()
    };

    const routerMock = {
      events: EMPTY,
      url: '/prueba'
    };

    const documentMock = {
      addEventListener:
        vi.fn()
    };

    beforeEach(() => {

      errorListener =
        null;

      rejectionListener =
        null;

      authServiceMock
        .getUsuario
        .mockReturnValue({
          id: 4,
          username: 'Probador'
        });

      comunidadStateMock
        .getComunidad
        .mockReturnValue({
          id: 33,
          nombre:
            'Comunidad de Prueba'
        });

      vi.spyOn(
        window,
        'addEventListener'
      ).mockImplementation(
        (
          (
            tipo: string,
            listener:
              EventListenerOrEventListenerObject
          ) => {

            if (
              typeof listener
              !== 'function'
            ) {
              return;
            }

            if (tipo === 'error') {
              errorListener =
                listener;

              return;
            }

            if (
              tipo
              === 'unhandledrejection'
            ) {
              rejectionListener =
                listener;
            }
          }
        ) as any
      );

      TestBed.configureTestingModule({
        providers: [
          AuditoriaFrontendService,

          provideHttpClient(),

          provideHttpClientTesting(),

          {
            provide: AuthService,
            useValue: authServiceMock
          },

          {
            provide:
              ComunidadStateService,
            useValue:
              comunidadStateMock
          },

          {
            provide: Router,
            useValue: routerMock
          },

          {
            provide: DOCUMENT,
            useValue: documentMock
          }
        ]
      });

      service =
        TestBed.inject(
          AuditoriaFrontendService
        );

      httpTesting =
        TestBed.inject(
          HttpTestingController
        );

      service.inicializar();
    });

    afterEach(() => {

      httpTesting.verify();

      vi.restoreAllMocks();
    });

    it(
      'debe auditar un error JavaScript incluyendo su stack',
      () => {

        expect(
          errorListener
        ).not.toBeNull();

        const error =
          new Error(
            'FALLO_FRONTEND_TEST'
          );

        error.stack =
          'Error: FALLO_FRONTEND_TEST\n'
          + '    at componente.ts:123:45';

        const evento =
          new ErrorEvent(
            'error',
            {
              message:
                'FALLO_FRONTEND_TEST',
              filename:
                'main.js',
              lineno: 123,
              colno: 45,
              error
            }
          );

        errorListener!(
          evento
        );

        const peticion =
          httpTesting.expectOne(
            '/api/auditoria/frontend'
          );

        expect(
          peticion.request.method
        ).toBe(
          'POST'
        );

        const body =
          peticion.request.body;

        expect(
          body.tipoEvento
        ).toBe(
          'ERROR_JAVASCRIPT'
        );

        expect(
          body.ruta
        ).toBe(
          '/prueba'
        );

        expect(
          body.comunidadId
        ).toBe(
          33
        );

        expect(
          body.detalle
        ).toBe(
          'FALLO_FRONTEND_TEST'
        );

        expect(
          body.fichero
        ).toBe(
          'main.js'
        );

        expect(
          body.linea
        ).toBe(
          123
        );

        expect(
          body.columna
        ).toBe(
          45
        );

        expect(
          body.stack
        ).toContain(
          'FALLO_FRONTEND_TEST'
        );

        expect(
          body.stack
        ).toContain(
          'componente.ts:123:45'
        );

        peticion.flush(
          null
        );
      }
    );

    it(
      'debe auditar una promesa rechazada incluyendo su stack',
      () => {

        expect(
          rejectionListener
        ).not.toBeNull();

        const error =
          new Error(
            'PROMESA_FRONTEND_TEST'
          );

        error.stack =
          'Error: PROMESA_FRONTEND_TEST\n'
          + '    at servicio.ts:77:10';

        const evento =
          {
            reason: error
          } as PromiseRejectionEvent;

        rejectionListener!(
          evento
        );

        const peticion =
          httpTesting.expectOne(
            '/api/auditoria/frontend'
          );

        expect(
          peticion.request.method
        ).toBe(
          'POST'
        );

        const body =
          peticion.request.body;

        expect(
          body.tipoEvento
        ).toBe(
          'PROMESA_RECHAZADA'
        );

        expect(
          body.detalle
        ).toContain(
          'PROMESA_FRONTEND_TEST'
        );

        expect(
          body.stack
        ).toContain(
          'PROMESA_FRONTEND_TEST'
        );

        expect(
          body.stack
        ).toContain(
          'servicio.ts:77:10'
        );

        peticion.flush(
          null
        );
      }
    );

    it(
      'no debe enviar auditoria frontend sin usuario autenticado',
      () => {

        authServiceMock
          .getUsuario
          .mockReturnValue(
            null
          );

        expect(
          errorListener
        ).not.toBeNull();

        const error =
          new Error(
            'ERROR_SIN_SESION'
          );

        errorListener!(
          new ErrorEvent(
            'error',
            {
              message:
                'ERROR_SIN_SESION',
              error
            }
          )
        );

        httpTesting.expectNone(
          '/api/auditoria/frontend'
        );
      }
    );

    it(
      'debe limitar el tamano del stack enviado',
      () => {

        expect(
          errorListener
        ).not.toBeNull();

        const error =
          new Error(
            'STACK_LARGO'
          );

        error.stack =
          'X'.repeat(
            9000
          );

        errorListener!(
          new ErrorEvent(
            'error',
            {
              message:
                'STACK_LARGO',
              error
            }
          )
        );

        const peticion =
          httpTesting.expectOne(
            '/api/auditoria/frontend'
          );

        const body =
          peticion.request.body;

        expect(
          body.stack
        ).toBeTruthy();

        /*
         * limitar() conserva 8000 caracteres
         * y añade el carácter de elipsis.
         */
        expect(
          body.stack.length
        ).toBe(
          8001
        );

        peticion.flush(
          null
        );
      }
    );
  }
);