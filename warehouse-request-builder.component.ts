// warehouse-request-builder.component.ts
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { FIELD_LIST, FILTER_OPERATORS } from '../field-config';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  MatCheckboxModule,
  MatInputModule,
  MatButtonModule,
  MatExpansionModule,
  MatSelectModule,
  MatIconModule,
} from '@angular/material';

@Component({
  selector: 'warehouse-request-builder',
  standalone: true,
  templateUrl: './warehouse-request-builder.component.html',
  styleUrls: ['./warehouse-request-builder.component.css'],
  imports: [
    ReactiveFormsModule,
    FormsModule,
    MatCheckboxModule,
    MatInputModule,
    MatButtonModule,
    MatExpansionModule,
    MatSelectModule,
    MatIconModule,
  ],
})
export class WarehouseRequestBuilderComponent implements OnInit {
  warehouseForm: FormGroup;
  fieldList = FIELD_LIST;
  filterOperators = FILTER_OPERATORS;
  generatedJSON: string = '';

  constructor(private fb: FormBuilder, private snackBar: MatSnackBar) {
    this.warehouseForm = this.fb.group({
      official: [false],
      invalidateCache: [false],
      jobIds: [''],
      valueCols: this.fb.array([]),
      aggregation: [''],
      filters: this.fb.array([]),
      pivotGroup: this.fb.group({
        aggColumn: [''],
        pivotColumn: [''],
        pivotValues: ['']
      }),
      distinct: [false],
      exportFormat: ['avro', Validators.required]
    });
  }

  ngOnInit(): void { }

  // Value Columns Methods
  get valueCols(): FormArray {
    return this.warehouseForm.get('valueCols') as FormArray;
  }

  addValueCol() {
    const valueColGroup = this.fb.group({
      field: ['', Validators.required]
    });
    this.valueCols.push(valueColGroup);
  }

  removeValueCol(index: number) {
    this.valueCols.removeAt(index);
  }

  // Filters Methods
  get filters(): FormArray {
    return this.warehouseForm.get('filters') as FormArray;
  }

  addFilter() {
    const filterGroup = this.fb.group({
      field: ['', Validators.required],
      filterType: ['', Validators.required],
      value: ['', Validators.required]
    });
    this.filters.push(filterGroup);
  }

  removeFilter(index: number) {
    this.filters.removeAt(index);
  }

  // Pivot Group Getter
  get pivotGroup(): FormGroup {
    return this.warehouseForm.get('pivotGroup') as FormGroup;
  }

  // Generate JSON
  generateJSON() {
    if (this.warehouseForm.invalid) {
      this.snackBar.open('Please fill in all required fields.', 'Close', { duration: 3000 });
      return;
    }

    const formValue = this.warehouseForm.value;

    const warehouseRequest: any = {
      official: formValue.official,
      invalidateCache: formValue.invalidateCache,
      jobIds: formValue.jobIds ? formValue.jobIds.split(',').map((s: string) => s.trim()) : [],
      valueCols: formValue.valueCols.map((vc: any) => ({ field: vc.field })),
      aggregation: formValue.aggregation ? formValue.aggregation.split(',').map((s: string) => s.trim()) : [],
      filterModel: {},
      pivot: {},
      distinct: formValue.distinct,
      exportFormat: formValue.exportFormat
    };

    // Filters
    formValue.filters.forEach((filter: any) => {
      warehouseRequest.filterModel[filter.field] = {
        filterType: filter.filterType,
        value: filter.value
      };
    });

    // Pivot
    const pivot = formValue.pivotGroup;
    if (pivot.aggColumn || pivot.pivotColumn || pivot.pivotValues) {
      warehouseRequest.pivot = {};
      if (pivot.aggColumn) warehouseRequest.pivot.aggColumn = pivot.aggColumn;
      if (pivot.pivotColumn) warehouseRequest.pivot.pivotColumn = pivot.pivotColumn;
      if (pivot.pivotValues) {
        warehouseRequest.pivot.pivotValues = pivot.pivotValues.split(',').map((s: string) => s.trim());
      }
    }

    this.generatedJSON = JSON.stringify(warehouseRequest, null, 2);
  }

  // Clear Form
  clearForm() {
    this.warehouseForm.reset({ official: false, invalidateCache: false, distinct: false, exportFormat: 'avro' });
    this.valueCols.clear();
    this.filters.clear();
    this.generatedJSON = '';
  }

  // Copy JSON to Clipboard
  copyJSON() {
    navigator.clipboard.writeText(this.generatedJSON).then(() => {
      this.snackBar.open('JSON copied to clipboard!', 'Close', { duration: 3000 });
    });
  }
}
