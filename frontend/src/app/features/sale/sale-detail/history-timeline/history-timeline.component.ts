import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';

import { VehicleHistoryEvent } from '../../../vehicles/models/vehicle-history.model';

@Component({
  selector: 'app-history-timeline',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule, TranslateModule],
  templateUrl: './history-timeline.component.html',
  styleUrl: './history-timeline.component.scss',
})
export class HistoryTimelineComponent {
  @Input() events: VehicleHistoryEvent[] = [];
  @Input() loading = false;
  @Input() error = false;
}
