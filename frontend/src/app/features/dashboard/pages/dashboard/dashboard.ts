import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

import { Health, HealthResponse } from '../../../../core/services/health';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {

  private healthService = inject(Health);

  backendStatus = 'Desconocido';
  backendApp = '';

  ngOnInit(): void {

    this.healthService.getHealth().subscribe({
      next: (response: HealthResponse) => {
        this.backendStatus = response.status;
        this.backendApp = response.app;
      },
      error: (error) => {
        console.error(error);
        this.backendStatus = 'ERROR';
      }
    });

  }
}
