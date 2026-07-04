import { useState, useEffect, useCallback } from 'react'
import { View, Text, Pressable, StyleSheet, Alert, ActivityIndicator } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import { Ionicons } from '@expo/vector-icons'
import { listMyRecipes, publishRecipe, unpublishRecipe, deleteRecipe } from '../api/recipe'
import { colors, radius } from '../theme'

function fmtQty(n) {
  const num = Number(n)
  return Number.isNaN(num) ? n : String(Number(num.toFixed(2)))
}

export default function MyRecipeList() {
  const navigation = useNavigation()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try { setItems(await listMyRecipes()) } finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const openDetail = (r) => navigation.navigate('RecipeDetail', { recipeId: r.recipeId })

  async function publish(r) {
    if (busy) return
    if (!r.imageUrl) {
      Alert.alert('사진 필요', '대표 사진을 먼저 등록해야 공개할 수 있어요', [
        { text: '취소', style: 'cancel' },
        { text: '상세로 이동', onPress: () => openDetail(r) },
      ])
      return
    }
    setBusy(r.recipeId)
    try {
      const res = await publishRecipe(r.recipeId)
      setItems((list) => list.map((x) => (x.recipeId === r.recipeId ? { ...x, isPublic: true } : x)))
      const consumed = res?.consumed || []
      const msg = consumed.length
        ? `공개했어요 · 냉장고에서 ${consumed.map((c) => `${c.name} ${fmtQty(c.used)}${c.unit}`).join(', ')} 차감`
        : '공개했어요. 이제 모두가 검색·찜할 수 있어요'
      Alert.alert('완료', msg)
    } catch (e) {
      Alert.alert('실패', e.response?.data?.message || '공개에 실패했어요')
    } finally { setBusy(null) }
  }

  async function unpublish(r) {
    if (busy) return
    setBusy(r.recipeId)
    try {
      await unpublishRecipe(r.recipeId)
      setItems((list) => list.map((x) => (x.recipeId === r.recipeId ? { ...x, isPublic: false } : x)))
    } catch (e) { Alert.alert('실패', e.response?.data?.message || '비공개 전환 실패') }
    finally { setBusy(null) }
  }

  function removeRecipe(r) {
    if (busy) return
    Alert.alert('삭제', `'${r.title}' 레시피를 삭제할까요? 되돌릴 수 없어요.`, [
      { text: '취소', style: 'cancel' },
      { text: '삭제', style: 'destructive', onPress: async () => {
        setBusy(r.recipeId)
        try { await deleteRecipe(r.recipeId); setItems((list) => list.filter((x) => x.recipeId !== r.recipeId)) }
        catch (e) { Alert.alert('실패', e.response?.data?.message || '삭제에 실패했어요') }
        finally { setBusy(null) }
      } },
    ])
  }

  if (loading) return <ActivityIndicator color={colors.primary} style={{ marginVertical: 16 }} />
  if (items.length === 0) return <Text style={styles.empty}>아직 담은 레시피가 없어요. AI 추천에서 "내 레시피로 담기"를 눌러보세요.</Text>

  return (
    <View style={{ gap: 8 }}>
      {items.map((r) => (
        <View key={r.recipeId} style={styles.row}>
          <Pressable style={styles.left} onPress={() => openDetail(r)}>
            <View style={styles.badge}><Text style={styles.badgeT}>AI</Text></View>
            <View style={{ flex: 1, minWidth: 0 }}>
              <Text style={styles.title} numberOfLines={1}>{r.title}</Text>
              <View style={styles.sub}>
                {!!r.cookTime && <Text style={styles.subT}>{r.cookTime}분</Text>}
                {!r.imageUrl && <Text style={styles.nophoto}>📷 사진 없음</Text>}
              </View>
            </View>
          </Pressable>
          <View style={styles.right}>
            {r.isPublic ? (
              <>
                <View style={[styles.state, styles.statePub]}><Text style={styles.statePubT}>공개됨</Text></View>
                <Pressable style={styles.ghost} disabled={busy === r.recipeId} onPress={() => unpublish(r)}><Text style={styles.ghostT}>{busy === r.recipeId ? '…' : '비공개로'}</Text></Pressable>
              </>
            ) : (
              <>
                <View style={[styles.state, styles.statePriv]}><Text style={styles.statePrivT}>비공개</Text></View>
                <Pressable style={styles.pub} disabled={busy === r.recipeId} onPress={() => publish(r)}><Text style={styles.pubT}>{busy === r.recipeId ? '…' : (r.imageUrl ? '공개하기' : '사진 등록')}</Text></Pressable>
              </>
            )}
            <Pressable hitSlop={6} disabled={busy === r.recipeId} onPress={() => removeRecipe(r)} style={{ padding: 4 }}>
              <Ionicons name="trash-outline" size={18} color="#8b95a1" />
            </Pressable>
          </View>
        </View>
      ))}
    </View>
  )
}

const styles = StyleSheet.create({
  empty: { fontSize: 14, color: '#999', paddingVertical: 12 },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 12, padding: 12 },
  left: { flexDirection: 'row', alignItems: 'center', gap: 10, flex: 1, minWidth: 0 },
  badge: { backgroundColor: '#F5F3FF', borderRadius: radius.pill, paddingHorizontal: 10, paddingVertical: 4 },
  badgeT: { color: '#7C3AED', fontSize: 12, fontWeight: '700' },
  title: { fontSize: 15, fontWeight: '600', color: colors.text },
  sub: { flexDirection: 'row', gap: 10, marginTop: 2 },
  subT: { fontSize: 12, color: '#999' },
  nophoto: { fontSize: 12, color: colors.warn, fontWeight: '600' },
  right: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  state: { borderRadius: radius.pill, paddingHorizontal: 10, paddingVertical: 4 },
  statePub: { backgroundColor: colors.primaryTint },
  statePubT: { color: colors.primaryDeep, fontSize: 12, fontWeight: '700' },
  statePriv: { backgroundColor: '#f3f4f6' },
  statePrivT: { color: '#6b7280', fontSize: 12, fontWeight: '700' },
  pub: { backgroundColor: colors.primary, borderRadius: radius.pill, paddingHorizontal: 14, paddingVertical: 8 },
  pubT: { color: colors.onPrimary, fontSize: 13, fontWeight: '700' },
  ghost: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.pill, paddingHorizontal: 12, paddingVertical: 8 },
  ghostT: { color: '#555', fontSize: 13, fontWeight: '600' },
})
