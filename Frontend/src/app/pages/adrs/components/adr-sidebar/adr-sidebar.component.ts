import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ADR_FILTER_OPTIONS, Adr, AdrStatus, AdrStatusFilter } from '../../../../models/adr.model';
import { AdrCardComponent } from '../adr-card/adr-card.component';
import { ProjectDto, ProjectRequest } from '../../../../models/project.model';

@Component({
  selector: 'app-adr-sidebar',
  standalone: true,
  imports: [CommonModule, FormsModule, AdrCardComponent],
  templateUrl: './adr-sidebar.component.html',
  styleUrl: './adr-sidebar.component.scss'
})
export class AdrSidebarComponent {
  @Input() adrs: Adr[] = [];
  @Input() selectedId: string | null = null;
  @Input() searchQuery = '';
  @Input() statusFilter: AdrStatus | 'ALL' = 'ALL';
  @Input() tagFilter = '';
  @Input() canCreate = true;
  @Input() isLoading = false;
  @Input() currentPage = 0;
  @Input() totalPages = 0;
  @Input() totalElements = 0;
  @Input() projects: ProjectDto[] = [];
  @Input() canManageProjects = false;
  @Input() canMoveAdrs = false;

  @Output() adrSelected = new EventEmitter<Adr>();
  @Output() createNew = new EventEmitter<void>();
  @Output() searchChanged = new EventEmitter<string>();
  @Output() filterChanged = new EventEmitter<AdrStatusFilter>();
  @Output() tagFilterChanged = new EventEmitter<string>();
  @Output() previousPage = new EventEmitter<void>();
  @Output() nextPage = new EventEmitter<void>();
  @Output() projectCreated = new EventEmitter<ProjectRequest>();
  @Output() projectUpdated = new EventEmitter<{ id: string; request: ProjectRequest }>();
  @Output() projectArchived = new EventEmitter<string>();
  @Output() adrMoved = new EventEmitter<{ adr: Adr; projectId: string | null }>();

  readonly filterOptions = ADR_FILTER_OPTIONS;
  showProjectForm = false;
  editingProject: ProjectDto | null = null;
  projectName = '';
  projectDescription = '';
  draggedAdr: Adr | null = null;

  get activeProjects(): ProjectDto[] { return this.projects.filter((project) => !project.archived); }
  get unassignedAdrs(): Adr[] { return this.adrs.filter((adr) => !adr.projectId); }
  adrsForProject(projectId: string): Adr[] { return this.adrs.filter((adr) => adr.projectId === projectId); }

  get hasNoResults(): boolean {
    return !this.isLoading
      && this.adrs.length === 0
      && (this.searchQuery.trim() !== '' || this.statusFilter !== 'ALL' || this.tagFilter.trim() !== '');
  }

  get isTrulyEmpty(): boolean {
    return !this.isLoading
      && this.adrs.length === 0
      && this.searchQuery.trim() === ''
      && this.statusFilter === 'ALL'
      && this.tagFilter.trim() === '';
  }

  onSearchInput(query: string): void {
    this.searchChanged.emit(query);
  }

  onTagSelected(tag: string): void {
    this.tagFilterChanged.emit(tag);
  }

  clearFilters(): void {
    this.filterChanged.emit('ALL');
    this.searchChanged.emit('');
    this.tagFilterChanged.emit('');
  }

  openCreateProject(): void {
    this.editingProject = null;
    this.projectName = '';
    this.projectDescription = '';
    this.showProjectForm = true;
  }

  openEditProject(project: ProjectDto): void {
    this.editingProject = project;
    this.projectName = project.name;
    this.projectDescription = project.description ?? '';
    this.showProjectForm = true;
  }

  saveProject(): void {
    const name = this.projectName.trim();
    if (!name) return;
    const request: ProjectRequest = { name, description: this.projectDescription.trim() || null };
    if (this.editingProject) this.projectUpdated.emit({ id: this.editingProject.id, request });
    else this.projectCreated.emit(request);
    this.cancelProjectForm();
  }

  cancelProjectForm(): void {
    this.showProjectForm = false;
    this.editingProject = null;
  }

  onDragStart(event: DragEvent, adr: Adr): void {
    if (!this.canMoveAdrs) { event.preventDefault(); return; }
    this.draggedAdr = adr;
    event.dataTransfer?.setData('text/plain', adr.id);
    if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move';
  }

  onDragEnd(): void { this.draggedAdr = null; }

  onDrop(event: DragEvent, projectId: string | null): void {
    event.preventDefault();
    const adr = this.draggedAdr;
    this.draggedAdr = null;
    if (adr && adr.projectId !== projectId) this.adrMoved.emit({ adr, projectId });
  }
}
