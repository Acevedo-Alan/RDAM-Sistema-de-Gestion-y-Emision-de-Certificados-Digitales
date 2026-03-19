import dayjs from 'dayjs';

export const formatDate = (date) =>
  dayjs(date).format('DD/MM/YYYY');

export const formatDateTime = (date) =>
  dayjs(date).format('DD/MM/YYYY HH:mm');

export const capitalize = (str) =>
  str ? str.charAt(0).toUpperCase() + str.slice(1).toLowerCase() : '';

export const CIRCUNSCRIPCIONES = {
  1: 'I - Santa Fe',
  2: 'II - Rosario',
  3: 'III - Venado Tuerto',
  4: 'IV - Reconquista',
  5: 'V - Rafaela',
};

export const CIRCUNSCRIPCIONES_OPTIONS = Object.entries(CIRCUNSCRIPCIONES).map(
  ([value, label]) => ({ value: Number(value), label })
);

export function formatCircunscripcion(id) {
  if (id == null) return 'No especificada';
  const label = CIRCUNSCRIPCIONES[Number(id)];
  return label ?? 'No especificada';
}
