const TOKEN_KEY = 'dw_access_token'
const REFRESH_TOKEN_KEY = 'dw_refresh_token'

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token)
  clearLegacyTokenStorage()
}

export function getRefreshToken(): string | null {
  return sessionStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setRefreshToken(token: string): void {
  sessionStorage.setItem(REFRESH_TOKEN_KEY, token)
  clearLegacyTokenStorage()
}

export function removeToken(): void {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  clearLegacyTokenStorage()
}

function clearLegacyTokenStorage(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  document.cookie = `${TOKEN_KEY}=; Max-Age=0; path=/`
  document.cookie = `${REFRESH_TOKEN_KEY}=; Max-Age=0; path=/`
}
