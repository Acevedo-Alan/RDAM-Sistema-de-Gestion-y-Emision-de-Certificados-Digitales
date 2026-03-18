import dayjs from 'dayjs';

export const formatDate = (date) =>
  dayjs(date).format('DD/MM/YYYY');

export const formatDateTime = (date) =>
  dayjs(date).format('DD/MM/YYYY HH:mm');

export const capitalize = (str) =>
  str ? str.charAt(0).toUpperCase() + str.slice(1).toLowerCase() : '';
