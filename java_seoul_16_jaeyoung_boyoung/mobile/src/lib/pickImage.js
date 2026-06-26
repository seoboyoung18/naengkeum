import * as ImagePicker from 'expo-image-picker'

/**
 * 갤러리에서 이미지 1장 선택 → { uri, name, type } 반환.
 * 권한 거부/취소 시 { error } 또는 { canceled }.
 */
export async function pickImage() {
  const perm = await ImagePicker.requestMediaLibraryPermissionsAsync()
  if (!perm.granted) return { error: '사진 접근 권한이 필요해요' }

  const res = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ImagePicker.MediaTypeOptions.Images,
    quality: 0.7,
  })
  if (res.canceled) return { canceled: true }

  const a = res.assets[0]
  const name = a.fileName || a.uri.split('/').pop() || 'photo.jpg'
  let type = a.mimeType
  if (!type) {
    const ext = (name.split('.').pop() || 'jpg').toLowerCase()
    type = ext === 'png' ? 'image/png' : ext === 'webp' ? 'image/webp' : 'image/jpeg'
  }
  return { file: { uri: a.uri, name, type } }
}
