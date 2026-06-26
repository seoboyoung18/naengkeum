import http from './http'

/** 팔로우 → { following: true, followerCount } */
export async function follow(userId) {
  const { data } = await http.post(`/api/follow/${userId}`)
  return data
}

/** 언팔로우 → { following: false, followerCount } */
export async function unfollow(userId) {
  const { data } = await http.delete(`/api/follow/${userId}`)
  return data
}
