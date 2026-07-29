import {
  ComponentFixture,
  TestBed
} from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';

import { Norma43Service } from '../../../../core/services/norma43.service';
import { ComunidadStateService } from '../../../../core/state/comunidad-state.service';
import { Norma43Import } from './norma43-import';

describe('Norma43Import', () => {

  let fixture: ComponentFixture<Norma43Import>;
  let component: Norma43Import;

  const comunidadSubject = new BehaviorSubject({
    id: 18,
    nombre: 'Comunidad de Propietarios Test'
  });

  const norma43Service = {
    previsualizar: () => of({
      comunidadId: 18,
      nombreFichero: 'extracto.txt',
      numeroMovimientos: 0,
      totalDebe: 0,
      totalHaber: 0,
      fechaInicial: null,
      fechaFinal: null,
      movimientos: []
    }),

    importar: () => of([])
  };

  const comunidadState = {
    init: () => undefined,
    comunidad$: comunidadSubject.asObservable()
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        Norma43Import
      ],
      providers: [
        provideRouter([]),
        {
          provide: Norma43Service,
          useValue: norma43Service
        },
        {
          provide: ComunidadStateService,
          useValue: comunidadState
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(
      Norma43Import
    );

    component = fixture.componentInstance;

    fixture.detectChanges();
  });

  it('debe crear el componente con la comunidad activa', () => {
    expect(component).toBeTruthy();
    expect(component.comunidad?.id).toBe(18);
    expect(component.comunidad?.nombre).toBe(
      'Comunidad de Propietarios Test'
    );
  });
});
