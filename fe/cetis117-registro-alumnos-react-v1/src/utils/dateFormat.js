export function formatDateMask(dateValue) {
  if (!dateValue) {
    return '—'
  }

  const value = String(dateValue).trim()

  if (!value) {
    return '—'
  }

  const normalized = value.includes('T') ? value : `${value}T00:00:00`
  const date = new Date(normalized)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()

  return `${day}/${month}/${year}`
}
