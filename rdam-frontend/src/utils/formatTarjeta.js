/**
 * Formatea un número de tarjeta mock para mostrar franquicia + últimos 4 dígitos.
 * '4111222233334444' → 'Visa •••• 4444'
 * '5111222233334444' → 'Mastercard •••• 4444'
 * null o undefined   → '—'
 */
export function formatTarjetaMock(numeroTarjeta) {
  if (!numeroTarjeta) return '—';

  const digits = String(numeroTarjeta).replace(/\D/g, '');
  if (digits.length < 4) return '—';

  const last4 = digits.slice(-4);
  const prefix = digits.charAt(0);

  let brand = 'Tarjeta';
  if (prefix === '4') brand = 'Visa';
  else if (prefix === '5') brand = 'Mastercard';

  return `${brand} •••• ${last4}`;
}
