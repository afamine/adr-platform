import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { WorkspaceMembership } from '../../models/auth.models';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { WorkspaceService } from '../../services/workspace.service';

@Component({
  selector: 'app-workspace-switcher',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './workspace-switcher.component.html',
  styleUrls: ['./workspace-switcher.component.scss']
})
export class WorkspaceSwitcherComponent implements OnInit {
  private readonly workspaceService = inject(WorkspaceService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  workspaces: WorkspaceMembership[] = [];
  selectedWorkspaceId = '';
  isLoading = false;
  isSwitching = false;

  ngOnInit(): void {
    this.loadWorkspaces();
  }

  get shouldShow(): boolean {
    return this.workspaces.length > 1;
  }

  onWorkspaceChange(workspaceId: string): void {
    if (!workspaceId || workspaceId === this.currentWorkspaceId || this.isSwitching) {
      return;
    }

    this.isSwitching = true;
    this.workspaceService.switchWorkspace(workspaceId).subscribe({
      next: (response) => {
        this.authService.saveTokens(response);
        this.notificationService.success('Workspace switched', 'Your workspace context has been updated.');
        window.location.reload();
      },
      error: () => {
        this.isSwitching = false;
        this.selectedWorkspaceId = this.currentWorkspaceId;
        this.notificationService.error('Unable to switch workspace', 'Please try again.');
      }
    });
  }

  private loadWorkspaces(): void {
    this.isLoading = true;
    this.workspaceService.getMyWorkspaces().subscribe({
      next: (workspaces) => {
        this.workspaces = workspaces;
        this.selectedWorkspaceId = this.currentWorkspaceId;
        this.isLoading = false;
      },
      error: () => {
        this.workspaces = [];
        this.isLoading = false;
      }
    });
  }

  private get currentWorkspaceId(): string {
    return this.workspaces.find((workspace) => workspace.current)?.workspaceId ?? this.authService.getCurrentUser()?.workspaceId ?? '';
  }
}
