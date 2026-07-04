import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Auth } from '../../services/auth';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnInit, OnDestroy {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly showPassword = signal(false);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    remember: [false],
  });

  private readonly matchSeconds = signal(5400);
  private clockHandle?: ReturnType<typeof setInterval>;

  protected readonly matchClockLabel = computed(() => {
    const total = this.matchSeconds();
    const minutes = Math.floor(total / 60);
    const seconds = total % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  });

  ngOnInit(): void {
    this.clockHandle = setInterval(() => this.matchSeconds.update((s) => s + 1), 1000);
  }

  ngOnDestroy(): void {
    if (this.clockHandle) {
      clearInterval(this.clockHandle);
    }
  }

  protected togglePasswordVisibility(): void {
    this.showPassword.update((value) => !value);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage.set(null);
    this.loading.set(true);
    const { username, password } = this.form.getRawValue();
    this.auth.login(username, password).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.redirigirPorRol(res.rol);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Usuario o contrasena incorrectos. Verifica tus datos e intentalo de nuevo.');
      },
    });
  }

  private redirigirPorRol(rol: string): void {
    switch (rol) {
      case 'ADMINISTRADOR':
      case 'RECEPCIONISTA':
        this.router.navigate(['/admin']);
        break;
      case 'ENTRENADOR':
        this.router.navigate(['/entrenador']);
        break;
      case 'ESTUDIANTE':
        this.router.navigate(['/estudiante']);
        break;
      default:
        this.router.navigate(['/dashboard']);
    }
  }

  protected fieldHasError(field: 'username' | 'password'): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && (control.touched || control.dirty);
  }
}