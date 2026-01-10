/**
 * useMLPredictions Hook
 * 
 * React hook for fetching and managing ML-based traffic predictions
 */

import { useState, useEffect, useCallback } from 'react'
import { getMLPredictions, getAllSegmentPredictions, checkMLAPIHealth, MLPredictionResponse } from '../services/api'

export interface PredictionData {
  segmentId: string
  currentSpeed: number
  predictions: {
    horizon: number // minutes
    speed: number
    change: number // percentage change from current
  }[]
  timestamp: string
}

export function useMLPredictions(refreshInterval: number = 30000) {
  const [predictions, setPredictions] = useState<PredictionData[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [mlApiAvailable, setMlApiAvailable] = useState(false)

  // Check if ML API is available
  useEffect(() => {
    const checkHealth = async () => {
      const isHealthy = await checkMLAPIHealth()
      setMlApiAvailable(isHealthy)
      if (!isHealthy) {
        console.info('ML API not available - predictions disabled')
      }
    }

    checkHealth()
    const healthInterval = setInterval(checkHealth, 60000) // Check every minute

    return () => clearInterval(healthInterval)
  }, [])

  // Transform ML API response to frontend format
  const transformPredictions = useCallback((response: MLPredictionResponse): PredictionData[] => {
    const grouped = new Map<string, any>()

    response.predictions.forEach(pred => {
      if (!grouped.has(pred.road_segment_id)) {
        grouped.set(pred.road_segment_id, {
          segmentId: pred.road_segment_id,
          currentSpeed: pred.current_speed_kmh,
          predictions: [],
          timestamp: pred.timestamp
        })
      }

      const segment = grouped.get(pred.road_segment_id)
      const change = ((pred.predicted_speed_kmh - pred.current_speed_kmh) / pred.current_speed_kmh) * 100

      segment.predictions.push({
        horizon: pred.prediction_horizon_minutes,
        speed: pred.predicted_speed_kmh,
        change: change
      })
    })

    return Array.from(grouped.values()).map(segment => ({
      ...segment,
      predictions: segment.predictions.sort((a: any, b: any) => a.horizon - b.horizon)
    }))
  }, [])

  // Fetch predictions
  const fetchPredictions = useCallback(async () => {
    if (!mlApiAvailable) {
      setLoading(false)
      return
    }

    try {
      setError(null)
      
      // Try to get predictions for all segments (30-min horizon)
      const response = await getAllSegmentPredictions(30)
      
      if (response) {
        const transformed = transformPredictions(response)
        setPredictions(transformed)
      } else {
        setError('Failed to fetch predictions')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error')
      console.error('Error fetching ML predictions:', err)
    } finally {
      setLoading(false)
    }
  }, [mlApiAvailable, transformPredictions])

  // Auto-refresh predictions
  useEffect(() => {
    fetchPredictions()

    if (refreshInterval > 0) {
      const interval = setInterval(fetchPredictions, refreshInterval)
      return () => clearInterval(interval)
    }
  }, [fetchPredictions, refreshInterval])

  return {
    predictions,
    loading,
    error,
    mlApiAvailable,
    refetch: fetchPredictions
  }
}

/**
 * Hook for getting predictions for specific segments
 */
export function useSegmentPredictions(segmentIds: string[], horizons: number[] = [10, 20, 30]) {
  const [predictions, setPredictions] = useState<MLPredictionResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchPredictions = useCallback(async (readings: any[]) => {
    setLoading(true)
    setError(null)

    try {
      const response = await getMLPredictions({
        readings,
        prediction_horizons: horizons
      })

      setPredictions(response)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error')
    } finally {
      setLoading(false)
    }
  }, [horizons])

  return {
    predictions,
    loading,
    error,
    fetchPredictions
  }
}
