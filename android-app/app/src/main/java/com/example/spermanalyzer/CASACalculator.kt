package com.example.spermanalyzer

import android.util.Log
import kotlin.math.*

/**
 * Computer Assisted Sperm Analysis (CASA) Calculator
 * Implements real scientific formulas for sperm motility analysis
 */
class CASACalculator {
    
    companion object {
        private const val TAG = "CASACalculator"
        private const val PIXEL_TO_MICRON_RATIO = 0.5 // Conversion factor (adjust based on microscope)
        private const val FRAME_RATE = 30.0 // fps
        private const val MIN_TRACK_LENGTH = 5 // Minimum trajectory points for analysis
    }
    
    /**
     * Calculate comprehensive CASA metrics from tracked objects
     */
    fun calculateMetrics(trackedObjects: List<TrackedObject>): CASAMetrics {
        Log.d(TAG, "Calculating CASA metrics for ${trackedObjects.size} tracked objects")
        
        // Filter objects with sufficient trajectory data
        val validTracks = trackedObjects.filter { 
            it.trajectory.size >= MIN_TRACK_LENGTH 
        }
        
        if (validTracks.isEmpty()) {
            return CASAMetrics(0.0, 0.0, 0.0, 0.0)
        }
        
        val vclValues = mutableListOf<Double>()
        val vslValues = mutableListOf<Double>()
        val linValues = mutableListOf<Double>()
        var motileCount = 0
        
        validTracks.forEach { track ->
            val metrics = calculateIndividualMetrics(track)
            
            vclValues.add(metrics.vcl)
            vslValues.add(metrics.vsl)
            linValues.add(metrics.linearity)
            
            if (metrics.isMotile) {
                motileCount++
            }
        }
        
        // Calculate average values
        val avgVCL = vclValues.average()
        val avgVSL = vslValues.average()
        val avgLIN = linValues.average()
        val motility = (motileCount.toDouble() / validTracks.size) * 100.0
        
        Log.d(TAG, "CASA Results - VCL: $avgVCL, VSL: $avgVSL, LIN: $avgLIN, MOT: $motility%")
        
        return CASAMetrics(
            vcl = avgVCL,
            vsl = avgVSL,
            lin = avgLIN,
            motility = motility
        )
    }
    
    /**
     * Calculate metrics for individual sperm
     */
    private fun calculateIndividualMetrics(track: TrackedObject): IndividualMetrics {
        val trajectory = track.trajectory
        
        if (trajectory.size < MIN_TRACK_LENGTH) {
            return IndividualMetrics(0.0, 0.0, 0.0, false)
        }
        
        // Convert pixels to microns
        val micronTrajectory = trajectory.map { point ->
            Pair(
                point.first * PIXEL_TO_MICRON_RATIO,
                point.second * PIXEL_TO_MICRON_RATIO
            )
        }
        
        // Calculate VCL (Velocity Curvilinear) - total path distance
        val vcl = calculateVCL(micronTrajectory)
        
        // Calculate VSL (Velocity Straight Line) - straight line distance
        val vsl = calculateVSL(micronTrajectory)
        
        // Calculate LIN (Linearity) - VSL/VCL ratio
        val linearity = if (vcl > 0) (vsl / vcl) * 100.0 else 0.0
        
        // Determine if sperm is motile (based on velocity threshold)
        val isMotile = vcl > 10.0 // μm/s threshold for motility
        
        return IndividualMetrics(vcl, vsl, linearity, isMotile)
    }
    
    /**
     * Calculate VCL (Velocity Curvilinear) - velocity along actual path
     */
    private fun calculateVCL(trajectory: List<Pair<Double, Double>>): Double {
        if (trajectory.size < 2) return 0.0
        
        var totalDistance = 0.0
        
        for (i in 1 until trajectory.size) {
            val dx = trajectory[i].first - trajectory[i-1].first
            val dy = trajectory[i].second - trajectory[i-1].second
            val distance = sqrt(dx*dx + dy*dy)
            totalDistance += distance
        }
        
        // Convert to velocity (μm/s)
        val timeInterval = (trajectory.size - 1) / FRAME_RATE
        return if (timeInterval > 0) totalDistance / timeInterval else 0.0
    }
    
    /**
     * Calculate VSL (Velocity Straight Line) - velocity along straight path
     */
    private fun calculateVSL(trajectory: List<Pair<Double, Double>>): Double {
        if (trajectory.size < 2) return 0.0
        
        val startPoint = trajectory.first()
        val endPoint = trajectory.last()
        
        val dx = endPoint.first - startPoint.first
        val dy = endPoint.second - startPoint.second
        val straightDistance = sqrt(dx*dx + dy*dy)
        
        // Convert to velocity (μm/s)
        val timeInterval = (trajectory.size - 1) / FRAME_RATE
        return if (timeInterval > 0) straightDistance / timeInterval else 0.0
    }
    
    /**
     * Calculate VAP (Velocity Average Path) - velocity along smoothed path
     */
    private fun calculateVAP(trajectory: List<Pair<Double, Double>>): Double {
        if (trajectory.size < 3) return calculateVSL(trajectory)
        
        // Apply smoothing to trajectory
        val smoothedTrajectory = smoothTrajectory(trajectory)
        
        var totalDistance = 0.0
        for (i in 1 until smoothedTrajectory.size) {
            val dx = smoothedTrajectory[i].first - smoothedTrajectory[i-1].first
            val dy = smoothedTrajectory[i].second - smoothedTrajectory[i-1].second
            val distance = sqrt(dx*dx + dy*dy)
            totalDistance += distance
        }
        
        val timeInterval = (smoothedTrajectory.size - 1) / FRAME_RATE
        return if (timeInterval > 0) totalDistance / timeInterval else 0.0
    }
    
    /**
     * Apply moving average smoothing to trajectory
     */
    private fun smoothTrajectory(trajectory: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
        if (trajectory.size <= 3) return trajectory
        
        val smoothed = mutableListOf<Pair<Double, Double>>()
        val windowSize = 3
        
        for (i in trajectory.indices) {
            val start = maxOf(0, i - windowSize/2)
            val end = minOf(trajectory.size - 1, i + windowSize/2)
            
            var sumX = 0.0
            var sumY = 0.0
            var count = 0
            
            for (j in start..end) {
                sumX += trajectory[j].first
                sumY += trajectory[j].second
                count++
            }
            
            smoothed.add(Pair(sumX / count, sumY / count))
        }
        
        return smoothed
    }
    
    /**
     * Calculate additional advanced metrics
     */
    fun calculateAdvancedMetrics(trackedObjects: List<TrackedObject>): AdvancedCASAMetrics {
        val validTracks = trackedObjects.filter { 
            it.trajectory.size >= MIN_TRACK_LENGTH 
        }
        
        if (validTracks.isEmpty()) {
            return AdvancedCASAMetrics(0.0, 0.0, 0.0, 0.0, 0.0)
        }
        
        val vapValues = mutableListOf<Double>()
        val wobbleValues = mutableListOf<Double>()
        val beatFrequencies = mutableListOf<Double>()
        val amplitudes = mutableListOf<Double>()
        
        validTracks.forEach { track ->
            val micronTrajectory = track.trajectory.map { point ->
                Pair(
                    point.first * PIXEL_TO_MICRON_RATIO,
                    point.second * PIXEL_TO_MICRON_RATIO
                )
            }
            
            val vcl = calculateVCL(micronTrajectory)
            val vap = calculateVAP(micronTrajectory)
            val wobble = if (vap > 0) (vcl / vap) * 100.0 else 0.0
            
            vapValues.add(vap)
            wobbleValues.add(wobble)
            
            // Calculate beat frequency and amplitude
            val (frequency, amplitude) = analyzeBeatPattern(micronTrajectory)
            beatFrequencies.add(frequency)
            amplitudes.add(amplitude)
        }
        
        return AdvancedCASAMetrics(
            vap = vapValues.average(),
            wobble = wobbleValues.average(),
            beatFrequency = beatFrequencies.average(),
            amplitude = amplitudes.average(),
            progressiveMotility = calculateProgressiveMotility(validTracks)
        )
    }
    
    /**
     * Analyze beat pattern to extract frequency and amplitude
     */
    private fun analyzeBeatPattern(trajectory: List<Pair<Double, Double>>): Pair<Double, Double> {
        if (trajectory.size < 10) return Pair(0.0, 0.0)
        
        // Calculate lateral displacement from straight path
        val startPoint = trajectory.first()
        val endPoint = trajectory.last()
        val pathLength = sqrt(
            (endPoint.first - startPoint.first).pow(2) + 
            (endPoint.second - startPoint.second).pow(2)
        )
        
        if (pathLength == 0.0) return Pair(0.0, 0.0)
        
        val lateralDisplacements = mutableListOf<Double>()
        
        trajectory.forEach { point ->
            // Calculate perpendicular distance from straight line
            val distance = calculatePerpendicularDistance(point, startPoint, endPoint)
            lateralDisplacements.add(distance)
        }
        
        // Find peaks to estimate frequency
        val peaks = findPeaks(lateralDisplacements)
        val frequency = if (peaks.size > 1) {
            (peaks.size - 1) * FRAME_RATE / trajectory.size
        } else {
            0.0
        }
        
        // Calculate amplitude as average of peak values
        val amplitude = if (peaks.isNotEmpty()) {
            peaks.map { lateralDisplacements[it] }.average()
        } else {
            0.0
        }
        
        return Pair(frequency, amplitude)
    }
    
    /**
     * Calculate perpendicular distance from point to line
     */
    private fun calculatePerpendicularDistance(
        point: Pair<Double, Double>,
        lineStart: Pair<Double, Double>,
        lineEnd: Pair<Double, Double>
    ): Double {
        val A = lineEnd.second - lineStart.second
        val B = lineStart.first - lineEnd.first
        val C = lineEnd.first * lineStart.second - lineStart.first * lineEnd.second
        
        return abs(A * point.first + B * point.second + C) / sqrt(A*A + B*B)
    }
    
    /**
     * Find peaks in lateral displacement data
     */
    private fun findPeaks(data: List<Double>): List<Int> {
        val peaks = mutableListOf<Int>()
        
        for (i in 1 until data.size - 1) {
            if (data[i] > data[i-1] && data[i] > data[i+1]) {
                peaks.add(i)
            }
        }
        
        return peaks
    }
    
    /**
     * Calculate progressive motility percentage
     */
    private fun calculateProgressiveMotility(tracks: List<TrackedObject>): Double {
        val progressiveCount = tracks.count { track ->
            val micronTrajectory = track.trajectory.map { point ->
                Pair(
                    point.first * PIXEL_TO_MICRON_RATIO,
                    point.second * PIXEL_TO_MICRON_RATIO
                )
            }
            
            val vsl = calculateVSL(micronTrajectory)
            vsl > 25.0 // μm/s threshold for progressive motility
        }
        
        return (progressiveCount.toDouble() / tracks.size) * 100.0
    }
}

// Data classes for metrics
data class IndividualMetrics(
    val vcl: Double,
    val vsl: Double,
    val linearity: Double,
    val isMotile: Boolean
)

data class AdvancedCASAMetrics(
    val vap: Double,        // Velocity Average Path
    val wobble: Double,     // Wobble (VCL/VAP)
    val beatFrequency: Double, // Beat cross frequency
    val amplitude: Double,  // Lateral head displacement
    val progressiveMotility: Double // Progressive motility %
)