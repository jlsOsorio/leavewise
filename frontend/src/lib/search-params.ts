type SearchParam = string | string[] | undefined;

function first(value: SearchParam): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

export function pageParam(value: SearchParam): number {
  const page = Number(first(value));
  return Number.isInteger(page) && page >= 0 ? page : 0;
}

export function textParam(value: SearchParam, maxLength: number): string {
  return (first(value) ?? '').trim().slice(0, maxLength);
}
