import { useState, useEffect, useCallback, useRef } from 'react'
import { View, Text, TextInput, Pressable, FlatList, StyleSheet, Alert, ActivityIndicator } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { searchRecipes } from '../api/recipe'
import { addRecipeWish, removeRecipeWish } from '../api/wishlist'
import { listFridge } from '../api/fridge'
import RecipeCard from '../components/RecipeCard'
import { colors, radius } from '../theme'

const SORTS = [
  { key: 'LATEST', label: '최신' },
  { key: 'POPULAR', label: '인기' },
  { key: 'RATING', label: '평점' },
  { key: 'COOK_TIME', label: '조리시간' },
]
const COOK_TIMES = [
  { min: null, max: null, label: '시간 전체' },
  { min: null, max: 10, label: '10분' },
  { min: 11, max: 20, label: '20분' },
  { min: 21, max: 30, label: '30분' },
]
const SIZE = 12

export default function RecipeListScreen({ route, navigation }) {
  const [keyword, setKeyword] = useState('')
  const [sort, setSort] = useState('LATEST')
  const [cook, setCook] = useState({ min: null, max: null })
  const [useMyFridge, setUseMyFridge] = useState(false)
  const [mine, setMine] = useState(false)

  const [content, setContent] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [err, setErr] = useState('')
  const fridgeNames = useRef('')

  async function ensureFridgeNames() {
    if (fridgeNames.current) return fridgeNames.current
    try {
      const d = await listFridge({ storageType: 'ALL', sort: 'EXPIRY_ASC' })
      fridgeNames.current = d.items.map((i) => i.name).join(',')
    } catch (_) { fridgeNames.current = '' }
    return fridgeNames.current
  }

  const load = useCallback(async (reset, opts = {}) => {
    const st = { keyword, sort, cook, useMyFridge, mine, ...opts }
    if (reset) setLoading(true)
    else setLoadingMore(true)
    setErr('')
    const nextPage = reset ? 0 : page + 1
    try {
      const params = { sort: st.sort, page: nextPage, size: SIZE }
      if (st.keyword.trim()) params.keyword = st.keyword.trim()
      if (st.cook.min) params.minCookTime = st.cook.min
      if (st.cook.max) params.maxCookTime = st.cook.max
      if (st.mine) params.mine = true
      if (st.useMyFridge) {
        const names = await ensureFridgeNames()
        if (names) params.ingredients = names
      }
      const data = await searchRecipes(params)
      setContent((prev) => (reset ? data.content : [...prev, ...data.content]))
      setPage(nextPage)
      setTotalPages(data.totalPages)
      setTotalElements(data.totalElements)
    } catch (e) {
      setErr(e.response?.data?.message || '레시피를 불러오지 못했습니다')
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [keyword, sort, cook, useMyFridge, mine, page])

  // 홈에서 검색어를 들고 들어온 경우
  useEffect(() => {
    const kw = route.params?.keyword
    if (kw !== undefined) {
      setKeyword(kw)
      load(true, { keyword: kw })
    } else {
      load(true)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [route.params?.keyword])

  function setTab(m) { if (mine === m) return; setMine(m); load(true, { mine: m }) }
  function pickSort(k) { setSort(k); load(true, { sort: k }) }
  function pickCook(c) { setCook({ min: c.min, max: c.max }); load(true, { cook: { min: c.min, max: c.max } }) }
  function toggleFridge() { const v = !useMyFridge; setUseMyFridge(v); load(true, { useMyFridge: v }) }
  const openDetail = (id) => navigation.navigate('RecipeDetail', { recipeId: id })

  async function toggleWish(item) {
    const prev = item.isWishlisted
    setContent((list) => list.map((r) => (r.recipeId === item.recipeId ? { ...r, isWishlisted: !prev } : r)))
    try {
      if (prev) await removeRecipeWish(item.recipeId)
      else await addRecipeWish(item.recipeId)
    } catch (e) {
      setContent((list) => list.map((r) => (r.recipeId === item.recipeId ? { ...r, isWishlisted: prev } : r)))
      if (e.response?.status !== 409) Alert.alert('실패', e.response?.data?.message || '찜 처리에 실패했어요')
    }
  }

  const Header = (
    <View>
      <Text style={styles.h}>레시피 탐색</Text>
      <View style={styles.tabs}>
        <Pressable style={[styles.tab, !mine && styles.tabOn]} onPress={() => setTab(false)}><Text style={[styles.tabT, !mine && styles.tabTOn]}>전체</Text></Pressable>
        <Pressable style={[styles.tab, mine && styles.tabOn]} onPress={() => setTab(true)}><Text style={[styles.tabT, mine && styles.tabTOn]}>내가 등록한</Text></Pressable>
      </View>
      <View style={styles.searchRow}>
        <TextInput style={styles.search} value={keyword} onChangeText={setKeyword} placeholder="레시피명·재료 검색" placeholderTextColor="#9AA0A6" returnKeyType="search" onSubmitEditing={() => load(true)} />
        <Pressable style={styles.go} onPress={() => load(true)}><Text style={styles.goT}>검색</Text></Pressable>
      </View>
      <View style={styles.chips}>
        {SORTS.map((s) => <Pressable key={s.key} style={[styles.chip, sort === s.key && styles.chipOn]} onPress={() => pickSort(s.key)}><Text style={[styles.chipT, sort === s.key && styles.chipTOn]}>{s.label}</Text></Pressable>)}
      </View>
      <View style={styles.chips}>
        {COOK_TIMES.map((c) => <Pressable key={c.label} style={[styles.chip, cook.min === c.min && cook.max === c.max && styles.chipOn]} onPress={() => pickCook(c)}><Text style={[styles.chipT, cook.min === c.min && cook.max === c.max && styles.chipTOn]}>{c.label}</Text></Pressable>)}
        <Pressable style={[styles.chip, useMyFridge && styles.chipMine]} onPress={toggleFridge}><Text style={[styles.chipT, useMyFridge && { color: colors.info, fontWeight: '700' }]}>내 재료</Text></Pressable>
      </View>
      {!loading && <Text style={styles.count}>{totalElements}개</Text>}
    </View>
  )

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      {loading ? (
        <>{Header}<ActivityIndicator color={colors.primary} style={{ marginTop: 30 }} /></>
      ) : (
        <FlatList
          data={content}
          keyExtractor={(r) => String(r.recipeId)}
          numColumns={2}
          columnWrapperStyle={{ gap: 12, paddingHorizontal: 16 }}
          contentContainerStyle={{ paddingBottom: 32, gap: 12 }}
          ListHeaderComponent={<View style={{ paddingHorizontal: 16 }}>{Header}</View>}
          renderItem={({ item }) => (
            <RecipeCard recipe={item} onOpen={openDetail} onToggleWish={toggleWish} style={{ flex: 1 }} />
          )}
          ListEmptyComponent={err ? <Text style={styles.err}>{err}</Text> : <Text style={styles.empty}>검색 결과가 없습니다.</Text>}
          onEndReachedThreshold={0.4}
          onEndReached={() => { if (!loadingMore && page + 1 < totalPages) load(false) }}
          ListFooterComponent={loadingMore ? <ActivityIndicator color={colors.primary} style={{ marginVertical: 16 }} /> : null}
        />
      )}
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.surfaceBg },
  h: { fontSize: 22, fontWeight: '800', color: colors.text, marginTop: 4, marginBottom: 12 },
  tabs: { flexDirection: 'row', gap: 8, marginBottom: 12 },
  tab: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.pill, paddingHorizontal: 16, paddingVertical: 7, backgroundColor: '#fff' },
  tabOn: { borderColor: colors.primaryDeep, backgroundColor: colors.primaryTint },
  tabT: { fontSize: 14, color: '#666' },
  tabTOn: { color: colors.primaryDeep, fontWeight: '700' },
  searchRow: { flexDirection: 'row', gap: 8, marginBottom: 12 },
  search: { flex: 1, borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingHorizontal: 12, paddingVertical: 10, fontSize: 14, backgroundColor: '#fff', color: colors.text },
  go: { backgroundColor: colors.primary, borderRadius: 8, paddingHorizontal: 18, justifyContent: 'center' },
  goT: { color: colors.onPrimary, fontWeight: '700', fontSize: 14 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginBottom: 8 },
  chip: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.pill, paddingHorizontal: 12, paddingVertical: 6, backgroundColor: '#fff' },
  chipOn: { borderColor: colors.primaryDeep, backgroundColor: colors.primaryTint },
  chipMine: { borderColor: colors.info, backgroundColor: '#EFF6FF' },
  chipT: { fontSize: 13, color: '#666' },
  chipTOn: { color: colors.primaryDeep, fontWeight: '700' },
  count: { fontSize: 12, color: '#999', marginBottom: 10 },
  empty: { textAlign: 'center', color: '#999', marginTop: 40 },
  err: { textAlign: 'center', color: '#e11d48', marginTop: 40 },
})
