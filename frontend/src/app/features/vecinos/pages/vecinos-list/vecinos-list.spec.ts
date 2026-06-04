import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VecinosList } from './vecinos-list';

describe('VecinosList', () => {
  let component: VecinosList;
  let fixture: ComponentFixture<VecinosList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VecinosList],
    }).compileComponents();

    fixture = TestBed.createComponent(VecinosList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
