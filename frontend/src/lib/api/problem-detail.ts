export type ProblemDetail = {
  type?: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  code: string;
  timestamp: string;
  errors?: { field: string; message: string }[];
};

export const PROBLEM_MEDIA_TYPE = 'application/problem+json';
