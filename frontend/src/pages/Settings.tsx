import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Save, User, Lock, Database, Download, LogOut } from 'lucide-react'
import { logout, getCurrentUser } from '../services/auth'

interface UserProfile {
  name: string
  email: string
}

interface DataSettings {
  temperatureUnit: 'celsius' | 'fahrenheit'
  distanceUnit: 'km' | 'miles'
  dateFormat: '24h' | '12h'
}

function Settings() {
  const navigate = useNavigate()
  const [profile, setProfile] = useState<UserProfile>({
    name: '',
    email: ''
  })
  const [dataSettings, setDataSettings] = useState<DataSettings>(() => {
    const saved = localStorage.getItem('cityflow-data-settings')
    return saved ? JSON.parse(saved) : {
      temperatureUnit: 'celsius',
      distanceUnit: 'km',
      dateFormat: '24h'
    }
  })
  
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [saved, setSaved] = useState(false)
  const [passwordError, setPasswordError] = useState('')

  useEffect(() => {
    const user = getCurrentUser()
    if (user) {
      setProfile({
        name: user.name || '',
        email: user.email || ''
      })
    }
  }, [])

  const handleSaveProfile = () => {
    // TODO: API call to update user profile
    localStorage.setItem('cityflow-user-profile', JSON.stringify(profile))
    setSaved(true)
    setTimeout(() => setSaved(false), 3000)
  }

  const handleSaveDataSettings = () => {
    localStorage.setItem('cityflow-data-settings', JSON.stringify(dataSettings))
    setSaved(true)
    setTimeout(() => setSaved(false), 3000)
  }

  const handleChangePassword = () => {
    setPasswordError('')
    
    if (!currentPassword || !newPassword || !confirmPassword) {
      setPasswordError('All password fields are required')
      return
    }
    
    if (newPassword !== confirmPassword) {
      setPasswordError('New passwords do not match')
      return
    }
    
    if (newPassword.length < 8) {
      setPasswordError('Password must be at least 8 characters')
      return
    }
    
    // TODO: API call to change password
    alert('Password changed successfully')
    setCurrentPassword('')
    setNewPassword('')
    setConfirmPassword('')
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const handleExportSettings = () => {
    const data = {
      profile,
      dataSettings,
      exportDate: new Date().toISOString()
    }
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `cityflow-settings-${Date.now()}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Settings</h1>
        <button
          onClick={handleLogout}
          className="flex items-center gap-2 bg-red-600 hover:bg-red-700 text-white px-6 py-2 rounded-lg transition-colors"
        >
          <LogOut size={20} />
          Logout
        </button>
      </div>

      {saved && (
        <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg text-green-800">
          Settings saved successfully!
        </div>
      )}

      <div className="space-y-6">
        {/* Account Information */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center gap-2 mb-4">
            <User className="text-blue-600" size={24} />
            <h2 className="text-xl font-semibold text-gray-800">Account Information</h2>
          </div>
          
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Full Name
              </label>
              <input
                type="text"
                value={profile.name}
                onChange={(e) => setProfile({ ...profile, name: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Enter your name"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Email Address
              </label>
              <input
                type="email"
                value={profile.email}
                disabled
                className="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 text-gray-600 cursor-not-allowed"
              />
              <p className="text-xs text-gray-500 mt-1">Email cannot be changed</p>
            </div>

            <div className="flex justify-end">
              <button
                onClick={handleSaveProfile}
                className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg transition-colors"
              >
                <Save size={18} />
                Save Profile
              </button>
            </div>
          </div>
        </div>

        {/* Change Password */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center gap-2 mb-4">
            <Lock className="text-blue-600" size={24} />
            <h2 className="text-xl font-semibold text-gray-800">Change Password</h2>
          </div>
          
          <div className="space-y-4">
            {passwordError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-md text-red-700 text-sm">
                {passwordError}
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Current Password
              </label>
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Enter current password"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                New Password
              </label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Enter new password"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Confirm New Password
              </label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Confirm new password"
              />
            </div>

            <div className="flex justify-end">
              <button
                onClick={handleChangePassword}
                className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg transition-colors"
              >
                <Lock size={18} />
                Update Password
              </button>
            </div>
          </div>
        </div>

        {/* Data Display Preferences */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center gap-2 mb-4">
            <Database className="text-blue-600" size={24} />
            <h2 className="text-xl font-semibold text-gray-800">Data Display Preferences</h2>
          </div>
          
          <div className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Temperature Unit
                </label>
                <select
                  value={dataSettings.temperatureUnit}
                  onChange={(e) => setDataSettings({ ...dataSettings, temperatureUnit: e.target.value as any })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="celsius">Celsius (°C)</option>
                  <option value="fahrenheit">Fahrenheit (°F)</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Distance Unit
                </label>
                <select
                  value={dataSettings.distanceUnit}
                  onChange={(e) => setDataSettings({ ...dataSettings, distanceUnit: e.target.value as any })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="km">Kilometers</option>
                  <option value="miles">Miles</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Time Format
                </label>
                <select
                  value={dataSettings.dateFormat}
                  onChange={(e) => setDataSettings({ ...dataSettings, dateFormat: e.target.value as any })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="24h">24-hour</option>
                  <option value="12h">12-hour (AM/PM)</option>
                </select>
              </div>
            </div>

            <div className="flex justify-end">
              <button
                onClick={handleSaveDataSettings}
                className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg transition-colors"
              >
                <Save size={18} />
                Save Preferences
              </button>
            </div>
          </div>
        </div>

        {/* Data Management */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center gap-2 mb-4">
            <Download className="text-blue-600" size={24} />
            <h2 className="text-xl font-semibold text-gray-800">Data Management</h2>
          </div>
          
          <div className="space-y-4">
            <div>
              <h3 className="text-sm font-medium text-gray-700 mb-2">Export Data</h3>
              <p className="text-sm text-gray-600 mb-3">Download your settings and preferences</p>
              <button 
                onClick={handleExportSettings}
                className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50 transition-colors text-sm"
              >
                Export Settings (JSON)
              </button>
            </div>
          </div>
        </div>

        {/* System Info */}
        <div className="bg-gray-50 rounded-lg p-4 text-sm text-gray-600">
          <div className="grid grid-cols-2 gap-y-2">
            <span>Version:</span>
            <span className="font-mono text-right">1.0.0</span>
            <span>Last Login:</span>
            <span className="text-right">{new Date().toLocaleString()}</span>
            <span>Account Created:</span>
            <span className="text-right">-</span>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Settings

