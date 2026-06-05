/** 오늘 날짜 (로컬 기준) YYYY-MM-DD */
export function todayISO() {
  const d = new Date()
  return toISO(d)
}

/** baseISO(YYYY-MM-DD)에 days 더한 날짜 YYYY-MM-DD (로컬 기준) */
export function addDaysISO(baseISO, days) {
  const [y, m, dd] = baseISO.split('-').map(Number)
  const d = new Date(y, m - 1, dd)
  d.setDate(d.getDate() + (days || 0))
  return toISO(d)
}

function toISO(d) {
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

/** 보관위치별 권장 일수 키 매핑 */
export function daysFieldFor(storageType) {
  return { FRIDGE: 'fridgeDays', FREEZER: 'freezerDays', ROOM_TEMP: 'roomTempDays' }[storageType]
}
