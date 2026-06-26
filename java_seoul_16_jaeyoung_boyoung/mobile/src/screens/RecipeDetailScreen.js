import { useState, useCallback } from 'react'
import { View, Text, Image, Pressable, ScrollView, TextInput, StyleSheet, Alert, ActivityIndicator } from 'react-native'
import { useFocusEffect } from '@react-navigation/native'
import { Ionicons } from '@expo/vector-icons'
import { fetchRecipeDetail, updateRecipeReview, uploadRecipeImage } from '../api/recipe'
import { addRecipeWish, removeRecipeWish } from '../api/wishlist'
import { pickImage } from '../lib/pickImage'
import { API_BASE } from '../config'
import ReviewSection from '../components/ReviewSection'
import ReportButton from '../components/ReportButton'
import { colors, radius, cardShadow } from '../theme'

const abs = (u) => (!u ? null : u.startsWith('/') ? API_BASE + u : u)

export default function RecipeDetailScreen({ route, navigation }) {
  const { recipeId } = route.params
  const [recipe, setRecipe] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [myReview, setMyReview] = useState('')
  const [savingReview, setSavingReview] = useState(false)
  const [uploading, setUploading] = useState(false)

  async function onUpload() {
    const { file, error: pErr, canceled } = await pickImage()
    if (canceled) return
    if (pErr) { Alert.alert('알림', pErr); return }
    setUploading(true)
    try {
      const { imageUrl } = await uploadRecipeImage(recipe.recipeId, file)
      setRecipe((r) => ({ ...r, thumbnailUrl: imageUrl }))
    } catch (e) { Alert.alert('실패', e.response?.data?.message || '사진 업로드에 실패했어요') }
    finally { setUploading(false) }
  }

  const load = useCallback(async () => {
    setError('')
    try {
      const data = await fetchRecipeDetail(recipeId)
      setRecipe(data)
      setMyReview(data.authorReview || '')
    } catch (e) {
      setError(e.response?.status === 404 ? '레시피를 찾을 수 없습니다' : (e.response?.data?.message || '불러오기 실패'))
    } finally { setLoading(false) }
  }, [recipeId])

  useFocusEffect(useCallback(() => { load() }, [load]))

  async function toggleWish() {
    const prev = recipe.isWishlisted
    setRecipe((r) => ({ ...r, isWishlisted: !prev }))
    try {
      if (prev) await removeRecipeWish(recipe.recipeId)
      else await addRecipeWish(recipe.recipeId)
    } catch (e) {
      setRecipe((r) => ({ ...r, isWishlisted: prev }))
      if (e.response?.status !== 409) Alert.alert('실패', e.response?.data?.message || '찜 처리에 실패했어요')
    }
  }

  async function saveMyReview() {
    setSavingReview(true)
    try {
      const { authorReview } = await updateRecipeReview(recipe.recipeId, myReview.trim())
      setRecipe((r) => ({ ...r, authorReview }))
      setMyReview(authorReview || '')
      Alert.alert('완료', '후기를 저장했어요')
    } catch (e) { Alert.alert('실패', e.response?.data?.message || '후기 저장에 실패했어요') }
    finally { setSavingReview(false) }
  }

  if (loading) return <View style={styles.center}><ActivityIndicator color={colors.primary} /></View>
  if (error) return <View style={styles.center}><Text style={styles.err}>{error}</Text></View>
  if (!recipe) return null

  const hero = abs(recipe.thumbnailUrl)
  const n = recipe.nutrition || {}
  const nutri = [['칼로리', n.calories, 'kcal'], ['탄수', n.carbs], ['단백', n.protein], ['지방', n.fat], ['나트륨', n.sodium]].filter((x) => x[1] != null)

  return (
    <ScrollView style={styles.wrap} contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
      <View style={styles.hero}>
        {hero ? <Image source={{ uri: hero }} style={styles.heroImg} /> : <Ionicons name="image-outline" size={56} color="#c7ccd1" />}
        {recipe.isOwner && (
          <Pressable style={styles.uploadBtn} disabled={uploading} onPress={onUpload}>
            <Text style={styles.uploadT}>{uploading ? '올리는 중…' : `📷 ${hero ? '사진 변경' : '사진 올리기'}`}</Text>
          </Pressable>
        )}
      </View>

      <Text style={styles.title}>{recipe.title}</Text>
      <View style={styles.meta}>
        {!!recipe.cookTime && <Text style={styles.metaT}><Ionicons name="time-outline" size={13} /> {recipe.cookTime}분</Text>}
        <Text style={styles.metaT}><Ionicons name="star" size={13} color="#f59e0b" /> {Number(recipe.avgRating).toFixed(1)} (리뷰 {recipe.reviewCount})</Text>
      </View>
      {!!recipe.summary && <Text style={styles.summary}>{recipe.summary}</Text>}

      {!!recipe.authorNote && (
        <View style={styles.note}>
          <Text style={styles.noteLbl}>💬 작성자 한마디</Text>
          <Text style={styles.noteText}>{recipe.authorNote}</Text>
        </View>
      )}

      {nutri.length > 0 && (
        <View style={styles.nutri}>
          {nutri.map(([lbl, v, u]) => (
            <View key={lbl} style={styles.nutriItem}><Text style={styles.nutriV}>{v}{u || ''}</Text><Text style={styles.nutriL}>{lbl}</Text></View>
          ))}
        </View>
      )}

      <View style={styles.cta}>
        <Pressable style={[styles.wish, recipe.isWishlisted && styles.wishOn]} onPress={toggleWish}>
          <Text style={[styles.wishT, recipe.isWishlisted && { color: colors.primaryDeep }]}>{recipe.isWishlisted ? '♥ 찜 해제' : '♡ 찜하기'}</Text>
        </Pressable>
        {!recipe.isOwner && <ReportButton targetType="RECIPE" targetId={recipe.recipeId} />}
      </View>

      {!!recipe.author && (
        <Pressable style={styles.authorCard} onPress={() => navigation.navigate('UserProfile', { userId: recipe.author.memberId })}>
          {abs(recipe.author.profileImageUrl)
            ? <Image source={{ uri: abs(recipe.author.profileImageUrl) }} style={styles.acAvatarImg} />
            : <View style={styles.acAvatar}><Text style={styles.acAvatarT}>{recipe.author.nickname?.[0] || '?'}</Text></View>}
          <View style={{ flex: 1 }}>
            <Text style={styles.acLabel}>레시피를 공개한 사람</Text>
            <Text style={styles.acNick}>{recipe.author.nickname}</Text>
          </View>
          <Ionicons name="chevron-forward" size={20} color="#c0c6cc" />
        </Pressable>
      )}

      <View style={styles.card}>
        <Text style={styles.sec}>재료</Text>
        {recipe.ingredients?.length ? recipe.ingredients.map((ing, i) => (
          <View key={i} style={styles.ingRow}><Text style={styles.ingName}>{ing.name}</Text><Text style={styles.ingQty}>{ing.qty}</Text></View>
        )) : <Text style={styles.muted}>등록된 재료 정보가 없습니다.</Text>}
      </View>

      <View style={styles.card}>
        <Text style={styles.sec}>조리 순서</Text>
        {recipe.steps?.length ? recipe.steps.map((s) => (
          <View key={s.stepNumber} style={styles.step}>
            <View style={styles.stepN}><Text style={styles.stepNT}>{s.stepNumber}</Text></View>
            <Text style={styles.stepDesc}>{s.description}</Text>
          </View>
        )) : <Text style={styles.muted}>등록된 조리 순서가 없습니다.</Text>}
      </View>

      {recipe.isOwner ? (
        <View style={styles.card}>
          <Text style={styles.sec}>내 후기</Text>
          <TextInput style={styles.reviewInput} value={myReview} onChangeText={setMyReview} multiline maxLength={1000} placeholder="직접 만들어 본 후기를 남겨보세요." placeholderTextColor="#9AA0A6" />
          <Pressable style={styles.reviewSave} disabled={savingReview} onPress={saveMyReview}><Text style={styles.reviewSaveT}>{savingReview ? '저장 중…' : '후기 저장'}</Text></Pressable>
        </View>
      ) : !!recipe.authorReview && (
        <View style={styles.card}>
          <Text style={styles.sec}>작성자 후기</Text>
          <Text style={styles.reviewText}>{recipe.authorReview}</Text>
        </View>
      )}

      <View style={styles.card}>
        <ReviewSection recipeId={recipe.recipeId} onChanged={load} />
      </View>
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: colors.surfaceBg },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.surfaceBg },
  err: { color: '#e11d48' },
  hero: { height: 220, borderRadius: radius.md, backgroundColor: '#f1f3f5', borderWidth: 1, borderColor: colors.line, alignItems: 'center', justifyContent: 'center', overflow: 'hidden' },
  heroImg: { width: '100%', height: '100%' },
  uploadBtn: { position: 'absolute', right: 12, bottom: 12, backgroundColor: 'rgba(0,0,0,0.6)', borderRadius: radius.pill, paddingHorizontal: 16, paddingVertical: 9 },
  uploadT: { color: '#fff', fontSize: 13, fontWeight: '600' },
  title: { fontSize: 24, fontWeight: '800', color: colors.text, marginTop: 16 },
  meta: { flexDirection: 'row', gap: 14, marginTop: 10 },
  metaT: { fontSize: 14, color: colors.textSoft },
  summary: { fontSize: 15, color: '#555', marginTop: 12, lineHeight: 22 },
  note: { backgroundColor: colors.primaryTint, borderRadius: radius.sm, padding: 12, marginTop: 14 },
  noteLbl: { fontSize: 12, fontWeight: '700', color: colors.primaryDeep },
  noteText: { fontSize: 14, color: '#444', marginTop: 6, lineHeight: 21 },
  nutri: { flexDirection: 'row', gap: 8, marginTop: 16 },
  nutriItem: { flex: 1, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line, borderRadius: radius.md, paddingVertical: 12, alignItems: 'center', ...cardShadow },
  nutriV: { fontSize: 15, fontWeight: '700', color: colors.text },
  nutriL: { fontSize: 10, color: colors.textSoft, marginTop: 3 },
  cta: { flexDirection: 'row', alignItems: 'center', gap: 10, marginTop: 18 },
  wish: { backgroundColor: colors.primary, borderRadius: radius.sm, paddingVertical: 13, paddingHorizontal: 28 },
  wishOn: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.primary },
  wishT: { color: colors.onPrimary, fontSize: 15, fontWeight: '700' },
  authorCard: { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line, borderRadius: radius.md, padding: 12, marginTop: 14, ...cardShadow },
  acAvatar: { width: 42, height: 42, borderRadius: 21, backgroundColor: colors.primaryTint, alignItems: 'center', justifyContent: 'center' },
  acAvatarImg: { width: 42, height: 42, borderRadius: 21 },
  acAvatarT: { color: colors.primaryDeep, fontWeight: '800', fontSize: 18 },
  acLabel: { fontSize: 11, color: colors.textSoft },
  acNick: { fontSize: 15, fontWeight: '700', color: colors.text },
  card: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line, borderRadius: radius.md, padding: 18, marginTop: 16, ...cardShadow },
  sec: { fontSize: 16, fontWeight: '700', marginBottom: 14, color: colors.text },
  ingRow: { flexDirection: 'row', justifyContent: 'space-between', borderWidth: 1, borderColor: colors.line, borderRadius: radius.sm, paddingHorizontal: 12, paddingVertical: 11, marginBottom: 8 },
  ingName: { fontSize: 14, color: colors.text },
  ingQty: { fontSize: 14, color: colors.textSoft },
  step: { flexDirection: 'row', gap: 12, marginBottom: 14 },
  stepN: { width: 26, height: 26, borderRadius: 13, backgroundColor: 'rgba(0,217,146,0.14)', alignItems: 'center', justifyContent: 'center' },
  stepNT: { color: colors.primaryDeep, fontWeight: '700', fontSize: 13 },
  stepDesc: { flex: 1, fontSize: 14, lineHeight: 21, paddingTop: 2 },
  reviewInput: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.sm, padding: 11, fontSize: 14, minHeight: 80, textAlignVertical: 'top' },
  reviewSave: { alignSelf: 'flex-end', marginTop: 10, backgroundColor: colors.primary, borderRadius: radius.sm, paddingHorizontal: 18, paddingVertical: 9 },
  reviewSaveT: { color: colors.onPrimary, fontWeight: '700', fontSize: 14 },
  reviewText: { fontSize: 14, color: '#444', lineHeight: 22 },
  muted: { color: '#999' },
})
