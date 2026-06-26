import { useState, useCallback } from 'react'
import { View, Text, Image, Pressable, ScrollView, Modal, TextInput, StyleSheet, Alert, ActivityIndicator, RefreshControl } from 'react-native'
import { useFocusEffect } from '@react-navigation/native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { Ionicons } from '@expo/vector-icons'
import { getMyPage, updateMe } from '../api/member'
import { listWishlist, removeRecipeWish } from '../api/wishlist'
import { listMyReviews } from '../api/member'
import { API_BASE } from '../config'
import { useAuth } from '../stores/auth'
import MyRecipeList from '../components/MyRecipeList'
import { colors, radius, cardShadow } from '../theme'

const abs = (u) => (!u ? null : u.startsWith('/') ? API_BASE + u : u)

export default function MyPageScreen({ navigation }) {
  const setNickname = useAuth((s) => s.setNickname)
  const logout = useAuth((s) => s.logout)
  const [me, setMe] = useState(null)
  const [wishes, setWishes] = useState([])
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)

  const [editOpen, setEditOpen] = useState(false)
  const [eNick, setENick] = useState('')
  const [eAllergy, setEAllergy] = useState('')
  const [ePw, setEPw] = useState('')
  const [eNewPw, setENewPw] = useState('')
  const [editErr, setEditErr] = useState('')
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    try {
      const [m, w, r] = await Promise.all([
        getMyPage(),
        listWishlist({ page: 0, size: 50 }).catch(() => ({ content: [] })),
        listMyReviews({ page: 0, size: 50 }).catch(() => ({ content: [] })),
      ])
      setMe(m); setWishes(w.content || []); setReviews(r.content || [])
    } finally { setLoading(false) }
  }, [])

  useFocusEffect(useCallback(() => { load() }, [load]))

  function openEdit() {
    setENick(me.nickname || ''); setEAllergy((me.allergies || []).join(', ')); setEPw(''); setENewPw(''); setEditErr(''); setEditOpen(true)
  }
  async function saveEdit() {
    setEditErr('')
    const payload = {}
    if (eNick && eNick !== me.nickname) payload.nickname = eNick.trim()
    payload.allergies = eAllergy.split(',').map((s) => s.trim()).filter(Boolean)
    if (eNewPw) {
      if (!ePw) { setEditErr('현재 비밀번호를 입력해 주세요'); return }
      payload.currentPassword = ePw; payload.newPassword = eNewPw
    }
    setSaving(true)
    try {
      const updated = await updateMe(payload)
      setMe(updated); setNickname(updated.nickname); setEditOpen(false)
      Alert.alert('완료', '정보를 수정했어요')
    } catch (e) { setEditErr(e.response?.data?.message || '수정 실패') }
    finally { setSaving(false) }
  }

  function openWish(w) {
    if (w.type === 'AI') Alert.alert('준비 중', 'AI 레시피 상세는 다음 단계에서 추가됩니다.')
    else navigation.navigate('RecipeDetail', { recipeId: w.recipeId })
  }
  function removeWish(w) {
    Alert.alert('찜 해제', '찜을 해제할까요?', [
      { text: '취소', style: 'cancel' },
      { text: '해제', onPress: async () => {
        try { if (w.type !== 'AI') await removeRecipeWish(w.recipeId); setWishes((l) => l.filter((x) => x.wishlistId !== w.wishlistId)) }
        catch (e) { Alert.alert('실패', '해제에 실패했어요') }
      } },
    ])
  }
  function confirmLogout() {
    Alert.alert('로그아웃', '로그아웃할까요?', [{ text: '취소', style: 'cancel' }, { text: '로그아웃', style: 'destructive', onPress: () => logout() }])
  }

  if (loading) return <SafeAreaView style={styles.center} edges={['top']}><ActivityIndicator color={colors.primary} /></SafeAreaView>
  if (!me) return null
  const s = me.stats || {}

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView contentContainerStyle={{ padding: 16, paddingBottom: 40 }} refreshControl={<RefreshControl refreshing={false} onRefresh={load} />}>
        <View style={styles.headRow}>
          <Text style={styles.h}>마이페이지</Text>
          <Pressable onPress={confirmLogout}><Text style={styles.logout}>로그아웃</Text></Pressable>
        </View>

        <View style={styles.profile}>
          <View style={styles.prow}>
            <Pressable onPress={() => Alert.alert('준비 중', '프로필 사진 변경은 다음 단계에서 추가됩니다.')}>
              {abs(me.profileImageUrl)
                ? <Image source={{ uri: abs(me.profileImageUrl) }} style={styles.avatarImg} />
                : <View style={styles.avatar}><Text style={styles.avatarT}>{me.nickname?.[0] || '?'}</Text></View>}
            </Pressable>
            <View style={{ flex: 1, minWidth: 0 }}>
              <Text style={styles.nick}>{me.nickname}</Text>
              <Text style={styles.email}>{me.email}</Text>
              {me.allergies?.length > 0 && (
                <View style={styles.allergyRow}>
                  {me.allergies.map((a) => <View key={a} style={styles.allergyChip}><Text style={styles.allergyT}>{a}</Text></View>)}
                </View>
              )}
            </View>
            <Pressable style={styles.editBtn} onPress={openEdit}><Text style={styles.editT}>정보 수정</Text></Pressable>
          </View>
          <View style={styles.stats}>
            {[['냉장고', s.fridgeCount, () => navigation.navigate('냉장고')],
              ['찜', s.wishlistCount, null],
              ['리뷰', s.reviewCount, null],
              ['팔로워', s.followerCount, () => navigation.navigate('FollowList', { tab: 'followers' })],
              ['팔로잉', s.followingCount, () => navigation.navigate('FollowList', { tab: 'following' })]].map(([l, v, on]) => (
              <Pressable key={l} style={styles.stat} disabled={!on} onPress={on || undefined}>
                <Text style={styles.statN}>{v ?? 0}</Text><Text style={styles.statL}>{l}</Text>
              </Pressable>
            ))}
          </View>
        </View>

        <View style={styles.panel}>
          <Text style={styles.sec}>찜한 레시피 <Text style={styles.cnt}>{wishes.length}</Text></Text>
          {wishes.length === 0 ? <Text style={styles.muted}>아직 찜한 레시피가 없어요.</Text> : wishes.map((w) => (
            <Pressable key={w.wishlistId} style={styles.wrow} onPress={() => openWish(w)}>
              <Text style={[styles.wtype, w.type === 'AI' && { color: '#7c3aed', fontWeight: '700' }]}>{w.type === 'AI' ? 'AI' : '📖'}</Text>
              <View style={{ flex: 1, minWidth: 0 }}>
                <Text style={styles.wtitle} numberOfLines={1}>{w.title}</Text>
                {!!w.cookTime && <Text style={styles.wsub}>{w.cookTime}분</Text>}
              </View>
              <Pressable hitSlop={8} onPress={() => removeWish(w)}><Ionicons name="heart" size={20} color={colors.danger} /></Pressable>
            </Pressable>
          ))}
        </View>

        <View style={styles.panel}>
          <Text style={styles.sec}>내 리뷰 <Text style={styles.cnt}>{reviews.length}</Text></Text>
          {reviews.length === 0 ? <Text style={styles.muted}>작성한 리뷰가 없어요.</Text> : reviews.map((r) => (
            <Pressable key={r.reviewId} style={styles.rcard} onPress={() => navigation.navigate('RecipeDetail', { recipeId: r.recipeId })}>
              <View style={styles.rtop}><Text style={styles.rtitle} numberOfLines={1}>{r.recipeTitle}</Text><Text style={styles.date}>{(r.createdAt || '').slice(0, 10)}</Text></View>
              <Text style={styles.rstars}>{'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}</Text>
              <Text style={styles.rcontent} numberOfLines={2}>{r.content}</Text>
            </Pressable>
          ))}
        </View>

        <View style={styles.panel}>
          <Text style={styles.sec}>마이 레시피</Text>
          <Text style={styles.secDesc}>AI로 담은 내 레시피 · 공개하면 모두가 검색·찜할 수 있어요</Text>
          <MyRecipeList />
        </View>
      </ScrollView>

      <Modal visible={editOpen} transparent animationType="slide" onRequestClose={() => setEditOpen(false)}>
        <View style={styles.overlay}>
          <View style={styles.sheet}>
            <Text style={styles.sheetH}>정보 수정</Text>
            <Text style={styles.label}>닉네임</Text>
            <TextInput style={styles.input} value={eNick} onChangeText={setENick} />
            <Text style={styles.label}>알레르기 (콤마 구분)</Text>
            <TextInput style={styles.input} value={eAllergy} onChangeText={setEAllergy} placeholder="예: 계란, 우유" placeholderTextColor="#9AA0A6" />
            <Text style={styles.label}>비밀번호 변경 (선택)</Text>
            <TextInput style={styles.input} value={ePw} onChangeText={setEPw} placeholder="현재 비밀번호" secureTextEntry placeholderTextColor="#9AA0A6" />
            <TextInput style={[styles.input, { marginTop: 8 }]} value={eNewPw} onChangeText={setENewPw} placeholder="새 비밀번호" secureTextEntry placeholderTextColor="#9AA0A6" />
            {!!editErr && <Text style={styles.err}>{editErr}</Text>}
            <View style={styles.sheetActions}>
              <Pressable style={styles.ghost} onPress={() => setEditOpen(false)}><Text style={styles.ghostT}>취소</Text></Pressable>
              <Pressable style={styles.save} disabled={saving} onPress={saveEdit}><Text style={styles.saveT}>{saving ? '저장 중…' : '저장'}</Text></Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.surfaceBg },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.surfaceBg },
  headRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 },
  h: { fontSize: 22, fontWeight: '800', color: colors.text },
  logout: { color: colors.textSoft, fontSize: 13, fontWeight: '600' },
  profile: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 14, padding: 18, marginBottom: 14, ...cardShadow },
  prow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  avatar: { width: 56, height: 56, borderRadius: 28, backgroundColor: colors.primaryTint, alignItems: 'center', justifyContent: 'center' },
  avatarImg: { width: 56, height: 56, borderRadius: 28 },
  avatarT: { color: colors.primaryDeep, fontSize: 22, fontWeight: '800' },
  nick: { fontSize: 18, fontWeight: '700', color: colors.text },
  email: { fontSize: 13, color: '#999', marginTop: 2 },
  allergyRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 8 },
  allergyChip: { backgroundColor: '#fef2f2', borderRadius: radius.pill, paddingHorizontal: 10, paddingVertical: 3 },
  allergyT: { fontSize: 12, color: '#ef4444' },
  editBtn: { borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingHorizontal: 12, paddingVertical: 8 },
  editT: { fontSize: 13, color: colors.text },
  stats: { flexDirection: 'row', gap: 8, marginTop: 18 },
  stat: { flex: 1, backgroundColor: '#f5f7f9', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  statN: { fontSize: 20, fontWeight: '800', color: colors.primaryDeep },
  statL: { fontSize: 11, color: '#666', marginTop: 2 },
  panel: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 14, padding: 18, marginBottom: 14, ...cardShadow },
  sec: { fontSize: 16, fontWeight: '700', color: colors.text, marginBottom: 6 },
  cnt: { color: colors.primaryDeep, fontSize: 14 },
  secDesc: { fontSize: 12, color: '#999', marginBottom: 12 },
  muted: { color: '#999', paddingVertical: 8 },
  wrow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12, borderTopWidth: 1, borderTopColor: colors.lineSoft },
  wtype: { fontSize: 13 },
  wtitle: { fontSize: 15, fontWeight: '600', color: colors.text },
  wsub: { fontSize: 12, color: '#999', marginTop: 2 },
  rcard: { borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 12, padding: 12, marginTop: 10 },
  rtop: { flexDirection: 'row', justifyContent: 'space-between' },
  rtitle: { fontSize: 14, fontWeight: '600', flex: 1, color: colors.text },
  date: { fontSize: 11, color: '#aaa' },
  rstars: { color: '#f59e0b', fontSize: 13, marginVertical: 4 },
  rcontent: { fontSize: 14, color: '#555' },
  overlay: { flex: 1, backgroundColor: 'rgba(17,24,39,0.5)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 16, borderTopRightRadius: 16, padding: 22, paddingBottom: 34 },
  sheetH: { fontSize: 17, fontWeight: '700', marginBottom: 8 },
  label: { fontSize: 13, color: '#555', marginTop: 12, marginBottom: 4 },
  input: { borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14 },
  err: { color: '#e11d48', fontSize: 13, marginTop: 10 },
  sheetActions: { flexDirection: 'row', gap: 8, justifyContent: 'flex-end', marginTop: 16 },
  ghost: { borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingHorizontal: 18, paddingVertical: 11 },
  ghostT: { color: colors.text, fontWeight: '600' },
  save: { backgroundColor: colors.primary, borderRadius: 8, paddingHorizontal: 22, paddingVertical: 11 },
  saveT: { color: colors.onPrimary, fontWeight: '700' },
})
