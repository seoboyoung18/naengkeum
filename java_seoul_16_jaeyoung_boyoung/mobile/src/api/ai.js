import { API_BASE } from '../config'
import { getToken } from '../lib/token'

/**
 * AI 레시피 추천 — POST /api/ai/recommend (백엔드는 SSE 스트림).
 * RN fetch는 증분 스트리밍을 지원하지 않으므로, 연결 종료 후 전체 본문(res.text())을
 * 받아 SSE 이벤트를 한 번에 파싱한다. (웹은 점진 렌더, 모바일은 완료 후 일괄 렌더)
 *
 * @param {{prioritizeExpiry?:boolean, useAllFridge?:boolean, applyAllergy?:boolean}} options
 * @returns {Promise<{source, title, summary, ingredients, steps, meta, error}>}
 */
export async function recommendAi(options) {
  const res = await fetch(`${API_BASE}/api/ai/recommend`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${getToken()}`,
      'Content-Type': 'application/json',
      Accept: 'text/event-stream, application/json',
    },
    body: JSON.stringify(options),
  })

  if (!res.ok) {
    let msg = `요청 실패 (HTTP ${res.status})`
    try { const j = await res.json(); msg = j.message || msg } catch (_) {}
    const err = new Error(msg)
    err.status = res.status
    throw err
  }

  const text = await res.text()
  const result = { source: null, title: '', summary: '', ingredients: [], steps: [], meta: null, error: null }
  for (const block of text.split('\n\n')) {
    const line = block.split('\n').find((l) => l.startsWith('data:'))
    if (!line) continue
    let ev
    try { ev = JSON.parse(line.slice(5).trim()) } catch (_) { continue }
    switch (ev.type) {
      case 'source': result.source = ev.value; break
      case 'title': result.title = ev.value; break
      case 'summary': result.summary = ev.value; break
      case 'ingredient': result.ingredients.push(ev.value); break
      case 'step': result.steps.push(ev.value); break
      case 'meta': result.meta = ev.value; break
      case 'error': result.error = ev.value || 'AI 처리 중 오류'; break
      default: break
    }
  }
  return result
}
