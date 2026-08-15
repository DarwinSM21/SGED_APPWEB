import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/theme.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');
  // Se inyecta para que el tema/fuente guardados se apliquen al arrancar,
  // antes de que se renderice ninguna pantalla (evita parpadeo de tema).
  private readonly theme = inject(ThemeService);
}
