import { useEffect } from 'react'
import { View, ActivityIndicator, StyleSheet } from 'react-native'
import { StatusBar } from 'expo-status-bar'
import { SafeAreaProvider } from 'react-native-safe-area-context'
import { NavigationContainer } from '@react-navigation/native'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs'
import { Ionicons } from '@expo/vector-icons'

import { useAuth } from './src/stores/auth'
import { colors } from './src/theme'
import LoginScreen from './src/screens/LoginScreen'
import RegisterScreen from './src/screens/RegisterScreen'
import HomeScreen from './src/screens/HomeScreen'
import FridgeScreen from './src/screens/FridgeScreen'
import RecipeListScreen from './src/screens/RecipeListScreen'
import RecipeDetailScreen from './src/screens/RecipeDetailScreen'
import MyPageScreen from './src/screens/MyPageScreen'
import ChallengeScreen from './src/screens/ChallengeScreen'
import ChallengeDetailScreen from './src/screens/ChallengeDetailScreen'
import UserProfileScreen from './src/screens/UserProfileScreen'
import FollowListScreen from './src/screens/FollowListScreen'

const Tab = createBottomTabNavigator()
const AuthStack = createNativeStackNavigator()
const RootStack = createNativeStackNavigator()

const TAB_ICON = { 홈: 'home', 냉장고: 'cube', 레시피: 'restaurant', 챌린지: 'trophy', 마이: 'person' }

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
      <Tab.Screen name="챌린지" component={ChallengeScreen} />
      <Tab.Screen name="마이" component={MyPageScreen} />
    </Tab.Navigator>
  )
}

const headerOpts = {
  headerShown: true,
  headerTintColor: colors.primaryDeep,
  headerTitleStyle: { color: colors.text },
  headerStyle: { backgroundColor: '#fff' },
}

function MainNavigator() {
  return (
    <RootStack.Navigator>
      <RootStack.Screen name="Tabs" component={MainTabs} options={{ headerShown: false }} />
      <RootStack.Screen name="RecipeDetail" component={RecipeDetailScreen} options={{ ...headerOpts, title: '레시피' }} />
      <RootStack.Screen name="ChallengeDetail" component={ChallengeDetailScreen} options={{ ...headerOpts, title: '챌린지' }} />
      <RootStack.Screen name="UserProfile" component={UserProfileScreen} options={{ ...headerOpts, title: '프로필' }} />
      <RootStack.Screen name="FollowList" component={FollowListScreen} options={{ ...headerOpts, title: '팔로우' }} />
    </RootStack.Navigator>
  )
}

function AuthNavigator() {
  return (
    <AuthStack.Navigator>
      <AuthStack.Screen name="Login" component={LoginScreen} options={{ headerShown: false }} />
      <AuthStack.Screen name="Register" component={RegisterScreen} options={{ ...headerOpts, title: '회원가입' }} />
    </AuthStack.Navigator>
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
          {token ? <MainNavigator /> : <AuthNavigator />}
        </NavigationContainer>
      )}
    </SafeAreaProvider>
  )
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.surfaceBg, gap: 10 },
})
