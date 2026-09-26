import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Sidebar } from '../sidebar/sidebar';
import { Navbar } from '../navbar/navbar';

import {
  GombethDialogComponent
} from '../../shared/gombeth-dialog/gombeth-dialog';

@Component({
  selector: 'app-main-layout',
  imports: [
    RouterOutlet,
    Sidebar,
    Navbar,
    GombethDialogComponent
  ],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout {
}
