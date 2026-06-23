import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../services/authservice';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {

  form: FormGroup;

  loading = signal(false);
  errorMessage = signal('');
  showPassword = signal(false);

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {

    this.form = this.fb.group({
      username: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(6)]],
      remember: [false]
    });

  }

  submit(): void {

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.login({
      username: this.form.value.username,
      password: this.form.value.password
    })
    .subscribe({
      next: (response) => {

        this.authService.saveToken(response.token);

        localStorage.setItem('username', response.username);
        localStorage.setItem('nombre', response.nombre);
        localStorage.setItem('rol', response.rol);

        this.loading.set(false);

        this.router.navigate(['/dashboard']);
      },

      error: () => {

        this.loading.set(false);

        this.errorMessage.set(
          'Usuario o contraseña incorrectos'
        );
      }
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(v => !v);
  }

  fieldHasError(field: string): boolean {

    const control = this.form.get(field);

    return !!(
      control &&
      control.invalid &&
      (control.touched || control.dirty)
    );
  }

  matchClockLabel(): string {

    return new Date().toLocaleTimeString(
      'es-EC',
      {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      }
    );
  }
}