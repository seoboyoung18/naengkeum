import { useEffect, useState } from 'react'
import { Modal, View, Text, TextInput, Pressable, ScrollView, StyleSheet, Alert, ActivityIndicator, KeyboardAvoidingView, Platform } from 'react-native'
import { createFridgeItem, updateFridgeItem } from '../api/fridge'
import { colors, radius } from '../theme'

const STORAGES = [
  { key: 'FRIDGE', label: '냉장' },
  { key: 'FREEZER', label: '냉동' },
  { key: 'ROOM_TEMP', label: '실온' },
]

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/

/**
 * 냉장고 재료 추가/수정 모달.
 * item=null이면 등록, 있으면 수정. onSaved()로 목록 새로고침을 알린다.
 * 등록/수정 본문 동일(FridgeItemRequest): name·qty·unit·storageType·expiryDate·memo.
 */
export default function FridgeItemModal({ visible, item, onClose, onSaved }) {
  const editing = !!item
  const [name, setName] = useState('')
  const [qty, setQty] = useState('')
  const [unit, setUnit] = useState('')
  const [storageType, setStorageType] = useState('FRIDGE')
  const [expiryDate, setExpiryDate] = useState('')
  const [memo, setMemo] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!visible) return
    setName(item?.name ?? '')
    setQty(item?.qty != null ? String(item.qty) : '')
    setUnit(item?.unit ?? '')
    setStorageType(item?.storageType ?? 'FRIDGE')
    setExpiryDate(item?.expiryDate ?? '')
    setMemo(item?.memo ?? '')
    setSaving(false)
  }, [visible, item])

  async function submit() {
    const n = name.trim()
    if (!n) return Alert.alert('확인', '재료명을 입력해 주세요')
    const q = Number(qty)
    if (qty === '' || Number.isNaN(q) || q < 0) return Alert.alert('확인', '수량은 0 이상 숫자여야 해요')
    if (!unit.trim()) return Alert.alert('확인', '단위를 입력해 주세요 (예: 개, g, ml)')
    if (!DATE_RE.test(expiryDate.trim())) return Alert.alert('확인', '유통기한을 YYYY-MM-DD 형식으로 입력해 주세요')

    const payload = {
      name: n,
      qty: q,
      unit: unit.trim(),
      storageType,
      expiryDate: expiryDate.trim(),
      memo: memo.trim() || null,
    }
    setSaving(true)
    try {
      if (editing) await updateFridgeItem(item.fridgeItemId, payload)
      else await createFridgeItem(payload)
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
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.backdrop}>
        <View style={styles.sheet}>
          <View style={styles.grab} />
          <Text style={styles.title}>{editing ? '재료 수정' : '재료 추가'}</Text>
          <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ paddingBottom: 8 }}>
            <Text style={styles.label}>재료명</Text>
            <TextInput style={styles.input} value={name} onChangeText={setName} placeholder="예: 계란" placeholderTextColor="#9AA0A6" maxLength={50} />

            <View style={styles.row2}>
              <View style={{ flex: 1 }}>
                <Text style={styles.label}>수량</Text>
                <TextInput style={styles.input} value={qty} onChangeText={setQty} placeholder="1" placeholderTextColor="#9AA0A6" keyboardType="decimal-pad" />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.label}>단위</Text>
                <TextInput style={styles.input} value={unit} onChangeText={setUnit} placeholder="개 / g / ml" placeholderTextColor="#9AA0A6" maxLength={10} />
              </View>
            </View>

            <Text style={styles.label}>보관 위치</Text>
            <View style={styles.seg}>
              {STORAGES.map((s) => (
                <Pressable key={s.key} style={[styles.segItem, storageType === s.key && styles.segOn]} onPress={() => setStorageType(s.key)}>
                  <Text style={[styles.segT, storageType === s.key && styles.segTOn]}>{s.label}</Text>
                </Pressable>
              ))}
            </View>

            <Text style={styles.label}>유통기한</Text>
            <TextInput style={styles.input} value={expiryDate} onChangeText={setExpiryDate} placeholder="YYYY-MM-DD" placeholderTextColor="#9AA0A6" keyboardType="numbers-and-punctuation" maxLength={10} />

            <Text style={styles.label}>메모 (선택)</Text>
            <TextInput style={styles.input} value={memo} onChangeText={setMemo} placeholder="예: 반개 사용함" placeholderTextColor="#9AA0A6" maxLength={100} />
          </ScrollView>

          <View style={styles.actions}>
            <Pressable style={[styles.btn, styles.cancel]} onPress={onClose} disabled={saving}>
              <Text style={styles.cancelT}>취소</Text>
            </Pressable>
            <Pressable style={[styles.btn, styles.save, saving && { opacity: 0.6 }]} onPress={submit} disabled={saving}>
              {saving ? <ActivityIndicator color={colors.onPrimary} /> : <Text style={styles.saveT}>{editing ? '수정' : '추가'}</Text>}
            </Pressable>
          </View>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  )
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.35)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, paddingBottom: 28, maxHeight: '88%' },
  grab: { alignSelf: 'center', width: 40, height: 4, borderRadius: 2, backgroundColor: '#D1D5DB', marginBottom: 12 },
  title: { fontSize: 18, fontWeight: '800', color: colors.text, marginBottom: 12 },
  label: { fontSize: 13, fontWeight: '700', color: colors.textSoft, marginTop: 12, marginBottom: 6 },
  input: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.md, paddingHorizontal: 12, paddingVertical: 10, fontSize: 15, color: colors.text, backgroundColor: '#fff' },
  row2: { flexDirection: 'row', gap: 12 },
  seg: { flexDirection: 'row', gap: 8 },
  segItem: { flex: 1, borderWidth: 1, borderColor: colors.line, borderRadius: radius.md, paddingVertical: 10, alignItems: 'center', backgroundColor: '#fff' },
  segOn: { borderColor: colors.primaryDeep, backgroundColor: colors.primaryTint },
  segT: { fontSize: 14, color: '#666' },
  segTOn: { color: colors.primaryDeep, fontWeight: '700' },
  actions: { flexDirection: 'row', gap: 10, marginTop: 18 },
  btn: { flex: 1, borderRadius: radius.md, paddingVertical: 13, alignItems: 'center', justifyContent: 'center' },
  cancel: { backgroundColor: '#F1F3F5' },
  cancelT: { color: '#495057', fontWeight: '700', fontSize: 15 },
  save: { backgroundColor: colors.primary },
  saveT: { color: colors.onPrimary, fontWeight: '800', fontSize: 15 },
})
