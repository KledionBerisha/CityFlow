/**
 * Authentication Service
 * Handles user login, registration, and token management with Keycloak
 */

const KEYCLOAK_URL = 'http://localhost:8080'
const REALM = 'cityflow'
const CLIENT_ID = 'cityflow-frontend'

export interface LoginCredentials {
  email: string
  password: string
}

export interface RegisterData {
  name: string
  email: string
  password: string
}

export interface AuthResponse {
  access_token: string
  refresh_token: string
  expires_in: number
  token_type: string
}

export interface User {
  id: string
  email: string
  name: string
}

/**
 * Login user with email and password
 */
export async function login(credentials: LoginCredentials): Promise<AuthResponse> {
  const tokenUrl = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`
  
  const formData = new URLSearchParams()
  formData.append('client_id', CLIENT_ID)
  formData.append('grant_type', 'password')
  formData.append('username', credentials.email)
  formData.append('password', credentials.password)

  const response = await fetch(tokenUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: formData.toString(),
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.error_description || 'Login failed')
  }

  const authData = await response.json()
  
  // Store tokens in localStorage
  localStorage.setItem('access_token', authData.access_token)
  localStorage.setItem('refresh_token', authData.refresh_token)
  
  return authData
}

/**
 * Register a new user
 */
export async function register(data: RegisterData): Promise<void> {
  // Backend API endpoint for registration
  const backendRegisterUrl = 'http://localhost:8081/api/auth/register'
  
  try {
    const response = await fetch(backendRegisterUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: data.email,
        email: data.email,
        firstName: data.name.split(' ')[0],
        lastName: data.name.split(' ').slice(1).join(' ') || '',
        password: data.password,
      }),
    })

    if (!response.ok) {
      let errorMessage = 'Registration failed'
      
      try {
        const errorData = await response.json()
        // Handle different error response formats
        if (errorData.message) {
          errorMessage = errorData.message
        } else if (errorData.error) {
          errorMessage = errorData.error
        } else if (typeof errorData === 'string') {
          errorMessage = errorData
        }
      } catch (e) {
        // If response is not JSON, try to get text
        try {
          const text = await response.text()
          if (text) {
            errorMessage = text
          } else {
            errorMessage = `Registration failed with status ${response.status}: ${response.statusText}`
          }
        } catch (textError) {
          errorMessage = `Registration failed with status ${response.status}: ${response.statusText}`
        }
      }
      
      throw new Error(errorMessage)
    }
  } catch (error: any) {
    // Re-throw if it's already an Error with message
    if (error instanceof Error) {
      throw error
    }
    // Otherwise wrap in Error
    throw new Error(error.message || 'Registration failed. Please try again.')
  }
}

/**
 * Logout user
 */
export async function logout(): Promise<void> {
  const refreshToken = localStorage.getItem('refresh_token')
  
  if (refreshToken) {
    const logoutUrl = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/logout`
    
    const formData = new URLSearchParams()
    formData.append('client_id', CLIENT_ID)
    formData.append('refresh_token', refreshToken)

    try {
      await fetch(logoutUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: formData.toString(),
      })
    } catch (error) {
      console.error('Logout request failed:', error)
    }
  }

  // Clear local storage
  localStorage.removeItem('access_token')
  localStorage.removeItem('refresh_token')
}

/**
 * Get current access token
 */
export function getAccessToken(): string | null {
  return localStorage.getItem('access_token')
}

/**
 * Check if user is authenticated
 */
export function isAuthenticated(): boolean {
  const token = getAccessToken()
  if (!token) return false

  try {
    // Decode JWT token to check expiration
    const payload = JSON.parse(atob(token.split('.')[1]))
    const expirationTime = payload.exp * 1000 // Convert to milliseconds
    return Date.now() < expirationTime
  } catch (error) {
    return false
  }
}

/**
 * Get current user info from token
 */
export function getCurrentUser(): User | null {
  const token = getAccessToken()
  if (!token) return null

  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return {
      id: payload.sub,
      email: payload.email || payload.preferred_username,
      name: payload.name || payload.preferred_username,
    }
  } catch (error) {
    return null
  }
}

/**
 * Refresh access token
 */
export async function refreshToken(): Promise<AuthResponse> {
  const refreshTokenValue = localStorage.getItem('refresh_token')
  if (!refreshTokenValue) {
    throw new Error('No refresh token available')
  }

  const tokenUrl = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`
  
  const formData = new URLSearchParams()
  formData.append('client_id', CLIENT_ID)
  formData.append('grant_type', 'refresh_token')
  formData.append('refresh_token', refreshTokenValue)

  const response = await fetch(tokenUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: formData.toString(),
  })

  if (!response.ok) {
    throw new Error('Token refresh failed')
  }

  const authData = await response.json()
  
  // Update tokens in localStorage
  localStorage.setItem('access_token', authData.access_token)
  localStorage.setItem('refresh_token', authData.refresh_token)
  
  return authData
}

/**
 * Update user password
 */
export async function updatePassword(currentPassword: string, newPassword: string): Promise<void> {
  const token = getAccessToken()
  if (!token) {
    throw new Error('Not authenticated')
  }

  const user = getCurrentUser()
  if (!user) {
    throw new Error('User not found')
  }

  // Verify current password by attempting login
  try {
    const tokenUrl = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`
    
    const formData = new URLSearchParams()
    formData.append('client_id', CLIENT_ID)
    formData.append('grant_type', 'password')
    formData.append('username', user.email)
    formData.append('password', currentPassword)

    const verifyResponse = await fetch(tokenUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: formData.toString(),
    })

    if (!verifyResponse.ok) {
      throw new Error('Current password is incorrect')
    }
  } catch (error: any) {
    throw new Error('Current password is incorrect')
  }

  // Update password via backend API (through API Gateway)
  const backendUrl = 'http://localhost:8000/api/auth/change-password'
  
  try {
    const response = await fetch(backendUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify({
        email: user.email,
        currentPassword,
        newPassword,
      }),
    })

    if (!response.ok) {
      // If endpoint doesn't exist or fails, throw error
      if (response.status === 404) {
        throw new Error('Password change feature is not yet available on the backend')
      }
      if (response.status === 401 || response.status === 403) {
        throw new Error('Authentication failed. Please log in again.')
      }
      
      const error = await response.json().catch(() => ({}))
      throw new Error(error.message || 'Failed to update password')
    }
  } catch (error: any) {
    // Check if it's a network error (backend not running)
    if (error.message.includes('fetch') || error.message.includes('Failed to fetch')) {
      throw new Error('Unable to connect to backend service. Please ensure the backend is running.')
    }
    throw error
  }
}
