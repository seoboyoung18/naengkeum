import { useState } from 'react'
import { View, Text, TextInput, Pressable, ScrollView, StyleSheet, KeyboardAvoidingView, Platform } from 'react-native'
import { Ionicons } from '@expo/vector-icons'
import { register as registerApi, checkEmail as checkEmailApi } from '../api/auth'
import { useAuth } from '../stores/auth'
import { colors, radius } from '../theme'

const emailValid = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)
const pwValid = (v) => v.length >= 8 && /[A-Za-z]/.test(v) && /\d/.test(v) && /[^A-Za-z0-9]/.test(v)
const nickValid = (v) => v.length >= 2 && v.length <= 10

export default function RegisterScreen() {
  const login = useAuth((s) => s.login)
  const [email, setEmail] = useState('')
  const [pw, setPw] = useState('')
  const [pw2, setPw2] = useState('')
  const [nickname, setNickname] = useState('')
  const [allergies, setAllergies] = useState('')
  const [marketing, setMarketing] = useState(false)
  const [emailAvailable, setEmailAvailable] = useState(null) // null|true|false
  const [checking, setChecking] = useState(false)
  const [loading, setLoading] = useState(false)
  const [err, setErr] = useState('')

  function onEmailChange(v) { setEmail(v); setEmailAvailable(null) }

  async function onCheck() {
    setErr('')
    if (!emailValid(email)) { setErr('올바른 이메일 형식이 아닙니다'); return }
    setChecking(true)
    try {
      const { available } = await checkEmailApi(email.trim())
      setEmailAvailable(available)
    } catch (e) { setErr(e.response?.data?.message || '이메일 확인 실패') }
    finally { setChecking(false) }
  }

  async function onSubmit() {
    setErr('')
    if (!emailValid(email)) return setErr('올바른 이메일 형식이 아닙니다')
    if (emailAvailable !== true) return setErr('이메일 중복확인을 해주세요')
    if (!pwValid(pw)) return setErr('비밀번호는 영문·숫자·특수문자 포함 8자 이상')
    if (pw !== pw2) return setErr('비밀번호가 일치하지 않습니다')
    if (!nickValid(nickname)) return setErr('닉네임은 2~10자여야 합니다')
    setLoading(true)
    try {
      const list = allergies.split(',').map((s) => s.trim()).filter(Boolean)
      await registerApi({ email: email.trim(), password: pw, nickname: nickname.trim(), allergies: list, marketingAgree: marketing })
      await login(email.trim(), pw) // 자동 로그인 → App이 메인 탭으로 전환
    } catch (e) {
      setErr(e.response?.data?.message || '회원가입에 실패했습니다')
    } finally { setLoading(false) }
  }

  return (
    <KeyboardAvoidingView style={styles.wrap} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        <Text style={styles.label}>이메일</Text>
        <View style={styles.row}>
          <TextInput style={[styles.input, { flex: 1 }]} value={email} onChangeText={onEmailChange} placeholder="test@email.com" autoCapitalize="none" keyboardType="email-address" placeholderTextColor="#9AA0A6" />
          <Pressable style={styles.check} disabled={checking} onPress={onCheck}><Text style={styles.checkT}>{checking ? '확인중' : '중복확인'}</Text></Pressable>
        </View>
        {emailAvailable === true && <Text style={styles.hintOk}>✓ 사용 가능한 이메일입니다</Text>}
        {emailAvailable === false && <Text style={styles.hintBad}>이미 사용 중인 이메일입니다</Text>}

        <Text style={styles.label}>비밀번호</Text>
        <TextInput style={styles.input} value={pw} onChangeText={setPw} placeholder="영문+숫자+특수문자 8자 이상" secureTextEntry placeholderTextColor="#9AA0A6" />
        <Text style={styles.label}>비밀번호 확인</Text>
        <TextInput style={styles.input} value={pw2} onChangeText={setPw2} placeholder="비밀번호 재입력" secureTextEntry placeholderTextColor="#9AA0A6" />
        <Text style={styles.label}>닉네임</Text>
        <TextInput style={styles.input} value={nickname} onChangeText={setNickname} placeholder="2~10자" placeholderTextColor="#9AA0A6" />
        <Text style={styles.label}>알레르기 (선택)</Text>
        <TextInput style={styles.input} value={allergies} onChangeText={setAllergies} placeholder="콤마로 구분  예: 계란, 우유" placeholderTextColor="#9AA0A6" />

        <Pressable style={styles.checkRow} onPress={() => setMarketing(!marketing)}>
          <Ionicons name={marketing ? 'checkbox' : 'square-outline'} size={18} color={marketing ? colors.primaryDeep : '#bbb'} />
          <Text style={styles.checkRowT}>마케팅 정보 수신 동의 (선택)</Text>
        </Pressable>

        {!!err && <Text style={styles.err}>{err}</Text>}

        <Pressable style={[styles.submit, loading && { opacity: 0.6 }]} disabled={loading} onPress={onSubmit}>
          <Text style={styles.submitT}>{loading ? '가입 중…' : '회원가입'}</Text>
        </Pressable>
      </ScrollView>
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: colors.surfaceBg },
  scroll: { padding: 20, paddingBottom: 40 },
  label: { fontSize: 13, color: '#555', marginTop: 12, marginBottom: 6 },
  input: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.md, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, backgroundColor: '#fff', color: colors.text },
  row: { flexDirection: 'row', gap: 8 },
  check: { borderWidth: 1, borderColor: colors.primaryDeep, borderRadius: radius.md, paddingHorizontal: 14, justifyContent: 'center', backgroundColor: '#fff' },
  checkT: { color: colors.primaryDeep, fontWeight: '600', fontSize: 13 },
  hintOk: { color: colors.primaryDeep, fontSize: 12, marginTop: 6 },
  hintBad: { color: '#e11d48', fontSize: 12, marginTop: 6 },
  checkRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 16 },
  checkRowT: { fontSize: 13, color: '#444' },
  err: { color: '#e11d48', fontSize: 13, marginTop: 12 },
  submit: { marginTop: 18, backgroundColor: colors.primary, borderRadius: radius.md, paddingVertical: 13, alignItems: 'center' },
  submitT: { color: colors.onPrimary, fontSize: 15, fontWeight: '700' },
})
