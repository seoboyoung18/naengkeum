import { useEffect, useMemo, useState } from 'react'
import { Modal, View, Text, Pressable, ScrollView, StyleSheet, Alert, ActivityIndicator } from 'react-native'
import { getSeasonings, saveSeasonings } from '../api/seasoning'
import { colors, radius } from '../theme'

const GROUPS = [
  { key: 'FRIDGE', label: '냉장' },
  { key: 'FREEZER', label: '냉동' },
  { key: 'ROOM_TEMP', label: '실온' },
]

/**
 * 조미료 선택 모달 — 전체 카탈로그를 보관위치별로 묶어 칩으로 토글하고,
 * 저장 시 보유(selected) id 집합을 PUT /api/seasonings로 갱신한다.
 */
export default function SeasoningModal({ visible, onClose, onSaved }) {
  const [catalog, setCatalog] = useState([])
  const [selected, setSelected] = useState(() => new Set())
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!visible) return
    setLoading(true)
    getSeasonings()
      .then((list) => {
        setCatalog(list || [])
        setSelected(new Set((list || []).filter((s) => s.owned).map((s) => s.seasoningId)))
      })
      .catch((e) => Alert.alert('실패', e.response?.data?.message || '조미료를 불러오지 못했어요'))
      .finally(() => setLoading(false))
  }, [visible])

  const grouped = useMemo(
    () => GROUPS.map((g) => ({ ...g, items: catalog.filter((s) => s.storageType === g.key) })).filter((g) => g.items.length),
    [catalog],
  )

  function toggle(id) {
    setSelected((prev) => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })
  }

  async function submit() {
    setSaving(true)
    try {
      await saveSeasonings([...selected])
      onSaved?.()
      onClose?.()
    } catch (e) {
      Alert.alert('실패', e.response?.data?.message || '저장에 실패했어요')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.sheet}>
          <View style={styles.grab} />
          <Text style={styles.title}>내 조미료 선택</Text>
          <Text style={styles.desc}>가지고 있는 조미료를 켜두면 AI 추천에서 "사야 할 재료"에서 제외돼요.</Text>

          {loading ? (
            <ActivityIndicator color={colors.primary} style={{ marginVertical: 40 }} />
          ) : (
            <ScrollView contentContainerStyle={{ paddingBottom: 8 }}>
              {grouped.map((g) => (
                <View key={g.key} style={{ marginTop: 14 }}>
                  <Text style={styles.grpLabel}>{g.label}</Text>
                  <View style={styles.chips}>
                    {g.items.map((s) => {
                      const on = selected.has(s.seasoningId)
                      return (
                        <Pressable key={s.seasoningId} style={[styles.chip, on && styles.chipOn]} onPress={() => toggle(s.seasoningId)}>
                          <Text style={[styles.chipT, on && styles.chipTOn]}>{on ? '✓ ' : ''}{s.name}</Text>
                        </Pressable>
                      )
                    })}
                  </View>
                </View>
              ))}
            </ScrollView>
          )}

          <View style={styles.actions}>
            <Pressable style={[styles.btn, styles.cancel]} onPress={onClose} disabled={saving}>
              <Text style={styles.cancelT}>취소</Text>
            </Pressable>
            <Pressable style={[styles.btn, styles.save, saving && { opacity: 0.6 }]} onPress={submit} disabled={saving || loading}>
              {saving ? <ActivityIndicator color={colors.onPrimary} /> : <Text style={styles.saveT}>저장 ({selected.size})</Text>}
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  )
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.35)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, paddingBottom: 28, maxHeight: '85%' },
  grab: { alignSelf: 'center', width: 40, height: 4, borderRadius: 2, backgroundColor: '#D1D5DB', marginBottom: 12 },
  title: { fontSize: 18, fontWeight: '800', color: colors.text },
  desc: { fontSize: 12.5, color: colors.textSoft, marginTop: 6, lineHeight: 18 },
  grpLabel: { fontSize: 13, fontWeight: '700', color: colors.primaryDeep, marginBottom: 8 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.pill, paddingHorizontal: 13, paddingVertical: 7, backgroundColor: '#fff' },
  chipOn: { borderColor: colors.primaryDeep, backgroundColor: colors.primaryTint },
  chipT: { fontSize: 13, color: '#555' },
  chipTOn: { color: colors.primaryDeep, fontWeight: '700' },
  actions: { flexDirection: 'row', gap: 10, marginTop: 18 },
  btn: { flex: 1, borderRadius: radius.md, paddingVertical: 13, alignItems: 'center', justifyContent: 'center' },
  cancel: { backgroundColor: '#F1F3F5' },
  cancelT: { color: '#495057', fontWeight: '700', fontSize: 15 },
  save: { backgroundColor: colors.primary },
  saveT: { color: colors.onPrimary, fontWeight: '800', fontSize: 15 },
})
