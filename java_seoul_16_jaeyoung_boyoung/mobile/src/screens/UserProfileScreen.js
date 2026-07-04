import { useState, useCallback } from 'react'
import { View, Text, Image, Pressable, ScrollView, StyleSheet, Alert, ActivityIndicator } from 'react-native'
import { useFocusEffect } from '@react-navigation/native'
import { fetchProfile, fetchUserRecipes } from '../api/member'
import { follow, unfollow } from '../api/follow'
import { addRecipeWish, removeRecipeWish } from '../api/wishlist'
import { API_BASE } from '../config'
import { useAuth } from '../stores/auth'
import RecipeCard from '../components/RecipeCard'
import { colors, radius, cardShadow } from '../theme'

const abs = (u) => (!u ? null : u.startsWith('/') ? API_BASE + u : u)

export default function UserProfileScreen({ route, navigation }) {
  const { userId } = route.params
  const myId = useAuth((s) => s.memberId)
  const [profile, setProfile] = useState(null)
  const [recipes, setRecipes] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    setError('')
    try {
      const p = await fetchProfile(userId)
      setProfile(p)
      fetchUserRecipes(userId).then(setRecipes).catch(() => setRecipes([]))
    } catch (e) {
      setError(e.response?.status === 404 ? '사용자를 찾을 수 없습니다' : (e.response?.data?.message || '불러오기 실패'))
    } finally { setLoading(false) }
  }, [userId])

  useFocusEffect(useCallback(() => { load() }, [load]))

  const isMe = profile && myId === profile.memberId

  async function toggleFollow() {
    setBusy(true)
    try {
      const res = profile.isFollowing ? await unfollow(profile.memberId) : await follow(profile.memberId)
      setProfile((p) => ({ ...p, isFollowing: res.following, followerCount: res.followerCount }))
    } catch (e) { Alert.alert('실패', e.response?.data?.message || '처리에 실패했어요') }
    finally { setBusy(false) }
  }

  async function toggleWish(item) {
    const prev = item.isWishlisted
    setRecipes((l) => l.map((r) => (r.recipeId === item.recipeId ? { ...r, isWishlisted: !prev } : r)))
    try {
      if (prev) await removeRecipeWish(item.recipeId)
      else await addRecipeWish(item.recipeId)
    } catch (e) {
      setRecipes((l) => l.map((r) => (r.recipeId === item.recipeId ? { ...r, isWishlisted: prev } : r)))
    }
  }

  if (loading) return <View style={styles.center}><ActivityIndicator color={colors.primary} /></View>
  if (error) return <View style={styles.center}><Text style={styles.err}>{error}</Text></View>
  if (!profile) return null

  const avatar = abs(profile.profileImageUrl)

  return (
    <ScrollView style={styles.wrap} contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
      <View style={styles.card}>
        {avatar ? <Image source={{ uri: avatar }} style={styles.avatarImg} /> : <View style={styles.avatar}><Text style={styles.avatarT}>{profile.nickname?.[0] || '?'}</Text></View>}
        <Text style={styles.nick}>{profile.nickname}</Text>
        <View style={styles.stats}>
          <View style={styles.stat}><Text style={styles.statN}>{profile.recipeCount}</Text><Text style={styles.statL}>레시피</Text></View>
          <View style={styles.stat}><Text style={styles.statN}>{profile.followerCount}</Text><Text style={styles.statL}>팔로워</Text></View>
        </View>
        {isMe ? (
          <View style={styles.meBtn}><Text style={styles.meT}>나</Text></View>
        ) : (
          <Pressable style={[styles.follow, profile.isFollowing && styles.followOn]} disabled={busy} onPress={toggleFollow}>
            <Text style={[styles.followT, profile.isFollowing && { color: colors.primaryDeep }]}>{profile.isFollowing ? '팔로잉' : '+ 팔로우'}</Text>
          </Pressable>
        )}
      </View>

      <Text style={styles.sec}>{isMe ? '내가 공개한 레시피' : `${profile.nickname}님의 레시피`} <Text style={styles.cnt}>{profile.recipeCount}</Text></Text>
      {recipes.length === 0 ? (
        <Text style={styles.empty}>아직 공개한 레시피가 없어요.</Text>
      ) : (
        <View style={styles.grid}>
          {recipes.map((r) => (
            <RecipeCard key={r.recipeId} recipe={r} style={styles.gridItem} onOpen={(id) => navigation.navigate('RecipeDetail', { recipeId: id })} onToggleWish={toggleWish} />
          ))}
        </View>
      )}
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: colors.surfaceBg },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.surfaceBg },
  err: { color: '#e11d48' },
  card: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line, borderRadius: 16, padding: 28, alignItems: 'center', ...cardShadow },
  avatar: { width: 72, height: 72, borderRadius: 36, backgroundColor: colors.primaryTint, alignItems: 'center', justifyContent: 'center', marginBottom: 12 },
  avatarImg: { width: 72, height: 72, borderRadius: 36, marginBottom: 12 },
  avatarT: { color: colors.primaryDeep, fontSize: 30, fontWeight: '800' },
  nick: { fontSize: 20, fontWeight: '700', color: colors.text },
  stats: { flexDirection: 'row', gap: 28, marginVertical: 16 },
  stat: { alignItems: 'center' },
  statN: { fontSize: 18, fontWeight: '800', color: '#333' },
  statL: { fontSize: 12, color: '#888', marginTop: 2 },
  follow: { paddingHorizontal: 28, paddingVertical: 11, borderRadius: radius.pill, backgroundColor: colors.primary },
  followOn: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.primaryDeep },
  followT: { color: colors.onPrimary, fontSize: 14, fontWeight: '700' },
  meBtn: { paddingHorizontal: 28, paddingVertical: 11, borderRadius: radius.pill, borderWidth: 1, borderColor: colors.line, backgroundColor: '#f5f7f9' },
  meT: { color: '#999', fontSize: 14 },
  sec: { fontSize: 16, fontWeight: '700', color: colors.text, marginTop: 24, marginBottom: 12 },
  cnt: { color: colors.primaryDeep, fontSize: 13 },
  empty: { textAlign: 'center', color: '#999', marginTop: 30 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  gridItem: { width: '48%' },
})
