import { useState, useCallback } from 'react'
import { View, Text, Pressable, ScrollView, StyleSheet, Alert, ActivityIndicator } from 'react-native'
import { useFocusEffect } from '@react-navigation/native'
import { Ionicons } from '@expo/vector-icons'
import { fetchChallengeDetail, joinChallenge, unjoinChallenge, updateProgress } from '../api/challenge'
import { colors, radius } from '../theme'

const PRESETS = [25, 50, 75, 100]
const dText = (d) => (d < 0 ? '종료된 챌린지' : d === 0 ? '오늘 마감 (D-DAY)' : `D-${d}`)

export default function ChallengeDetailScreen({ route }) {
  const { challengeId } = route.params
  const [c, setC] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setError('')
    try { setC(await fetchChallengeDetail(challengeId)) }
    catch (e) { setError(e.response?.status === 404 ? '챌린지를 찾을 수 없습니다' : (e.response?.data?.message || '불러오기 실패')) }
    finally { setLoading(false) }
  }, [challengeId])

  useFocusEffect(useCallback(() => { load() }, [load]))

  async function join() {
    setBusy(true)
    try { await joinChallenge(challengeId); await load() }
    catch (e) { Alert.alert(e.response?.status === 409 ? '알림' : '실패', e.response?.status === 409 ? '이미 참여 중이에요' : (e.response?.data?.message || '참여 실패')) }
    finally { setBusy(false) }
  }
  function unjoin() {
    Alert.alert('참여 취소', '참여를 취소할까요? 진행률이 초기화됩니다.', [
      { text: '취소', style: 'cancel' },
      { text: '확인', style: 'destructive', onPress: async () => {
        setBusy(true)
        try { await unjoinChallenge(challengeId); await load() }
        catch (e) { Alert.alert('실패', e.response?.data?.message || '취소 실패') }
        finally { setBusy(false) }
      } },
    ])
  }
  async function setProgress(p) {
    setBusy(true)
    try {
      const res = await updateProgress(challengeId, p)
      Alert.alert('완료', res.badgeEarned ? '🎉 배지를 획득했어요!' : res.achieved ? '챌린지 달성! 🎯' : `진행률 ${res.progress}%`)
      await load()
    } catch (e) { Alert.alert('실패', e.response?.data?.message || '진행률 갱신 실패') }
    finally { setBusy(false) }
  }

  if (loading) return <View style={styles.center}><ActivityIndicator color={colors.primary} /></View>
  if (error) return <View style={styles.center}><Text style={styles.err}>{error}</Text></View>
  if (!c) return null

  return (
    <ScrollView style={styles.wrap} contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
      <View style={[styles.dday, c.dDay < 0 && styles.ddayEnded]}><Text style={[styles.ddayT, c.dDay < 0 && { color: '#999' }]}>{dText(c.dDay)}</Text></View>
      <Text style={styles.title}>{c.title}</Text>
      <Text style={styles.desc}>{c.description}</Text>
      <Text style={styles.meta}><Ionicons name="people-outline" size={13} color="#999" /> {c.participantCount}명 참여 · {c.startDate} ~ {c.endDate}</Text>

      <View style={styles.reward}>
        <View style={styles.ric}><Text style={{ fontSize: 24 }}>🏅</Text></View>
        <View><Text style={styles.rlbl}>완료 보상</Text><Text style={styles.rname}>{c.badge?.name}</Text></View>
      </View>

      {c.myStatus === 'JOINED' ? (
        <>
          <View style={styles.progHead}><Text style={{ fontSize: 14, color: colors.text }}>내 진행률</Text><Text style={styles.pct}>{c.myProgress}%</Text></View>
          <View style={styles.bar}><View style={[styles.fill, { width: `${c.myProgress}%` }]} /></View>
          <Text style={styles.sec}>진행률 갱신</Text>
          <View style={styles.presets}>
            {PRESETS.map((p) => (
              <Pressable key={p} style={[styles.preset, c.myProgress >= p && styles.presetOn]} disabled={busy} onPress={() => setProgress(p)}>
                <Text style={[styles.presetT, c.myProgress >= p && { color: colors.primaryDeep, fontWeight: '700' }]}>{p}%</Text>
              </Pressable>
            ))}
          </View>
          <Pressable style={styles.unjoin} disabled={busy} onPress={unjoin}><Text style={styles.unjoinT}>참여 취소</Text></Pressable>
        </>
      ) : (
        <Pressable style={[styles.join, c.dDay < 0 && styles.joinDisabled]} disabled={busy || c.dDay < 0} onPress={join}>
          <Text style={styles.joinT}>{c.dDay < 0 ? '종료된 챌린지' : '챌린지 참여하기'}</Text>
        </Pressable>
      )}
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: colors.surfaceBg },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.surfaceBg },
  err: { color: '#e11d48' },
  dday: { alignSelf: 'flex-start', backgroundColor: '#fef2f2', borderRadius: 6, paddingHorizontal: 10, paddingVertical: 4 },
  ddayEnded: { backgroundColor: '#f1f3f5' },
  ddayT: { fontSize: 12, fontWeight: '800', color: '#ef4444' },
  title: { fontSize: 22, fontWeight: '800', color: colors.text, marginTop: 12 },
  desc: { fontSize: 14, color: '#555', marginTop: 6, lineHeight: 21 },
  meta: { fontSize: 12, color: '#999', marginTop: 8 },
  reward: { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: '#fff7ed', borderWidth: 1, borderColor: '#fde68a', borderRadius: 12, padding: 14, marginVertical: 18 },
  ric: { width: 46, height: 46, borderRadius: 23, backgroundColor: '#fff', alignItems: 'center', justifyContent: 'center' },
  rlbl: { fontSize: 12, color: '#b45309' },
  rname: { fontSize: 16, fontWeight: '700', color: '#92400e' },
  progHead: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline', marginTop: 8 },
  pct: { fontSize: 20, fontWeight: '800', color: colors.primaryDeep },
  bar: { height: 10, backgroundColor: '#f0f0f0', borderRadius: 5, overflow: 'hidden', marginVertical: 8 },
  fill: { height: '100%', backgroundColor: colors.primaryDeep },
  sec: { fontSize: 13, color: '#999', marginTop: 18, marginBottom: 8 },
  presets: { flexDirection: 'row', gap: 8 },
  preset: { flex: 1, borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingVertical: 11, alignItems: 'center', backgroundColor: '#fff' },
  presetOn: { borderColor: colors.primaryDeep, backgroundColor: colors.primaryTint },
  presetT: { fontSize: 14, color: '#555' },
  unjoin: { marginTop: 18, borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingVertical: 12, alignItems: 'center', backgroundColor: '#fff' },
  unjoinT: { color: '#888', fontSize: 13 },
  join: { marginTop: 20, backgroundColor: colors.primary, borderRadius: 10, paddingVertical: 14, alignItems: 'center' },
  joinDisabled: { backgroundColor: '#9ca3af' },
  joinT: { color: colors.onPrimary, fontSize: 15, fontWeight: '700' },
})
