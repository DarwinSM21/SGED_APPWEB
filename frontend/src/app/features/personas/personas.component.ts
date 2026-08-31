import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../auth/auth.service';
import { PersonasAdminComponent } from './personas-admin.component';
import { PersonasRecepcionComponent } from './personas-recepcion.component';

@Component({
  selector: 'app-personas',
  standalone: true,
  imports: [CommonModule, PersonasAdminComponent, PersonasRecepcionComponent],
  template: `
    @if (esAdministrador()) {
      <app-personas-admin />
    } @else {
      <app-personas-recepcion />
    }
  `,
})
export class PersonasComponent {
  private readonly auth = inject(AuthService);
  readonly esAdministrador = computed(() => this.auth.currentUser()?.rol === 'ADMINISTRADOR');
}
