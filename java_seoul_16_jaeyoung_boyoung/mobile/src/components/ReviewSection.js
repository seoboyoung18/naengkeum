import { useState, useEffect, useCallback } from 'react'
import { View, Text, TextInput, Pressable, StyleSheet, Alert, ActivityIndicator } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import { listReviews, createReview, updateReview, deleteReview } from '../api/review'
import ReportButton from './ReportButton'
import { colors, radius } from '../theme'

function Stars({ value, onChange, size = 24 }) {
  return (
    <View style={{ flexDirection: 'row' }}>
      {[1, 2, 3, 4, 5].map((n) => (
        <Pressable key={n} onPress={() => onChange?.(n)} disabled={!onChange} hitSlop={4}>
          <Text style={{ fontSize: size, color: n <= value ? '#f59e0b' : '#ddd' }}>★</Text>
        </Pressable>
      ))}
    </View>
  )
}

export default function ReviewSection({ recipeId, onChanged }) {
  const navigation = useNavigation()
  const [reviews, setReviews] = useState([])
  const [stats, setStats] = useState({ avg: 0, dist: {} })
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)

  const [rating, setRating] = useState(5)
  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [writeErr, setWriteErr] = useState('')

  const [editingId, setEditingId] = useState(null)
  const [editRating, setEditRating] = useState(5)
  const [editContent, setEditContent] = useState('')

  const myReview = reviews.find((r) => r.isOwner) || null

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listReviews(recipeId, { page: 0, size: 50 })
      setReviews(data.content)
      setStats(data.ratingStats || { avg: 0, dist: {} })
      setTotal(data.totalElements)
    } finally {
      setLoading(false)
    }
  }, [recipeId])

  useEffect(() => { load() }, [load])

  async function submit() {
    setWriteErr('')
    if (!content.trim()) { setWriteErr('내용을 입력해 주세요'); return }
    setSubmitting(true)
    try {
      await createReview({ recipeId: Number(recipeId), rating, content: content.trim() })
      setContent(''); setRating(5)
      await load(); onChanged?.()
    } catch (e) {
      setWriteErr(e.response?.status === 409 ? '이미 이 레시피에 리뷰를 작성하셨습니다' : (e.response?.data?.message || '리뷰 작성 실패'))
    } finally {
      setSubmitting(false)
    }
  }

  async function saveEdit(r) {
    try {
      await updateReview(r.reviewId, { rating: editRating, content: editContent.trim() })
      setEditingId(null); await load(); onChanged?.()
    } catch (e) { Alert.alert('실패', e.response?.data?.message || '수정에 실패했어요') }
  }

  function remove(r) {
    Alert.alert('삭제', '리뷰를 삭제할까요?', [
      { text: '취소', style: 'cancel' },
      { text: '삭제', style: 'destructive', onPress: async () => {
        try { await deleteReview(r.reviewId); await load(); onChanged?.() }
        catch (e) { Alert.alert('실패', e.response?.data?.message || '삭제에 실패했어요') }
      } },
    ])
  }

  const distPct = (star) => (total ? Math.round(((stats.dist?.[star] || 0) / total) * 100) : 0)

  return (
    <View>
      <Text style={styles.sec}>리뷰 <Text style={styles.cnt}>{total}</Text></Text>

      {total > 0 && (
        <View style={styles.stats}>
          <View style={styles.avgBox}>
            <Text style={styles.avgBig}>{Number(stats.avg).toFixed(1)}</Text>
            <Stars value={Math.round(stats.avg)} size={13} />
          </View>
          <View style={{ flex: 1 }}>
            {[5, 4, 3, 2, 1].map((s) => (
              <View key={s} style={styles.barRow}>
                <Text style={styles.barLbl}>{s}</Text>
                <View style={styles.bar}><View style={[styles.fill, { width: `${distPct(s)}%` }]} /></View>
              </View>
            ))}
          </View>
        </View>
      )}

      {!myReview ? (
        <View style={styles.write}>
          <Stars value={rating} onChange={setRating} />
          <TextInput style={styles.ta} value={content} onChangeText={setContent} multiline placeholder="이 레시피 어땠나요? 꿀팁도 환영!" placeholderTextColor="#9AA0A6" />
          {!!writeErr && <Text style={styles.err}>{writeErr}</Text>}
          <Pressable style={styles.submit} disabled={submitting} onPress={submit}><Text style={styles.submitT}>{submitting ? '등록 중…' : '리뷰 등록'}</Text></Pressable>
        </View>
      ) : (
        <Text style={styles.mineNote}>✓ 내가 작성한 리뷰는 아래에서 수정/삭제할 수 있어요.</Text>
      )}

      {loading ? (
        <ActivityIndicator color={colors.primary} style={{ marginVertical: 16 }} />
      ) : total === 0 ? (
        <Text style={styles.muted}>첫 리뷰를 남겨보세요!</Text>
      ) : (
        reviews.map((r) => (
          <View key={r.reviewId} style={[styles.item, r.isOwner && styles.itemOwner]}>
            <View style={styles.itop}>
              <Pressable disabled={r.isOwner} onPress={() => navigation.navigate('UserProfile', { userId: r.memberId })}>
                <Text style={[styles.nick, !r.isOwner && styles.nickLink]}>{r.nickname}{r.isOwner ? '  (나)' : ''}</Text>
              </Pressable>
              <Text style={styles.date}>{(r.createdAt || '').slice(0, 10)}</Text>
            </View>
            {editingId === r.reviewId ? (
              <>
                <Stars value={editRating} onChange={setEditRating} />
                <TextInput style={styles.ta} value={editContent} onChangeText={setEditContent} multiline />
                <View style={styles.editActions}>
                  <Pressable style={styles.ghost} onPress={() => setEditingId(null)}><Text style={styles.ghostT}>취소</Text></Pressable>
                  <Pressable style={styles.submitSm} onPress={() => saveEdit(r)}><Text style={styles.submitT}>저장</Text></Pressable>
                </View>
              </>
            ) : (
              <>
                <Stars value={r.rating} size={13} />
                <Text style={styles.content}>{r.content}</Text>
                <View style={styles.actions}>
                  {r.isOwner ? (
                    <>
                      <Pressable onPress={() => { setEditingId(r.reviewId); setEditRating(r.rating); setEditContent(r.content) }}><Text style={styles.actT}>수정</Text></Pressable>
                      <Pressable onPress={() => remove(r)}><Text style={styles.actT}>삭제</Text></Pressable>
                    </>
                  ) : (
                    <ReportButton targetType="REVIEW" targetId={r.reviewId} compact />
                  )}
                </View>
              </>
            )}
          </View>
        ))
      )}
    </View>
  )
}

const styles = StyleSheet.create({
  sec: { fontSize: 16, fontWeight: '700', marginBottom: 12, color: colors.text },
  cnt: { color: colors.primaryDeep, fontSize: 14 },
  stats: { flexDirection: 'row', gap: 16, alignItems: 'center', backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 12, padding: 14, marginBottom: 14 },
  avgBox: { alignItems: 'center', width: 80 },
  avgBig: { fontSize: 28, fontWeight: '800', color: colors.primaryDeep },
  barRow: { flexDirection: 'row', alignItems: 'center', gap: 8, marginVertical: 3 },
  barLbl: { fontSize: 11, color: '#999', width: 10 },
  bar: { flex: 1, height: 6, backgroundColor: '#f0f0f0', borderRadius: 3, overflow: 'hidden' },
  fill: { height: '100%', backgroundColor: '#fbbf24' },
  write: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 12, padding: 12, marginBottom: 14 },
  ta: { borderWidth: 1, borderColor: colors.line, borderRadius: 8, padding: 10, fontSize: 14, marginTop: 8, minHeight: 60, textAlignVertical: 'top' },
  err: { color: '#e11d48', fontSize: 13, marginTop: 8 },
  submit: { marginTop: 10, backgroundColor: colors.primary, borderRadius: radius.sm, paddingVertical: 10, alignItems: 'center', alignSelf: 'flex-start', paddingHorizontal: 16 },
  submitSm: { backgroundColor: colors.primary, borderRadius: radius.sm, paddingVertical: 8, paddingHorizontal: 14 },
  submitT: { color: colors.onPrimary, fontWeight: '700', fontSize: 14 },
  mineNote: { fontSize: 13, color: colors.primaryDeep, marginBottom: 12 },
  muted: { color: '#999' },
  item: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 12, padding: 12, marginBottom: 10 },
  itemOwner: { borderColor: '#bbf7d0', backgroundColor: '#f6fef9' },
  itop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  nick: { fontSize: 13, fontWeight: '600', color: colors.text },
  nickLink: { color: colors.primaryDeep },
  date: { fontSize: 11, color: '#aaa' },
  content: { fontSize: 14, marginTop: 4 },
  actions: { flexDirection: 'row', gap: 14, marginTop: 8, alignItems: 'center' },
  actT: { color: '#888', fontSize: 12 },
  editActions: { flexDirection: 'row', gap: 8, justifyContent: 'flex-end', marginTop: 8 },
  ghost: { borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingHorizontal: 14, paddingVertical: 8 },
  ghostT: { fontSize: 13, color: colors.text },
})
