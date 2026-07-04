import { View, Text, Image, Pressable, StyleSheet } from 'react-native'
import { Ionicons } from '@expo/vector-icons'
import { API_BASE } from '../config'
import { colors, radius, cardShadow } from '../theme'

const SOURCE = {
  PUBLIC: { label: '공공', bg: '#EEF2F7', fg: '#64748B' },
  USER: { label: '✍️ 직접', bg: '#FFF7ED', fg: '#C2410C' },
  AI_SAVED: { label: 'AI', bg: '#F5F3FF', fg: '#7C3AED' },
}

function imageUrl(u) {
  if (!u) return null
  return u.startsWith('/') ? API_BASE + u : u
}

export default function RecipeCard({ recipe, onOpen, onToggleWish, wishable = true, style }) {
  const src = SOURCE[recipe.source] || SOURCE.PUBLIC
  const img = imageUrl(recipe.thumbnailUrl)
  return (
    <Pressable style={[styles.card, style]} onPress={() => onOpen?.(recipe.recipeId)}>
      <View style={styles.thumb}>
        {img ? (
          <Image source={{ uri: img }} style={styles.img} />
        ) : (
          <Ionicons name="image-outline" size={36} color="#C7CCD1" />
        )}
        {wishable && (
          <Pressable style={styles.heart} hitSlop={8} onPress={() => onToggleWish?.(recipe)}>
            <Ionicons
              name={recipe.isWishlisted ? 'heart' : 'heart-outline'}
              size={18}
              color={recipe.isWishlisted ? colors.danger : '#bbb'}
            />
          </Pressable>
        )}
      </View>
      <View style={styles.info}>
        <View style={styles.trow}>
          <Text style={styles.title} numberOfLines={1}>
            {recipe.title}
          </Text>
          <View style={[styles.src, { backgroundColor: src.bg }]}>
            <Text style={[styles.srcT, { color: src.fg }]}>{src.label}</Text>
          </View>
        </View>
        <View style={styles.meta}>
          {!!recipe.cookTime && (
            <Text style={styles.metaT}>
              <Ionicons name="time-outline" size={12} color={colors.textSoft} /> {recipe.cookTime}분
            </Text>
          )}
          <Text style={styles.metaT}>
            <Ionicons name="star" size={12} color="#E4D50A" /> {Number(recipe.avgRating || 0).toFixed(1)}
            {recipe.reviewCount ? ` (${recipe.reviewCount})` : ''}
          </Text>
        </View>
        {!!recipe.mainIngredients?.length && (
          <Text style={styles.ings} numberOfLines={1}>
            {recipe.mainIngredients.join(' · ')}
          </Text>
        )}
      </View>
    </Pressable>
  )
}

const styles = StyleSheet.create({
  card: { backgroundColor: '#fff', borderWidth: 1, borderColor: colors.lineSoft, borderRadius: radius.card, overflow: 'hidden', ...cardShadow },
  thumb: { height: 120, backgroundColor: '#F1F3F5', alignItems: 'center', justifyContent: 'center' },
  img: { width: '100%', height: '100%' },
  heart: { position: 'absolute', top: 8, right: 8, width: 30, height: 30, borderRadius: 15, backgroundColor: 'rgba(255,255,255,0.9)', alignItems: 'center', justifyContent: 'center' },
  info: { padding: 12 },
  trow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  title: { flex: 1, fontSize: 14, fontWeight: '700', color: colors.text },
  src: { borderRadius: radius.pill, paddingHorizontal: 8, paddingVertical: 3 },
  srcT: { fontSize: 10, fontWeight: '700' },
  meta: { flexDirection: 'row', gap: 10, marginTop: 6 },
  metaT: { fontSize: 12, color: colors.textSoft },
  ings: { fontSize: 12, color: '#aaa', marginTop: 5 },
})
