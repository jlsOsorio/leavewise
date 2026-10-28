const NIF_PATTERN = /^\d{9}$/;
const MODULUS = 11;

// Portuguese NIF (BR-3), mirroring the backend's NifValidator: 9 digits, the first 8 weighted
// 9 down to 2; a remainder of 0 or 1 gives check digit 0, else 11 - remainder
export function isValidNif(nif: string): boolean {
  if (!NIF_PATTERN.test(nif)) {
    return false;
  }

  const digits = [...nif].map(Number);
  const weightedSum = digits
    .slice(0, 8)
    .reduce((sum, digit, index) => sum + digit * (9 - index), 0);
  const remainder = weightedSum % MODULUS;
  const expectedCheckDigit = remainder < 2 ? 0 : MODULUS - remainder;
  return digits[8] === expectedCheckDigit;
}
