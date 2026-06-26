import { useEffect } from 'react'
import { View, Text, ActivityIndicator, StyleSheet } from 'react-native'
import { StatusBar } from 'expo-status-bar'
import { SafeAreaProvider } from 'react-native-safe-area-context'
import { NavigationContainer } from '@react-navigation/native'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs'
import { Ionicons } from '@expo/vector-icons'

import { useAuth } from './src/stores/auth'
import { colors } from './src/theme'
import LoginScreen from './src/screens/LoginScreen'
import HomeScreen from './src/screens/HomeScreen'
import FridgeScreen from './src/screens/FridgeScreen'
import RecipeListScreen from './src/screens/RecipeListScreen'

const Tab = createBottomTabNavigator()
const Stack = createNativeStackNavigator()

const TAB_ICON = { 홈: 'home', 냉장고: 'cube', 레시피: 'restaurant', 마이: 'person' }

function MyPlaceholder() {
  return (
    <View style={styles.center}>
      <Ionicons name="construct-outline" size={36} color={colors.mute} />
      <Text style={styles.soon}>마이페이지 — 다음 단계에서 추가됩니다</Text>
    </View>
  )
}

function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: colors.primaryDeep,
        tabBarInactiveTintColor: '#9AA0A6',
        tabBarIcon: ({ color, size, focused }) => {
          const base = TAB_ICON[route.name] || 'ellipse'
          return <Ionicons name={focused ? base : `${base}-outline`} size={size} color={color} />
        },
      })}
    >
      <Tab.Screen name="홈" component={HomeScreen} />
      <Tab.Screen name="냉장고" component={FridgeScreen} />
      <Tab.Screen name="레시피" component={RecipeListScreen} />
      <Tab.Screen name="마이" component={MyPlaceholder} />
    </Tab.Navigator>
  )
}

export default function App() {
  const ready = useAuth((s) => s.ready)
  const token = useAuth((s) => s.token)
  const restore = useAuth((s) => s.restore)

  useEffect(() => {
    restore()
  }, [restore])

  return (
    <SafeAreaProvider>
      <StatusBar style="dark" />
      {!ready ? (
        <View style={styles.center}>
          <ActivityIndicator color={colors.primary} size="large" />
        </View>
      ) : (
        <NavigationContainer>
          {token ? (
            <MainTabs />
          ) : (
            <Stack.Navigator screenOptions={{ headerShown: false }}>
              <Stack.Screen name="Login" component={LoginScreen} />
            </Stack.Navigator>
          )}
        </NavigationContainer>
      )}
    </SafeAreaProvider>
  )
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.surfaceBg, gap: 10 },
  soon: { fontSize: 14, color: colors.textSoft },
})
