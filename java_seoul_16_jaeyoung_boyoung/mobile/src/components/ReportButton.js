import { useState } from 'react'
import { View, Text, Pressable, Modal, TextInput, StyleSheet, Alert } from 'react-native'
import { createReport } from '../api/report'
import { colors, radius } from '../theme'

export default function ReportButton({ targetType, targetId, label = '신고', compact = false }) {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const targetLabel = targetType === 'REVIEW' ? '리뷰' : '레시피'

  async function submit() {
    setSubmitting(true)
    try {
      await createReport({ targetType, targetId: Number(targetId), reason: reason.trim() || undefined })
      setOpen(false)
      setReason('')
      Alert.alert('신고 접수', '검토 후 조치할게요.')
    } catch (e) {
      setOpen(false)
      if (e.response?.status === 409) Alert.alert('알림', `이미 신고한 ${targetLabel}예요`)
      else Alert.alert('실패', e.response?.data?.message || '신고에 실패했어요')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <Pressable onPress={() => { setReason(''); setOpen(true) }} style={compact ? null : styles.trigger}>
        <Text style={compact ? styles.compactT : styles.triggerT}>🚩 {label}</Text>
      </Pressable>
      <Modal visible={open} transparent animationType="fade" onRequestClose={() => !submitting && setOpen(false)}>
        <View style={styles.overlay}>
          <View style={styles.modal}>
            <Text style={styles.title}>{targetLabel} 신고</Text>
            <Text style={styles.sub}>신고 사유를 알려주시면 검토에 도움이 돼요. (선택)</Text>
            <TextInput
              style={styles.input}
              value={reason}
              onChangeText={setReason}
              multiline
              maxLength={255}
              placeholder="예) 욕설/광고/허위 정보 등"
              placeholderTextColor="#9AA0A6"
            />
            <View style={styles.actions}>
              <Pressable style={styles.ghost} disabled={submitting} onPress={() => setOpen(false)}><Text style={styles.ghostT}>취소</Text></Pressable>
              <Pressable style={styles.danger} disabled={submitting} onPress={submit}><Text style={styles.dangerT}>{submitting ? '접수 중…' : '신고하기'}</Text></Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </>
  )
}

const styles = StyleSheet.create({
  trigger: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.sm, paddingHorizontal: 14, paddingVertical: 9, backgroundColor: '#fff' },
  triggerT: { color: colors.textSoft, fontSize: 13, fontWeight: '600' },
  compactT: { color: '#aaa', fontSize: 12 },
  overlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.45)', alignItems: 'center', justifyContent: 'center', padding: 20 },
  modal: { width: '100%', maxWidth: 380, backgroundColor: '#fff', borderRadius: radius.md, padding: 22 },
  title: { fontSize: 18, fontWeight: '700', marginBottom: 6 },
  sub: { fontSize: 13, color: colors.textSoft, marginBottom: 14 },
  input: { borderWidth: 1, borderColor: colors.line, borderRadius: 8, padding: 10, fontSize: 14, minHeight: 72, textAlignVertical: 'top' },
  actions: { flexDirection: 'row', gap: 8, justifyContent: 'flex-end', marginTop: 14 },
  ghost: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.sm, paddingHorizontal: 18, paddingVertical: 10 },
  ghostT: { fontSize: 14, fontWeight: '700', color: colors.text },
  danger: { backgroundColor: '#e11d48', borderRadius: radius.sm, paddingHorizontal: 18, paddingVertical: 10 },
  dangerT: { fontSize: 14, fontWeight: '700', color: '#fff' },
})
