import { useState, useCallback } from 'react'
import { View, Text, Pressable, ScrollView, StyleSheet, Alert, ActivityIndicator, RefreshControl } from 'react-native'
import { useFocusEffect } from '@react-navigation/native'
import { Ionicons } from '@expo/vector-icons'
import { SafeAreaView } from 'react-native-safe-area-context'
import { listFridge, deleteFridgeItem } from '../api/fridge'
import { getSeasonings } from '../api/seasoning'
import { colors, radius, cardShadow } from '../theme'

const STORAGES = [
  { key: 'ALL', label: '전체' },
  { key: 'FRIDGE', label: '냉장' },
  { key: 'FREEZER', label: '냉동' },
  { key: 'ROOM_TEMP', label: '실온' },
]
const SORTS = [
  { key: 'EXPIRY_ASC', label: '임박순' },
  { key: 'CREATED_DESC', label: '최신순' },
  { key: 'NAME_ASC', label: '이름순' },
]
const STORAGE_LABEL = { FRIDGE: '냉장', FREEZER: '냉동', ROOM_TEMP: '실온' }

function dday(d) {
  if (d < 0) return { t: `D+${Math.abs(d)}`, bg: '#FEF2F2', fg: colors.danger }
  if (d === 0) return { t: 'D-DAY', bg: '#FEF2F2', fg: colors.danger }
  if (d <= 3) return { t: `D-${d}`, bg: '#FFF7ED', fg: colors.warn }
  return { t: `D-${d}`, bg: colors.primaryTint, fg: colors.primaryDeep }
}

export default function FridgeScreen() {
  const [items, setItems] = useState([])
  const [summary, setSummary] = useState({ fridgeCount: 0, freezerCount: 0, roomTempCount: 0 })
  const [seasonings, setSeasonings] = useState([])
  const [storage, setStorage] = useState('ALL')
  const [sort, setSort] = useState('EXPIRY_ASC')
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')

  const load = useCallback(async (st = storage, so = sort) => {
    setErr('')
    try {
      const [data, seas] = await Promise.all([
        listFridge({ storageType: st, sort: so }),
        getSeasonings().catch(() => []),
      ])
      setItems(data.items)
      setSummary(data.summary)
      setSeasonings((seas || []).filter((s) => s.owned))
    } catch (e) {
      setErr(e.response?.data?.message || '냉장고를 불러오지 못했습니다')
    } finally {
      setLoading(false)
    }
  }, [storage, sort])

  useFocusEffect(useCallback(() => { load(storage, sort) }, [load, storage, sort]))

  const imminent = items.filter((i) => i.dDay <= 3).length

  function selectStorage(k) { setStorage(k); setLoading(true); load(k, sort) }
  function selectSort(k) { setSort(k); setLoading(true); load(storage, k) }
  const soon = () => Alert.alert('준비 중', '재료/조미료 추가·수정은 다음 단계에서 추가됩니다.')

  function onDelete(item) {
    Alert.alert('삭제', `'${item.name}'을(를) 삭제할까요?`, [
      { text: '취소', style: 'cancel' },
      {
        text: '삭제', style: 'destructive',
        onPress: async () => {
          try { await deleteFridgeItem(item.fridgeItemId); load(storage, sort) }
          catch (e) { Alert.alert('실패', e.response?.data?.message || '삭제에 실패했어요') }
        },
      },
    ])
  }

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView contentContainerStyle={styles.scroll} refreshControl={<RefreshControl refreshing={false} onRefresh={() => load(storage, sort)} />}>
        <View style={styles.head}>
          <Text style={styles.h}>내 냉장고</Text>
          <View style={styles.actions}>
            <Pressable style={styles.addBtn} onPress={soon}><Text style={styles.addBtnT}>＋ 재료</Text></Pressable>
            <Pressable style={[styles.addBtn, styles.addAlt]} onPress={soon}><Text style={[styles.addBtnT, { color: colors.primaryDeep }]}>＋ 조미료</Text></Pressable>
          </View>
        </View>

        <View style={styles.summary}>
          {[['냉장', summary.fridgeCount], ['냉동', summary.freezerCount], ['실온', summary.roomTempCount], ['임박', imminent]].map(([l, v], i) => (
            <View key={l} style={styles.sumItem}><Text style={[styles.sumNum, i === 3 && imminent > 0 && { color: colors.warn }]}>{v}</Text><Text style={styles.sumLbl}>{l}</Text></View>
          ))}
        </View>

        <Text style={styles.srLabel}>내 조미료</Text>
        {seasonings.length ? (
          <View style={styles.chips}>
            {seasonings.map((s) => <View key={s.seasoningId} style={styles.chip}><Text style={styles.chipT}>{s.name}</Text></View>)}
          </View>
        ) : (
          <Text style={styles.srEmpty}>아직 없어요 · ＋ 조미료로 체크하세요</Text>
        )}

        <View style={styles.tabs}>
          {STORAGES.map((s) => (
            <Pressable key={s.key} style={[styles.tab, storage === s.key && styles.tabOn]} onPress={() => selectStorage(s.key)}>
              <Text style={[styles.tabT, storage === s.key && styles.tabTOn]}>{s.label}</Text>
            </Pressable>
          ))}
        </View>
        <View style={styles.sortRow}>
          {SORTS.map((s) => (
            <Pressable key={s.key} onPress={() => selectSort(s.key)}>
              <Text style={[styles.sortT, sort === s.key && styles.sortTOn]}>{s.label}</Text>
            </Pressable>
          ))}
        </View>

        {loading ? (
          <ActivityIndicator color={colors.primary} style={{ marginTop: 30 }} />
        ) : err ? (
          <Text style={styles.err}>{err}</Text>
        ) : items.length === 0 ? (
          <Text style={styles.empty}>재료가 없습니다. ＋ 재료로 추가해 보세요.</Text>
        ) : (
          <View style={styles.list}>
            {items.map((it) => {
              const d = dday(it.dDay)
              return (
                <View key={it.fridgeItemId} style={styles.row}>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.name}>{it.name} <Text style={styles.qty}>{it.qty}{it.unit}</Text></Text>
                    <Text style={styles.subline}>{STORAGE_LABEL[it.storageType]} · {it.expiryDate}</Text>
                  </View>
                  <View style={[styles.ddayBadge, { backgroundColor: d.bg }]}><Text style={[styles.ddayT, { color: d.fg }]}>{d.t}</Text></View>
                  <Pressable hitSlop={8} onPress={() => onDelete(it)} style={styles.del}>
                    <Ionicons name="trash-outline" size={18} color="#8b95a1" />
                  </Pressable>
                </View>
              )
            })}
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.surfaceBg },
  scroll: { padding: 16, paddingBottom: 32 },
  head: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 },
  h: { fontSize: 22, fontWeight: '800', color: colors.text },
  actions: { flexDirection: 'row', gap: 8 },
  addBtn: { backgroundColor: colors.primary, borderRadius: 8, paddingHorizontal: 12, paddingVertical: 8 },
  addAlt: { backgroundColor: colors.primaryTint },
  addBtnT: { color: colors.onPrimary, fontWeight: '700', fontSize: 13 },
  summary: { flexDirection: 'row', gap: 8, marginBottom: 16 },
  sumItem: { flex: 1, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 10, paddingVertical: 12, alignItems: 'center', ...cardShadow },
  sumNum: { fontSize: 18, fontWeight: '800', color: colors.primaryDeep },
  sumLbl: { fontSize: 12, color: '#666', marginTop: 2 },
  srLabel: { fontSize: 15, fontWeight: '700', color: colors.primaryDeep, marginBottom: 8 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginBottom: 16 },
  chip: { backgroundColor: '#E5E7EB', borderRadius: radius.pill, paddingHorizontal: 11, paddingVertical: 5 },
  chipT: { fontSize: 12, color: '#374151', fontWeight: '600' },
  srEmpty: { fontSize: 12, color: '#aaa', marginBottom: 16 },
  tabs: { flexDirection: 'row', gap: 6, marginBottom: 10 },
  tab: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.pill, paddingHorizontal: 12, paddingVertical: 6, backgroundColor: '#fff' },
  tabOn: { borderColor: colors.primaryDeep, backgroundColor: colors.primaryTint },
  tabT: { fontSize: 13, color: '#666' },
  tabTOn: { color: colors.primaryDeep, fontWeight: '700' },
  sortRow: { flexDirection: 'row', gap: 14, marginBottom: 14 },
  sortT: { fontSize: 13, color: '#999' },
  sortTOn: { color: colors.primaryDeep, fontWeight: '700' },
  list: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 14, overflow: 'hidden', ...cardShadow },
  row: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingHorizontal: 14, paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: colors.lineSoft },
  name: { fontSize: 14, fontWeight: '600', color: '#333' },
  qty: { color: '#aaa', fontWeight: '400', fontSize: 12 },
  subline: { fontSize: 12, color: '#999', marginTop: 3 },
  ddayBadge: { minWidth: 48, alignItems: 'center', borderRadius: 8, paddingVertical: 5, paddingHorizontal: 8 },
  ddayT: { fontSize: 12, fontWeight: '800' },
  del: { padding: 4 },
  empty: { textAlign: 'center', color: '#999', marginTop: 40 },
  err: { color: '#e11d48', textAlign: 'center', marginTop: 20 },
})
