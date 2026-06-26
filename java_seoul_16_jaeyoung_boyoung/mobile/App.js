import { StatusBar } from 'expo-status-bar'
import { NavigationContainer } from '@react-navigation/native'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import { View, Text, StyleSheet } from 'react-native'

const Stack = createNativeStackNavigator()

// TODO: Vue 버전(frontend/src/views)의 화면을 src/screens 로 포팅해 연결
function PlaceholderScreen() {
  return (
    <View style={styles.center}>
      <Text style={styles.brand}>냉큼</Text>
      <Text style={styles.sub}>React Native 버전 — 준비 중</Text>
    </View>
  )
}

export default function App() {
  return (
    <NavigationContainer>
      <StatusBar style="dark" />
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        <Stack.Screen name="Home" component={PlaceholderScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  )
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#F7F8FA' },
  brand: { fontSize: 40, fontWeight: '800', color: '#10B981' },
  sub: { marginTop: 8, fontSize: 14, color: '#8B95A1' },
})
