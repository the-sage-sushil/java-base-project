import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { ProfileResponse } from '../../core/auth/auth.models';

@Component({
  standalone: true,
  imports: [RouterLink],
  templateUrl: './profile.html' ,
  styleUrls: ["./profile.scss"]
})
export class ProfileComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly response = signal<ProfileResponse | null>(null);

  ngOnInit(): void {
    this.auth.profile().subscribe({
      next: response => this.response.set(response),
      error: error => console.error('Failed to load profile:', error)
    });
  }
}