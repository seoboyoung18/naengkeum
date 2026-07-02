import { useState } from 'react'
import {
  View, Text, TextInput, Pressable, StyleSheet,
  KeyboardAvoidingView, Platform, ScrollView,
} from 'react-native'
import { Ionicons } from '@expo/vector-icons'
import { useAuth } from '../stores/auth'
import { colors, radius, cardShadow } from '../theme'

export default function LoginScreen({ navigation }) {
  const login = useAuth((s) => s.login)
  const socialLogin = useAuth((s) => s.socialLogin)
  const [email, setEmail] = useState('')
  const [pw, setPw] = useState('')
  const [remember, setRemember] = useState(false)
  const [loading, setLoading] = useState(false)
  const [social, setSocial] = useState('')   // 진행 중인 소셜 provider
  const [err, setErr] = useState('')

  async function onSubmit() {
    setErr('')
    setLoading(true)
    try {
      await login(email.trim(), pw, remember)
      // 로그인 성공 → App.js가 token 변화를 감지해 자동으로 메인 탭으로 전환
    } catch (e) {
      setErr(e.response?.data?.message || '로그인에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  async function onSocial(provider) {
    setErr('')
    setSocial(provider)
    try {
      await socialLogin(provider)
      // 성공 시 App.js가 token 변화를 감지해 자동으로 메인 탭 전환. 취소면 null → 화면 유지.
    } catch (e) {
      setErr(e.message || '소셜 로그인에 실패했습니다')
    } finally {
      setSocial('')
    }
  }

  return (
    <KeyboardAvoidingView style={styles.wrap} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        <View style={styles.card}>
          <Text style={styles.brand}>냉큼</Text>
          <Text style={styles.sub}>남은 재료로 큼지막한 행복을</Text>

          <Text style={styles.label}>이메일</Text>
          <TextInput
            style={styles.input}
            value={email}
            onChangeText={setEmail}
            placeholder="test@email.com"
            autoCapitalize="none"
            keyboardType="email-address"
            placeholderTextColor="#9AA0A6"
          />

          <Text style={styles.label}>비밀번호</Text>
          <TextInput
            style={styles.input}
            value={pw}
            onChangeText={setPw}
            placeholder="비밀번호"
            secureTextEntry
            placeholderTextColor="#9AA0A6"
          />

          <Pressable style={styles.remember} onPress={() => setRemember(!remember)}>
            <Ionicons name={remember ? 'checkbox' : 'square-outline'} size={18} color={remember ? colors.primaryDeep : '#bbb'} />
            <Text style={styles.rememberT}>로그인 유지</Text>
          </Pressable>

          {!!err && <Text style={styles.err}>{err}</Text>}

          <Pressable style={[styles.submit, loading && { opacity: 0.6 }]} onPress={onSubmit} disabled={loading}>
            <Text style={styles.submitT}>{loading ? '로그인 중…' : '로그인'}</Text>
          </Pressable>

          <Pressable onPress={() => navigation.navigate('Register')} style={styles.linkBtn}>
            <Text style={styles.link}>회원가입</Text>
          </Pressable>

          <View style={styles.divider}>
            <View style={styles.line} />
            <Text style={styles.or}>또는</Text>
            <View style={styles.line} />
          </View>

          <Pressable style={[styles.social, { backgroundColor: '#FEE500' }, social && { opacity: 0.6 }]} onPress={() => onSocial('kakao')} disabled={!!social}>
            <Text style={[styles.socialT, { color: '#191600' }]}>{social === 'kakao' ? '카카오 로그인 중…' : '카카오로 시작하기'}</Text>
          </Pressable>
          <Pressable style={[styles.social, { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line }, social && { opacity: 0.6 }]} onPress={() => onSocial('google')} disabled={!!social}>
            <Text style={[styles.socialT, { color: '#3C4043' }]}>{social === 'google' ? 'Google 로그인 중…' : 'Google로 시작하기'}</Text>
          </Pressable>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: colors.surfaceBg },
  scroll: { flexGrow: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  card: { width: '100%', maxWidth: 380, backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: 16, padding: 24, ...cardShadow },
  brand: { fontSize: 30, fontWeight: '800', color: colors.primaryDeep, textAlign: 'center' },
  sub: { fontSize: 13, color: '#888', textAlign: 'center', marginTop: 4, marginBottom: 14 },
  label: { fontSize: 13, color: '#555', marginTop: 12, marginBottom: 6 },
  input: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.md, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: colors.text },
  remember: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 14 },
  rememberT: { fontSize: 13, color: '#444' },
  err: { color: '#e11d48', fontSize: 13, marginTop: 12 },
  submit: { marginTop: 18, backgroundColor: colors.primary, borderRadius: radius.md, paddingVertical: 13, alignItems: 'center' },
  submitT: { color: colors.onPrimary, fontSize: 15, fontWeight: '700' },
  linkBtn: { marginTop: 14, alignItems: 'center' },
  link: { color: colors.primaryDeep, fontSize: 13, fontWeight: '600' },
  divider: { flexDirection: 'row', alignItems: 'center', marginVertical: 16 },
  line: { flex: 1, height: 1, backgroundColor: colors.lineSoft },
  or: { paddingHorizontal: 12, color: '#aaa', fontSize: 12 },
  social: { borderRadius: radius.md, paddingVertical: 13, alignItems: 'center', marginTop: 10 },
  socialT: { fontSize: 14, fontWeight: '600' },
})
