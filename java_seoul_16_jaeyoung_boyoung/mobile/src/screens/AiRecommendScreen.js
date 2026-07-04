import { useState } from 'react'
import { View, Text, Pressable, ScrollView, StyleSheet, ActivityIndicator, Alert } from 'react-native'
import { Ionicons } from '@expo/vector-icons'
import { recommendAi } from '../api/ai'
import { addRecipeWish, saveAiRecipe } from '../api/wishlist'
import { registerFromAi } from '../api/recipe'
import { colors, radius, cardShadow } from '../theme'

const OPTIONS = [
  ['prioritizeExpiry', '유통기한 임박 우선'],
  ['useAllFridge', '냉장고 재료 전부 사용'],
  ['applyAllergy', '알레르기 반영'],
]

export default function AiRecommendScreen({ navigation }) {
  const [opts, setOpts] = useState({ prioritizeExpiry: true, useAllFridge: false, applyAllergy: true })
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [registered, setRegistered] = useState(false)
  const [busy, setBusy] = useState(false)

  async function run() {
    setError(''); setResult(null); setSaved(false); setRegistered(false); setLoading(true)
    try {
      const r = await recommendAi(opts)
      if (r.error) setError(r.error)
      setResult(r)
    } catch (e) {
      setError(e.message || 'AI 추천 요청에 실패했습니다')
    } finally { setLoading(false) }
  }

  async function save() {
    if (busy || saved) return
    setBusy(true)
    try {
      if (result.source?.origin === 'DB' && result.source.recipeId) await addRecipeWish(result.source.recipeId)
      else await saveAiRecipe({ title: result.title, summary: result.summary || null, ingredientsJson: result.ingredients, stepsJson: result.steps, cookTime: result.meta?.cookTime ?? null })
      setSaved(true); Alert.alert('완료', '찜에 저장했어요')
    } catch (e) {
      if (e.response?.status === 409) { setSaved(true); Alert.alert('알림', '이미 찜한 레시피예요') }
      else Alert.alert('실패', e.response?.data?.message || '저장에 실패했어요')
    } finally { setBusy(false) }
  }

  async function register() {
    if (busy || registered) return
    setBusy(true)
    try {
      await registerFromAi({ title: result.title, summary: result.summary || null, ingredientsJson: result.ingredients, stepsJson: result.steps, cookTime: result.meta?.cookTime ?? null })
      setRegistered(true); Alert.alert('완료', '마이 레시피에 담았어요. 마이페이지에서 공개할 수 있어요')
    } catch (e) { Alert.alert('실패', e.response?.data?.message || '담기에 실패했어요') }
    finally { setBusy(false) }
  }

  const isDB = result?.source?.origin === 'DB'

  return (
    <ScrollView style={styles.wrap} contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
      <Text style={styles.lead}>냉장고 재료로 만들 수 있는 레시피를 추천해 드려요.</Text>

      <View style={styles.opts}>
        <Text style={styles.optsTitle}>추천 옵션</Text>
        {OPTIONS.map(([k, label]) => (
          <Pressable key={k} style={styles.optRow} onPress={() => setOpts((o) => ({ ...o, [k]: !o[k] }))}>
            <Ionicons name={opts[k] ? 'checkbox' : 'square-outline'} size={20} color={opts[k] ? colors.primaryDeep : '#bbb'} />
            <Text style={styles.optLabel}>{label}</Text>
          </Pressable>
        ))}
      </View>

      <Pressable style={[styles.run, loading && { opacity: 0.6 }]} disabled={loading} onPress={run}>
        <Text style={styles.runT}>{loading ? '추천 받는 중…' : result ? '다시 추천 받기' : '추천 받기'}</Text>
      </Pressable>

      {loading && <View style={styles.loadingBox}><ActivityIndicator color={colors.primary} /><Text style={styles.muted}>AI가 레시피를 만드는 중…</Text></View>}
      {!!error && <Text style={styles.err}>⚠️ {error}</Text>}

      {result && !!result.title && (
        <View style={styles.result}>
          <View style={[styles.badge, isDB ? styles.badgeDb : styles.badgeAi]}>
            <Text style={[styles.badgeT, { color: isDB ? '#2563eb' : '#7c3aed' }]}>{isDB ? '보유 재료 기반 추천' : 'AI 생성 레시피'}</Text>
          </View>
          <Text style={styles.title}>{result.title}</Text>
          {!!result.summary && <Text style={styles.summary}>{result.summary}</Text>}

          {result.ingredients.length > 0 && <Text style={styles.sec}>재료</Text>}
          {result.ingredients.map((i, idx) => (
            <View key={idx} style={[styles.ing, i.owned && styles.ingOwned]}>
              <Text style={styles.ingName}>{i.owned ? '✓ ' : ''}{i.name} <Text style={styles.ingQty}>{i.qty}{i.unit || ''}</Text></Text>
              <View style={[styles.tag, i.owned ? styles.tagY : styles.tagN]}><Text style={[styles.tagT, { color: i.owned ? colors.primaryDeep : '#f59e0b' }]}>{i.owned ? '보유' : '구매'}</Text></View>
            </View>
          ))}

          {result.steps.length > 0 && <Text style={styles.sec}>조리 순서</Text>}
          {result.steps.map((s) => (
            <View key={s.stepNumber} style={styles.step}>
              <View style={styles.stepN}><Text style={styles.stepNT}>{s.stepNumber}</Text></View>
              <Text style={styles.stepD}>{s.description}</Text>
            </View>
          ))}

          {!!result.meta && (
            <View style={styles.meta}>
              {!!result.meta.cookTime && <Text style={styles.metaT}><Ionicons name="time-outline" size={12} color={colors.primary} /> {result.meta.cookTime}분</Text>}
              {!!result.meta.difficulty && <Text style={styles.metaT}><Ionicons name="trending-up-outline" size={12} color={colors.primary} /> {result.meta.difficulty}</Text>}
              {!!result.meta.servings && <Text style={styles.metaT}><Ionicons name="person-outline" size={12} color={colors.primary} /> {result.meta.servings}인분</Text>}
            </View>
          )}

          <View style={styles.actions}>
            {isDB && <Pressable style={styles.ghost} onPress={() => navigation.navigate('RecipeDetail', { recipeId: result.source.recipeId })}><Text style={styles.ghostT}>상세 보기</Text></Pressable>}
            <Pressable style={styles.saveBtn} disabled={busy || saved} onPress={save}><Text style={styles.saveT}>{saved ? '✓ 찜됨' : '♡ 찜 저장'}</Text></Pressable>
            {!isDB && <Pressable style={styles.regBtn} disabled={busy || registered} onPress={register}><Text style={styles.regT}>{registered ? '✓ 담음' : '＋ 내 레시피로'}</Text></Pressable>}
          </View>
          {!isDB && <Text style={styles.regNote}>＊ "담기"는 마이 레시피에 보관돼요. 공개는 마이페이지에서 합니다.</Text>}
        </View>
      )}
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: colors.surfaceBg },
  lead: { color: '#777', fontSize: 14, marginBottom: 16 },
  opts: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 14, padding: 18, gap: 4, ...cardShadow },
  optsTitle: { fontSize: 15, fontWeight: '700', color: '#333', marginBottom: 6 },
  optRow: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingVertical: 6 },
  optLabel: { fontSize: 14, color: '#444' },
  run: { marginTop: 14, backgroundColor: colors.primary, borderRadius: 10, paddingVertical: 14, alignItems: 'center' },
  runT: { color: colors.onPrimary, fontSize: 15, fontWeight: '700' },
  loadingBox: { alignItems: 'center', gap: 10, marginTop: 30 },
  muted: { color: '#999', fontSize: 13 },
  err: { color: '#e11d48', fontSize: 14, marginTop: 16 },
  result: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 14, padding: 18, marginTop: 16, ...cardShadow },
  badge: { alignSelf: 'flex-start', borderRadius: radius.pill, paddingHorizontal: 10, paddingVertical: 5, marginBottom: 8 },
  badgeDb: { backgroundColor: '#eff6ff' },
  badgeAi: { backgroundColor: '#f5f3ff' },
  badgeT: { fontSize: 12, fontWeight: '700' },
  title: { fontSize: 19, fontWeight: '800', color: colors.text },
  summary: { color: '#666', fontSize: 14, marginTop: 6 },
  sec: { fontSize: 13, color: '#999', marginTop: 16, marginBottom: 8 },
  ing: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingHorizontal: 10, paddingVertical: 9, marginBottom: 6 },
  ingOwned: { borderColor: '#bbf7d0' },
  ingName: { fontSize: 14, color: colors.text },
  ingQty: { color: '#999', fontSize: 13 },
  tag: { borderRadius: radius.pill, paddingHorizontal: 8, paddingVertical: 2 },
  tagY: { backgroundColor: colors.primaryTint },
  tagN: { backgroundColor: '#fff7ed' },
  tagT: { fontSize: 11, fontWeight: '700' },
  step: { flexDirection: 'row', gap: 10, marginBottom: 10 },
  stepN: { width: 24, height: 24, borderRadius: 12, backgroundColor: colors.primaryTint, alignItems: 'center', justifyContent: 'center' },
  stepNT: { color: colors.primaryDeep, fontWeight: '700', fontSize: 12 },
  stepD: { flex: 1, fontSize: 14, lineHeight: 20 },
  meta: { flexDirection: 'row', gap: 14, marginTop: 12 },
  metaT: { color: '#888', fontSize: 13 },
  actions: { flexDirection: 'row', gap: 8, marginTop: 16 },
  ghost: { flex: 1, borderWidth: 1, borderColor: colors.primaryDeep, borderRadius: 8, paddingVertical: 11, alignItems: 'center' },
  ghostT: { color: colors.primaryDeep, fontSize: 14, fontWeight: '600' },
  saveBtn: { flex: 1, borderWidth: 1, borderColor: colors.primaryDeep, borderRadius: 8, paddingVertical: 11, alignItems: 'center', backgroundColor: '#fff' },
  saveT: { color: colors.primaryDeep, fontSize: 14, fontWeight: '700' },
  regBtn: { flex: 1, backgroundColor: colors.primary, borderRadius: 8, paddingVertical: 11, alignItems: 'center' },
  regT: { color: colors.onPrimary, fontSize: 14, fontWeight: '700' },
  regNote: { fontSize: 12, color: '#999', marginTop: 8 },
})
