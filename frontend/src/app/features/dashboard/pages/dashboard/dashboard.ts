import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Auth } from '../../../../auth/services/auth';

interface Categoria { name: string; val: number; color: string; }
interface Actividad {
  ini: string; av: string; name: string; act: string;
  cat: string; cc: string; date: string; st: string; sc: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  protected readonly sidebarOpen = signal(false);
  protected readonly year = signal(2026);

  // Usuario logueado (viene del login real)
  protected readonly user = this.auth.getUser();
  protected readonly userName = this.user?.nombre ?? 'Usuario';
  protected readonly userRol = this.formatRol(this.user?.rol ?? '');
  protected readonly userIni = this.iniciales(this.user?.nombre ?? 'US');

  protected readonly today = signal('');

  // ----- Datos (mock por ahora; luego se conectan a la API) -----
  protected readonly months = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic'];
  protected readonly barData: (number | null)[] = [52,60,55,72,64,68,null,null,null,null,null,null];
  protected readonly currentMonth = 5;
  protected readonly maxBar = Math.max(...this.barData.filter((v): v is number => v != null));

  protected readonly cats: Categoria[] = [
    { name: 'Sub-18', val: 45, color: '#10b981' },
    { name: 'Sub-15', val: 38, color: '#4f7cf6' },
    { name: 'Sub-12', val: 32, color: '#8b5cf6' },
  ];
  protected readonly totalJugadores = computed(() => this.cats.reduce((a, c) => a + c.val, 0));

  protected readonly actividades: Actividad[] = [
    { ini:'CL', av:'#10b981', name:'Carlos Lopez', act:'Pago de membresia', cat:'Sub-18', cc:'s18', date:'08 jun 2026', st:'Completado', sc:'ok' },
    { ini:'AM', av:'#4f7cf6', name:'Ana Martinez', act:'Asistencia RFID', cat:'Sub-15', cc:'s15', date:'08 jun 2026', st:'Presente', sc:'present' },
    { ini:'PG', av:'#8b5cf6', name:'Pedro Garcia', act:'Lesion registrada', cat:'Sub-15', cc:'s15', date:'07 jun 2026', st:'En recuperacion', sc:'rec' },
    { ini:'LS', av:'#f59e0b', name:'Luis Salazar', act:'Membresia vencida', cat:'Sub-12', cc:'s12', date:'07 jun 2026', st:'Vencida', sc:'due' },
    { ini:'VR', av:'#ec4899', name:'Valeria Ruiz', act:'Nueva inscripcion', cat:'Sub-18', cc:'s18', date:'06 jun 2026', st:'Completado', sc:'ok' },
  ];

  ngOnInit(): void {
    const m = ['ene','feb','mar','abr','may','jun','jul','ago','sep','oct','nov','dic'];
    const d = new Date(2026, 5, 29);
    this.today.set(`${d.getDate()} ${m[d.getMonth()]} ${d.getFullYear()}`);
  }

  // ----- Helpers de la dona (SVG) -----
  protected donutSegments = computed(() => {
    const total = this.totalJugadores();
    let offset = 25;
    return this.cats.map((c) => {
      const frac = c.val / total;
      const dash = frac * 100 - 1.5;
      const seg = { color: c.color, dasharray: `${dash} ${100 - dash}`, dashoffset: offset };
      offset -= frac * 100;
      return seg;
    });
  });

  protected pct(val: number): number {
    return Math.round((val / this.totalJugadores()) * 100);
  }

  protected barHeight(v: number | null): number {
    return v != null ? (v / this.maxBar) * 100 : 14;
  }

  // ----- Navegacion -----
  protected toggleSidebar(): void { this.sidebarOpen.update((v) => !v); }
  protected closeSidebar(): void { this.sidebarOpen.set(false); }

  protected prevYear(): void { if (this.year() > 2022) this.year.update((y) => y - 1); }
  protected nextYear(): void { if (this.year() < 2026) this.year.update((y) => y + 1); }

  protected logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  // ----- Utilidades -----
  private iniciales(nombre: string): string {
    const parts = nombre.trim().split(/\s+/);
    if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
    return nombre.slice(0, 2).toUpperCase();
  }

  private formatRol(rol: string): string {
    const map: Record<string, string> = {
      ADMINISTRADOR: 'Administrador',
      RECEPCIONISTA: 'Recepcionista',
      ENTRENADOR: 'Entrenador',
      ESTUDIANTE: 'Estudiante',
      USER: 'Usuario',
    };
    return map[rol] ?? rol;
  }
}
