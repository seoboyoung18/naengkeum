import { useState, useEffect, useCallback } from 'react'
import { View, Text, Pressable, ScrollView, StyleSheet, Alert, ActivityIndicator } from 'react-native'
import { colors, radius, cardShadow } from '../theme'
import * as admin from '../api/admin'

const TABS = ['대시보드', '회원', '레시피', '리뷰', '신고']

export default function AdminScreen() {
  const [tab, setTab] = useState('대시보드')
  const [stats, setStats] = useState(null)
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async (t) => {
    setLoading(true)
    try {
      if (t === '대시보드') setStats(await admin.getStats())
      else if (t === '회원') setRows(await admin.getUsers())
      else if (t === '레시피') setRows(await admin.getRecipes())
      else if (t === '리뷰') setRows(await admin.getReviews())
      else if (t === '신고') setRows(await admin.getReports())
    } catch (e) {
      Alert.alert('오류', e.response?.data?.message || '불러오기 실패')
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { load('대시보드') }, [load])
  function pick(t) { setTab(t); if (t !== '대시보드') setRows([]); load(t) }

  function confirm(msg, fn) {
    Alert.alert('확인', msg, [{ text: '취소', style: 'cancel' }, { text: '확인', style: 'destructive', onPress: fn }])
  }
  const act = async (fn, after) => { try { await fn(); after?.() } catch (e) { Alert.alert('실패', e.response?.data?.message || '처리 실패') } }

  return (
    <View style={styles.wrap}>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.tabsScroll} contentContainerStyle={styles.tabs}>
        {TABS.map((t) => (
          <Pressable key={t} style={[styles.tab, tab === t && styles.tabOn]} onPress={() => pick(t)}>
            <Text style={[styles.tabT, tab === t && styles.tabTOn]}>{t}</Text>
          </Pressable>
        ))}
      </ScrollView>

      {loading ? <ActivityIndicator color={colors.primary} style={{ marginTop: 30 }} /> : (
        <ScrollView contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
          {tab === '대시보드' && stats && (
            <View style={styles.cards}>
              {[['전체 회원', stats.totalMembers], ['레시피', stats.totalRecipes], ['리뷰', stats.totalReviews], ['챌린지 참여', stats.activeChallengeParticipants], ['미처리 신고', stats.pendingReports, true]].map(([l, v, danger]) => (
                <View key={l} style={[styles.card, danger && v > 0 && styles.cardDanger]}>
                  <Text style={[styles.cardN, danger && v > 0 && { color: colors.danger }]}>{v ?? 0}</Text>
                  <Text style={styles.cardL}>{l}</Text>
                </View>
              ))}
            </View>
          )}

          {tab === '회원' && rows.map((u) => (
            <View key={u.memberId} style={styles.row}>
              <View style={{ flex: 1 }}>
                <Text style={styles.rTitle}>{u.nickname} <View style={[styles.roleBadge, u.role === 'ADMIN' && styles.roleAdmin]}><Text style={[styles.roleT, u.role === 'ADMIN' && { color: '#fff' }]}>{u.role}</Text></View></Text>
                <Text style={styles.rSub}>{u.email} · {(u.createdAt || '').slice(0, 10)}</Text>
              </View>
              <Pressable style={styles.del} onPress={() => confirm(`'${u.nickname}' 회원을 삭제할까요?`, () => act(() => admin.deleteUser(u.memberId), () => load('회원')))}><Text style={styles.delT}>삭제</Text></Pressable>
            </View>
          ))}

          {tab === '레시피' && rows.map((r) => (
            <View key={r.recipeId} style={styles.row}>
              <View style={{ flex: 1 }}>
                <Text style={styles.rTitle} numberOfLines={1}>{r.title}</Text>
                <Text style={styles.rSub}>{r.authorNickname || r.author} · {r.isPublic ? '공개' : '비공개'}{r.reportCount ? ` · 신고 ${r.reportCount}` : ''}</Text>
              </View>
              <Pressable style={styles.del} onPress={() => confirm('레시피를 삭제할까요?', () => act(() => admin.deleteAdminRecipe(r.recipeId), () => load('레시피')))}><Text style={styles.delT}>삭제</Text></Pressable>
            </View>
          ))}

          {tab === '리뷰' && rows.map((r) => (
            <View key={r.reviewId} style={styles.row}>
              <View style={{ flex: 1 }}>
                <Text style={styles.rTitle} numberOfLines={1}>{r.recipeTitle} · ★{r.rating}</Text>
                <Text style={styles.rSub} numberOfLines={1}>{r.nickname} · {r.content}{r.reportCount ? ` · 신고 ${r.reportCount}` : ''}</Text>
              </View>
              <Pressable style={styles.del} onPress={() => confirm('리뷰를 삭제할까요?', () => act(() => admin.deleteAdminReview(r.reviewId), () => load('리뷰')))}><Text style={styles.delT}>삭제</Text></Pressable>
            </View>
          ))}

          {tab === '신고' && rows.map((r, i) => (
            <View key={i} style={styles.row}>
              <View style={{ flex: 1 }}>
                <Text style={styles.rTitle} numberOfLines={1}><Text style={styles.typeBadge}>{r.targetType === 'REVIEW' ? '리뷰' : '레시피'}</Text> {r.title || r.content}</Text>
                <Text style={styles.rSub}>{r.reason || '사유 없음'} · 신고 {r.reportCount}</Text>
              </View>
              <View style={styles.reportActions}>
                <Pressable style={styles.del} onPress={() => confirm('대상을 삭제할까요?', () => act(() => (r.targetType === 'REVIEW' ? admin.deleteAdminReview(r.targetId) : admin.deleteAdminRecipe(r.targetId)), () => load('신고')))}><Text style={styles.delT}>삭제</Text></Pressable>
                <Pressable style={styles.ignore} onPress={() => act(() => (r.targetType === 'REVIEW' ? admin.resolveReviewReports(r.targetId) : admin.resolveRecipeReports(r.targetId)), () => load('신고'))}><Text style={styles.ignoreT}>무시</Text></Pressable>
              </View>
            </View>
          ))}

          {tab !== '대시보드' && rows.length === 0 && <Text style={styles.empty}>항목이 없습니다.</Text>}
        </ScrollView>
      )}
    </View>
  )
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: colors.surfaceBg },
  tabsScroll: { flexGrow: 0, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: colors.lineSoft },
  tabs: { paddingHorizontal: 12, paddingVertical: 10, gap: 6 },
  tab: { paddingHorizontal: 14, paddingVertical: 7, borderRadius: radius.pill, backgroundColor: colors.surfaceBg },
  tabOn: { backgroundColor: colors.primaryTint },
  tabT: { fontSize: 13, color: '#666' },
  tabTOn: { color: colors.primaryDeep, fontWeight: '700' },
  cards: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  card: { width: '47.5%', flexGrow: 1, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 14, padding: 18, ...cardShadow },
  cardDanger: { borderColor: '#fecaca', backgroundColor: '#fef2f2' },
  cardN: { fontSize: 26, fontWeight: '800', color: colors.primaryDeep },
  cardL: { fontSize: 12, color: '#777', marginTop: 4 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 10, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 12, padding: 12, marginBottom: 8, ...cardShadow },
  rTitle: { fontSize: 14, fontWeight: '600', color: colors.text },
  rSub: { fontSize: 12, color: '#999', marginTop: 3 },
  roleBadge: { backgroundColor: colors.primaryTint, borderRadius: radius.pill, paddingHorizontal: 7, paddingVertical: 1 },
  roleAdmin: { backgroundColor: colors.canvas },
  roleT: { fontSize: 10, fontWeight: '700', color: colors.primaryDeep },
  typeBadge: { fontSize: 11, fontWeight: '700', color: colors.primaryDeep },
  del: { borderWidth: 1, borderColor: '#fecdd3', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 7 },
  delT: { color: '#e11d48', fontSize: 13, fontWeight: '600' },
  reportActions: { flexDirection: 'row', gap: 6 },
  ignore: { borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingHorizontal: 12, paddingVertical: 7 },
  ignoreT: { color: '#888', fontSize: 13 },
  empty: { textAlign: 'center', color: '#999', marginTop: 30 },
})
