import { useState, useCallback } from 'react'
import { View, Text, Pressable, ScrollView, StyleSheet, ActivityIndicator, RefreshControl } from 'react-native'
import { useFocusEffect } from '@react-navigation/native'
import { Ionicons } from '@expo/vector-icons'
import { SafeAreaView } from 'react-native-safe-area-context'
import { listChallenges, myChallenges, fetchChallengeStats } from '../api/challenge'
import { listBadges } from '../api/member'
import { colors, radius, cardShadow } from '../theme'

const TABS = [{ key: 'active', label: '진행중' }, { key: 'ended', label: '종료' }, { key: 'my', label: '내 챌린지' }]
const dText = (d) => (d < 0 ? '종료' : d === 0 ? 'D-DAY' : `D-${d}`)

export default function ChallengeScreen({ navigation }) {
  const [tab, setTab] = useState('active')
  const [items, setItems] = useState([])
  const [badges, setBadges] = useState([])
  const [active, setActive] = useState(0)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async (t = tab) => {
    setLoading(true)
    try {
      const list = t === 'my' ? await myChallenges() : await listChallenges(t)
      setItems(list)
    } finally { setLoading(false) }
  }, [tab])

  useFocusEffect(useCallback(() => {
    load(tab)
    fetchChallengeStats().then((s) => setActive(s.activeParticipants)).catch(() => {})
    listBadges().then(setBadges).catch(() => {})
  }, [load, tab]))

  function pick(k) { setTab(k); load(k) }

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView contentContainerStyle={{ padding: 16, paddingBottom: 40 }} refreshControl={<RefreshControl refreshing={false} onRefresh={() => load(tab)} />}>
        <Text style={styles.h}>냉파 챌린지</Text>

        <View style={styles.banner}>
          <Text style={styles.bannerT}>🔥 지금 <Text style={{ fontWeight: '800' }}>{active}</Text>명이 챌린지에 도전 중!</Text>
          <Text style={styles.bannerSub}>나도 도전해 식비를 아껴봐요</Text>
        </View>

        <View style={styles.badges}>
          <Text style={styles.bsec}>내 배지 <Text style={{ color: colors.primaryDeep }}>{badges.length}</Text></Text>
          {badges.length === 0 ? (
            <Text style={styles.muted}>아직 획득한 배지가 없어요. 챌린지를 완료해 보세요!</Text>
          ) : (
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 14 }}>
              {badges.map((b) => (
                <View key={b.badgeId} style={styles.badge}><View style={styles.bic}><Text style={{ fontSize: 22 }}>🏅</Text></View><Text style={styles.bname}>{b.name}</Text></View>
              ))}
            </ScrollView>
          )}
        </View>

        <View style={styles.tabs}>
          {TABS.map((t) => <Pressable key={t.key} style={[styles.tab, tab === t.key && styles.tabOn]} onPress={() => pick(t.key)}><Text style={[styles.tabT, tab === t.key && styles.tabTOn]}>{t.label}</Text></Pressable>)}
        </View>

        {loading ? <ActivityIndicator color={colors.primary} style={{ marginTop: 30 }} />
          : items.length === 0 ? <Text style={styles.empty}>표시할 챌린지가 없어요.</Text>
          : items.map((c) => (
            <Pressable key={c.challengeId} style={styles.card} onPress={() => navigation.navigate('ChallengeDetail', { challengeId: c.challengeId })}>
              <View style={styles.ctop}>
                <View style={[styles.dday, c.dDay < 0 && styles.ddayEnded]}><Text style={[styles.ddayT, c.dDay < 0 && { color: '#999' }]}>{dText(c.dDay)}</Text></View>
                <Text style={styles.part}><Ionicons name="people-outline" size={13} color="#888" /> {c.participantCount}</Text>
              </View>
              <Text style={styles.ctitle}>{c.title}</Text>
              <Text style={styles.cdesc} numberOfLines={2}>{c.description}</Text>
              <View style={styles.cfoot}>
                <Text style={styles.reward}>🏅 {c.badge?.name}</Text>
                {c.myStatus === 'JOINED' && <Text style={styles.joined}>참여중 {c.myProgress}%</Text>}
              </View>
              {c.myStatus === 'JOINED' && <View style={styles.bar}><View style={[styles.fill, { width: `${c.myProgress}%` }]} /></View>}
            </Pressable>
          ))}
      </ScrollView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.surfaceBg },
  h: { fontSize: 22, fontWeight: '800', color: colors.text, marginBottom: 14 },
  banner: { backgroundColor: '#F59E0B', borderRadius: 14, padding: 18, marginBottom: 14 },
  bannerT: { color: '#fff', fontSize: 15 },
  bannerSub: { color: 'rgba(255,255,255,0.92)', fontSize: 13, marginTop: 4 },
  badges: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 12, padding: 14, marginBottom: 14, ...cardShadow },
  bsec: { fontSize: 14, fontWeight: '600', marginBottom: 8, color: colors.text },
  muted: { color: '#999', fontSize: 12 },
  badge: { alignItems: 'center', gap: 4 },
  bic: { width: 46, height: 46, borderRadius: 23, backgroundColor: '#fff7ed', borderWidth: 1, borderColor: '#fde68a', alignItems: 'center', justifyContent: 'center' },
  bname: { fontSize: 11, color: '#555' },
  tabs: { flexDirection: 'row', gap: 6, marginBottom: 12 },
  tab: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.pill, paddingHorizontal: 14, paddingVertical: 7, backgroundColor: '#fff' },
  tabOn: { borderColor: colors.primaryDeep, backgroundColor: colors.primaryTint },
  tabT: { fontSize: 13, color: '#666' },
  tabTOn: { color: colors.primaryDeep, fontWeight: '700' },
  empty: { textAlign: 'center', color: '#999', marginTop: 36 },
  card: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 14, padding: 16, marginBottom: 12, ...cardShadow },
  ctop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  dday: { backgroundColor: '#fef2f2', borderRadius: 6, paddingHorizontal: 8, paddingVertical: 3 },
  ddayEnded: { backgroundColor: '#f1f3f5' },
  ddayT: { fontSize: 12, fontWeight: '800', color: '#ef4444' },
  part: { fontSize: 12, color: '#888' },
  ctitle: { fontSize: 16, fontWeight: '700', marginTop: 8, color: colors.text },
  cdesc: { fontSize: 13, color: '#888', marginTop: 3 },
  cfoot: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 10 },
  reward: { color: '#f59e0b', fontWeight: '600', fontSize: 12 },
  joined: { color: colors.primaryDeep, fontWeight: '700', fontSize: 12 },
  bar: { height: 6, backgroundColor: '#f0f0f0', borderRadius: 3, overflow: 'hidden', marginTop: 8 },
  fill: { height: '100%', backgroundColor: colors.primaryDeep },
})
