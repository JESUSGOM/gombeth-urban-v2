import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VecinoEdit } from './vecino-edit';

describe('VecinoEdit', () => {
  let component: VecinoEdit;
  let fixture: ComponentFixture<VecinoEdit>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VecinoEdit],
    }).compileComponents();

    fixture = TestBed.createComponent(VecinoEdit);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
