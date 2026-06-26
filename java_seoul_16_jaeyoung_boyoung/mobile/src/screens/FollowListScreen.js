import { useState, useCallback } from 'react'
import { View, Text, Pressable, ScrollView, StyleSheet, Alert, ActivityIndicator } from 'react-native'
import { useFocusEffect } from '@react-navigation/native'
import { listFollowing, listFollowers } from '../api/member'
import { follow, unfollow } from '../api/follow'
import { colors, radius, cardShadow } from '../theme'

export default function FollowListScreen({ route, navigation }) {
  const [tab, setTab] = useState(route.params?.tab === 'followers' ? 'followers' : 'following')
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async (t = tab) => {
    setLoading(true)
    try { setItems(t === 'followers' ? await listFollowers() : await listFollowing()) }
    finally { setLoading(false) }
  }, [tab])

  useFocusEffect(useCallback(() => { load(tab) }, [load, tab]))

  function pick(t) { setTab(t); load(t) }

  async function toggle(u) {
    const prev = u.isFollowing
    setItems((l) => l.map((x) => (x.memberId === u.memberId ? { ...x, isFollowing: !prev } : x)))
    try {
      if (prev) await unfollow(u.memberId)
      else await follow(u.memberId)
    } catch (e) {
      setItems((l) => l.map((x) => (x.memberId === u.memberId ? { ...x, isFollowing: prev } : x)))
      Alert.alert('실패', e.response?.data?.message || '처리에 실패했어요')
    }
  }

  return (
    <ScrollView style={styles.wrap} contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
      <View style={styles.tabs}>
        <Pressable style={[styles.tab, tab === 'following' && styles.tabOn]} onPress={() => pick('following')}><Text style={[styles.tabT, tab === 'following' && styles.tabTOn]}>팔로잉</Text></Pressable>
        <Pressable style={[styles.tab, tab === 'followers' && styles.tabOn]} onPress={() => pick('followers')}><Text style={[styles.tabT, tab === 'followers' && styles.tabTOn]}>팔로워</Text></Pressable>
      </View>

      {loading ? <ActivityIndicator color={colors.primary} style={{ marginTop: 30 }} />
        : items.length === 0 ? <Text style={styles.empty}>{tab === 'followers' ? '아직 나를 팔로우하는 사람이 없어요.' : '아직 팔로우한 사람이 없어요.'}</Text>
        : items.map((u) => (
          <View key={u.memberId} style={styles.row}>
            <Pressable style={styles.who} onPress={() => navigation.navigate('UserProfile', { userId: u.memberId })}>
              <View style={styles.avatar}><Text style={styles.avatarT}>{u.nickname?.[0] || '?'}</Text></View>
              <View><Text style={styles.nick}>{u.nickname}</Text><Text style={styles.sub}>리뷰 {u.reviewCount}</Text></View>
            </Pressable>
            <Pressable style={[styles.follow, u.isFollowing && styles.followOn]} onPress={() => toggle(u)}>
              <Text style={[styles.followT, u.isFollowing && { color: colors.primaryDeep }]}>{u.isFollowing ? '팔로잉' : '+ 팔로우'}</Text>
            </Pressable>
          </View>
        ))}
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: colors.surfaceBg },
  tabs: { flexDirection: 'row', gap: 8, marginBottom: 14 },
  tab: { flex: 1, borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingVertical: 10, alignItems: 'center', backgroundColor: '#fff' },
  tabOn: { borderColor: colors.primaryDeep, backgroundColor: colors.primaryTint },
  tabT: { fontSize: 14, color: '#666' },
  tabTOn: { color: colors.primaryDeep, fontWeight: '700' },
  empty: { textAlign: 'center', color: '#999', marginTop: 36 },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 12, padding: 12, marginBottom: 8, ...cardShadow },
  who: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  avatar: { width: 40, height: 40, borderRadius: 20, backgroundColor: colors.primaryTint, alignItems: 'center', justifyContent: 'center' },
  avatarT: { color: colors.primaryDeep, fontWeight: '800' },
  nick: { fontSize: 14, fontWeight: '600', color: colors.text },
  sub: { fontSize: 12, color: '#999' },
  follow: { paddingHorizontal: 14, paddingVertical: 7, borderRadius: radius.pill, backgroundColor: colors.primary },
  followOn: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.primaryDeep },
  followT: { color: colors.onPrimary, fontSize: 12, fontWeight: '700' },
})
