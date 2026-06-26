import { useState, useEffect } from 'react'
import { View, Text, Pressable, Modal, ScrollView, StyleSheet, ActivityIndicator } from 'react-native'
import { Ionicons } from '@expo/vector-icons'
import { fetchAiRecipe } from '../api/wishlist'
import { colors, radius } from '../theme'

export default function AiRecipeModal({ aiRecipeId, onClose }) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let alive = true
    fetchAiRecipe(aiRecipeId)
      .then((d) => alive && setData(d))
      .catch((e) => alive && setError(e.response?.data?.message || '불러오기 실패'))
      .finally(() => alive && setLoading(false))
    return () => { alive = false }
  }, [aiRecipeId])

  return (
    <Modal visible transparent animationType="slide" onRequestClose={onClose}>
      <View style={styles.overlay}>
        <View style={styles.sheet}>
          <View style={styles.hd}>
            <View style={styles.badge}><Text style={styles.badgeT}>AI 레시피</Text></View>
            <Pressable hitSlop={8} onPress={onClose}><Ionicons name="close" size={22} color="#888" /></Pressable>
          </View>
          {loading ? <ActivityIndicator color={colors.primary} style={{ marginVertical: 24 }} />
            : error ? <Text style={styles.err}>{error}</Text>
            : data && (
              <ScrollView style={{ maxHeight: 480 }}>
                <Text style={styles.title}>{data.title}</Text>
                {!!data.summary && <Text style={styles.summary}>{data.summary}</Text>}
                {!!data.cookTime && <Text style={styles.meta}><Ionicons name="time-outline" size={12} /> {data.cookTime}분</Text>}
                {data.ingredients?.length > 0 && <Text style={styles.sec}>재료</Text>}
                {(data.ingredients || []).map((i, idx) => (
                  <View key={idx} style={[styles.ing, i.owned && styles.ingOwned]}>
                    <Text style={styles.ingName}>{i.owned ? '✓ ' : ''}{i.name} <Text style={styles.ingQty}>{i.qty}{i.unit || ''}</Text></Text>
                    <View style={[styles.tag, { backgroundColor: i.owned ? colors.primaryTint : '#fff7ed' }]}><Text style={[styles.tagT, { color: i.owned ? colors.primaryDeep : '#f59e0b' }]}>{i.owned ? '보유' : '구매'}</Text></View>
                  </View>
                ))}
                {data.steps?.length > 0 && <Text style={styles.sec}>조리 순서</Text>}
                {(data.steps || []).map((s) => (
                  <View key={s.stepNumber} style={styles.step}>
                    <View style={styles.stepN}><Text style={styles.stepNT}>{s.stepNumber}</Text></View>
                    <Text style={styles.stepD}>{s.description}</Text>
                  </View>
                ))}
              </ScrollView>
            )}
        </View>
      </View>
    </Modal>
  )
}

const styles = StyleSheet.create({
  overlay: { flex: 1, backgroundColor: 'rgba(17,24,39,0.5)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 16, borderTopRightRadius: 16, padding: 22, paddingBottom: 34 },
  hd: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 },
  badge: { backgroundColor: '#f5f3ff', borderRadius: radius.pill, paddingHorizontal: 10, paddingVertical: 5 },
  badgeT: { color: '#7c3aed', fontSize: 12, fontWeight: '700' },
  err: { color: '#e11d48', paddingVertical: 16 },
  title: { fontSize: 19, fontWeight: '800', color: colors.text },
  summary: { color: '#666', fontSize: 14, marginTop: 4 },
  meta: { fontSize: 13, color: '#888', marginTop: 6 },
  sec: { fontSize: 13, color: '#999', marginTop: 16, marginBottom: 6 },
  ing: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', borderWidth: 1, borderColor: colors.line, borderRadius: 8, paddingHorizontal: 10, paddingVertical: 8, marginBottom: 6 },
  ingOwned: { borderColor: '#bbf7d0' },
  ingName: { fontSize: 14, color: colors.text },
  ingQty: { color: '#999', fontSize: 13 },
  tag: { borderRadius: radius.pill, paddingHorizontal: 8, paddingVertical: 2 },
  tagT: { fontSize: 11, fontWeight: '700' },
  step: { flexDirection: 'row', gap: 10, marginBottom: 10 },
  stepN: { width: 24, height: 24, borderRadius: 12, backgroundColor: colors.primaryTint, alignItems: 'center', justifyContent: 'center' },
  stepNT: { color: colors.primaryDeep, fontWeight: '700', fontSize: 12 },
  stepD: { flex: 1, fontSize: 14, lineHeight: 20 },
})
