import { computed, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AppTopbarComponent } from '../../components/app-topbar/app-topbar.component';
import { EditorHeaderComponent } from '../../components/editor-header/editor-header.component';
import { SidebarComponent } from '../../components/sidebar/sidebar.component';
import { ADR_STATUS_OPTIONS, ADR_TAB_ORDER, AdrStatus } from '../../models/adr.model';
import { AdrService } from '../../services/adr.service';
import { TabNavComponent } from '../../shared/tab-nav/tab-nav.component';

@Component({
  selector: 'app-adr-editor-page',
  imports: [AppTopbarComponent, EditorHeaderComponent, FormsModule, SidebarComponent, TabNavComponent],
  templateUrl: './adr-editor-page.component.html',
  styleUrl: './adr-editor-page.component.css'
})
export class AdrEditorPageComponent {
  protected readonly adrService = inject(AdrService);
  protected readonly statusOptions = ADR_STATUS_OPTIONS;
  protected readonly tabs = ADR_TAB_ORDER;
  protected readonly selectedAdr = this.adrService.selectedAdr;
  protected readonly activeTab = this.adrService.activeTab;
  protected readonly filteredAdrs = this.adrService.filteredAdrs;
  protected readonly searchQuery = this.adrService.searchQuery;
  protected readonly statusFilter = this.adrService.statusFilter;
  protected readonly previewMode = this.adrService.previewMode;
  protected readonly collaborateMode = this.adrService.collaborateMode;
  protected readonly aiAssistMode = this.adrService.aiAssistMode;

  protected readonly activeSection = computed(() => {
    const adr = this.selectedAdr();
    const tab = this.activeTab();

    return adr ? adr.sections[tab] : null;
  });

  protected updateStatus(status: string): void {
    this.adrService.updateStatus(status as AdrStatus);
  }

  protected updateActiveSection(content: string): void {
    this.adrService.updateSectionContent(this.activeTab(), content);
  }
}
