// src/app/field-config.ts

export const FIELD_LIST = [
  'Result_snap',
  'Result_context',
  'Result_EPI',
  'Result_HMSBook',
  'Result_JobId',
  'Result_InstrumentId',
  'Result_Errors',
  'Result_PortfolioId',
  'Result_LegalEntity',
  'Result_Desk',
  // Add more fields as needed
];

export const AGGREGATION_FUNCTIONS = [
  'sum',
  'avg',
  'min',
  'max',
  'count',
  'any_value',
];

export const FILTER_OPERATORS = [
  { value: 'equal', viewValue: 'Equal' },
  { value: 'not_equal', viewValue: 'Not Equal' },
  { value: 'in', viewValue: 'In' },
  { value: 'not_in', viewValue: 'Not In' },
  { value: 'less', viewValue: 'Less Than' },
  { value: 'greater', viewValue: 'Greater Than' },
];
