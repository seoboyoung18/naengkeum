import { useState, useCallback } from 'react'
import { View, Text, TextInput, Pressable, ScrollView, StyleSheet, Alert, ActivityIndicator, RefreshControl } from 'react-native'
import { useFocusEffect } from '@react-navigation/native'
import { Ionicons } from '@expo/vector-icons'
import { SafeAreaView } from 'react-native-safe-area-context'
import { fetchMe } from '../api/auth'
import { fetchDashboard } from '../api/fridge'
import { useAuth } from '../stores/auth'
import { colors, radius, cardShadow } from '../theme'

const STORAGE = { FRIDGE: '냉장', FREEZER: '냉동', ROOM_TEMP: '실온' }

export default function HomeScreen({ navigation }) {
  const nickname = useAuth((s) => s.nickname)
  const [me, setMe] = useState(null)
  const [summary, setSummary] = useState({ fridgeCount: 0, freezerCount: 0, roomTempCount: 0 })
  const [expiring, setExpiring] = useState([])
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')

  const load = useCallback(async () => {
    setErr('')
    try {
      const [m, dash] = await Promise.all([fetchMe().catch(() => null), fetchDashboard()])
      if (m) setMe(m)
      setSummary(dash.summary)
      setExpiring(dash.expiringItems || [])
    } catch (e) {
      setErr(e.response?.data?.message || '대시보드를 불러오지 못했습니다')
    } finally {
      setLoading(false)
    }
  }, [])

  useFocusEffect(useCallback(() => { load() }, [load]))

  const total = summary.fridgeCount + summary.freezerCount + summary.roomTempCount
  const dText = (d) => (d < 0 ? `${Math.abs(d)}일 지남` : d === 0 ? '오늘까지' : `${d}일 남음`)
  const search = () => navigation.navigate('레시피', { keyword: keyword.trim() })
  const soon = () => Alert.alert('준비 중', 'AI 추천은 다음 단계에서 추가됩니다.')

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView contentContainerStyle={styles.scroll} refreshControl={<RefreshControl refreshing={false} onRefresh={load} />}>
        <View style={styles.top}>
          <Text style={styles.hi}>안녕하세요, <Text style={styles.hiName}>{nickname || me?.nickname || ''}</Text>님</Text>
        </View>
        <View style={styles.search}>
          <Ionicons name="search-outline" size={18} color="#9AA0A6" />
          <TextInput style={styles.searchInput} value={keyword} onChangeText={setKeyword} placeholder="레시피·재료 검색" placeholderTextColor="#9AA0A6" returnKeyType="search" onSubmitEditing={search} />
        </View>

        <Pressable style={styles.aiBanner} onPress={soon}>
          <View style={{ flex: 1 }}>
            <Text style={styles.aiTitle}>냉장고 재료로 AI 추천 받기</Text>
            <Text style={styles.aiSub}>남은 재료로 만들 수 있는 레시피를 추천해 드려요</Text>
          </View>
          <View style={styles.aiBtn}><Text style={styles.aiBtnT}>추천 받기</Text></View>
        </Pressable>

        {loading ? (
          <ActivityIndicator color={colors.primary} style={{ marginTop: 40 }} />
        ) : err ? (
          <Text style={styles.err}>{err}</Text>
        ) : (
          <>
            <View style={styles.stats}>
              <Pressable style={styles.stat} onPress={() => navigation.navigate('냉장고')}>
                <Text style={styles.statNum}>{total}</Text><Text style={styles.statLbl}>냉장고 재료</Text>
              </Pressable>
              <Pressable style={styles.stat} onPress={() => navigation.navigate('냉장고')}>
                <Text style={[styles.statNum, expiring.length > 0 && { color: colors.warn }]}>{expiring.length}</Text><Text style={styles.statLbl}>임박</Text>
              </Pressable>
              <View style={styles.stat}>
                <Text style={styles.statNum}>{me?.stats?.wishlistCount ?? 0}</Text><Text style={styles.statLbl}>찜한 레시피</Text>
              </View>
              <View style={styles.stat}>
                <Text style={styles.statNum}>{me?.stats?.reviewCount ?? 0}</Text><Text style={styles.statLbl}>내 리뷰</Text>
              </View>
            </View>

            <Pressable style={[styles.card, expiring.length ? styles.imminent : styles.imminentOk]} onPress={() => navigation.navigate('냉장고')}>
              <View style={styles.chead}>
                <Text style={[styles.ctitle, { color: expiring.length ? '#C2410C' : colors.primaryDeep }]}>유통기한 임박</Text>
                <View style={[styles.badge, { backgroundColor: expiring.length ? '#F97316' : colors.primaryDeep }]}><Text style={styles.badgeT}>{expiring.length}</Text></View>
              </View>
              {expiring.length === 0 ? (
                <Text style={styles.iok}>임박한 재료가 없어요. 잘 관리하고 있어요! 👍</Text>
              ) : (
                expiring.slice(0, 6).map((it) => (
                  <View key={it.fridgeItemId} style={styles.irow}>
                    <Text style={styles.iname}>{it.name} <Text style={styles.iqty}>{it.qty}{it.unit}</Text></Text>
                    <Text style={[styles.idday, { color: it.dDay < 0 ? colors.danger : colors.warn }]}>{dText(it.dDay)}</Text>
                  </View>
                ))
              )}
            </Pressable>

            <Pressable style={styles.card} onPress={() => navigation.navigate('냉장고')}>
              <View style={styles.chead}>
                <Text style={styles.ctitleDark}>내 냉장고</Text>
                <Text style={styles.total}>{total}개</Text>
              </View>
              <View style={styles.storage}>
                {[['FRIDGE', summary.fridgeCount], ['FREEZER', summary.freezerCount], ['ROOM_TEMP', summary.roomTempCount]].map(([k, v]) => (
                  <View key={k} style={styles.storageItem}><Text style={styles.storageNum}>{v}</Text><Text style={styles.storageLbl}>{STORAGE[k]}</Text></View>
                ))}
              </View>
            </Pressable>
          </>
        )}
      </ScrollView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.surfaceBg },
  scroll: { padding: 16, paddingBottom: 32 },
  top: { marginBottom: 14 },
  hi: { fontSize: 20, color: colors.text },
  hiName: { fontWeight: '800' },
  search: { flexDirection: 'row', alignItems: 'center', gap: 8, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line, borderRadius: 10, paddingHorizontal: 12, marginBottom: 16 },
  searchInput: { flex: 1, paddingVertical: 10, fontSize: 14, color: colors.text },
  aiBanner: { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: colors.primaryDeep, borderRadius: 16, padding: 20, marginBottom: 16 },
  aiTitle: { color: '#fff', fontSize: 16, fontWeight: '700' },
  aiSub: { color: 'rgba(255,255,255,0.9)', fontSize: 12, marginTop: 4 },
  aiBtn: { backgroundColor: '#fff', borderRadius: 10, paddingHorizontal: 16, paddingVertical: 9 },
  aiBtnT: { color: colors.primaryDeep, fontWeight: '700', fontSize: 13 },
  stats: { flexDirection: 'row', flexWrap: 'wrap', gap: 10, marginBottom: 14 },
  stat: { width: '47.5%', flexGrow: 1, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 14, padding: 16, ...cardShadow },
  statNum: { fontSize: 24, fontWeight: '800', color: colors.primaryDeep },
  statLbl: { fontSize: 12, color: '#777', marginTop: 4 },
  card: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 14, padding: 16, marginBottom: 14, ...cardShadow },
  imminent: { borderColor: '#FED7AA' },
  imminentOk: { borderColor: '#BBF7D0' },
  chead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  ctitle: { fontSize: 15, fontWeight: '700' },
  ctitleDark: { fontSize: 15, fontWeight: '700', color: colors.text },
  total: { color: colors.primaryDeep, fontWeight: '700' },
  badge: { minWidth: 22, height: 22, borderRadius: 11, paddingHorizontal: 7, alignItems: 'center', justifyContent: 'center' },
  badgeT: { color: '#fff', fontSize: 12, fontWeight: '700' },
  iok: { fontSize: 14, color: colors.primaryDeep, marginTop: 12 },
  irow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 10, borderTopWidth: 1, borderTopColor: colors.lineSoft, marginTop: 10 },
  iname: { color: '#333', fontSize: 14 },
  iqty: { color: '#aaa', fontSize: 12 },
  idday: { fontSize: 13, fontWeight: '700' },
  storage: { flexDirection: 'row', gap: 10, marginTop: 12 },
  storageItem: { flex: 1, backgroundColor: '#F5F7F9', borderRadius: 10, paddingVertical: 14, alignItems: 'center' },
  storageNum: { fontSize: 20, fontWeight: '800', color: colors.primaryDeep },
  storageLbl: { fontSize: 12, color: '#666', marginTop: 2 },
  err: { color: '#e11d48', marginTop: 20, textAlign: 'center' },
})
