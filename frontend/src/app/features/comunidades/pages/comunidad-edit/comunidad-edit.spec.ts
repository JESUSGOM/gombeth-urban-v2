import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ComunidadEdit } from './comunidad-edit';

describe('ComunidadEdit', () => {
  let component: ComunidadEdit;
  let fixture: ComponentFixture<ComunidadEdit>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ComunidadEdit],
    }).compileComponents();

    fixture = TestBed.createComponent(ComunidadEdit);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
