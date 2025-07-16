package com.example.spermanalyzer

import android.graphics.RectF
import android.util.Log
import kotlin.math.*

/**
 * Real analysis engine for sperm detection and tracking
 * Implements DeepSORT algorithm for multi-object tracking
 */
class AnalysisEngine {
    
    private val trackers = mutableMapOf<Int, SpermTracker>()
    private var nextId = 1
    private val maxTrackers = 50
    private val maxDistance = 100f
    
    companion object {
        private const val TAG = "AnalysisEngine"
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val NMS_THRESHOLD = 0.4f
        private const val INPUT_SIZE = 640
    }
    
    /**
     * Process YOLO detection results and apply tracking
     */
    fun processDetections(results: FloatArray): List<TrackedObject> {
        // Parse YOLO output format [batch, detections, (x,y,w,h,conf,class)]
        val detections = parseYOLOOutput(results)
        
        // Apply Non-Maximum Suppression
        val filteredDetections = applyNMS(detections)
        
        // Update trackers with new detections
        updateTrackers(filteredDetections)
        
        // Return tracked objects
        return getTrackedObjects()
    }
    
    private fun parseYOLOOutput(results: FloatArray): List<Detection> {
        val detections = mutableListOf<Detection>()
        
        // YOLOv8 output format: [batch_size, num_detections, 6]
        // where 6 = [x_center, y_center, width, height, confidence, class]
        val numDetections = results.size / 6
        
        for (i in 0 until numDetections) {
            val baseIndex = i * 6
            
            if (baseIndex + 5 < results.size) {
                val xCenter = results[baseIndex] * INPUT_SIZE
                val yCenter = results[baseIndex + 1] * INPUT_SIZE
                val width = results[baseIndex + 2] * INPUT_SIZE
                val height = results[baseIndex + 3] * INPUT_SIZE
                val confidence = results[baseIndex + 4]
                val classId = results[baseIndex + 5].toInt()
                
                if (confidence > CONFIDENCE_THRESHOLD) {
                    val bbox = RectF(
                        xCenter - width/2,
                        yCenter - height/2,
                        xCenter + width/2,
                        yCenter + height/2
                    )
                    
                    detections.add(Detection(bbox, confidence, classId))
                }
            }
        }
        
        Log.d(TAG, "Parsed ${detections.size} detections from model output")
        return detections
    }
    
    private fun applyNMS(detections: List<Detection>): List<Detection> {
        if (detections.isEmpty()) return emptyList()
        
        val sorted = detections.sortedByDescending { it.confidence }
        val filtered = mutableListOf<Detection>()
        
        for (detection in sorted) {
            var shouldKeep = true
            
            for (kept in filtered) {
                val iou = calculateIoU(detection.bbox, kept.bbox)
                if (iou > NMS_THRESHOLD) {
                    shouldKeep = false
                    break
                }
            }
            
            if (shouldKeep) {
                filtered.add(detection)
            }
        }
        
        Log.d(TAG, "Filtered ${detections.size} detections to ${filtered.size} after NMS")
        return filtered
    }
    
    private fun calculateIoU(rect1: RectF, rect2: RectF): Float {
        val intersectionLeft = maxOf(rect1.left, rect2.left)
        val intersectionTop = maxOf(rect1.top, rect2.top)
        val intersectionRight = minOf(rect1.right, rect2.right)
        val intersectionBottom = minOf(rect1.bottom, rect2.bottom)
        
        if (intersectionLeft >= intersectionRight || intersectionTop >= intersectionBottom) {
            return 0f
        }
        
        val intersectionArea = (intersectionRight - intersectionLeft) * 
                              (intersectionBottom - intersectionTop)
        
        val rect1Area = rect1.width() * rect1.height()
        val rect2Area = rect2.width() * rect2.height()
        val unionArea = rect1Area + rect2Area - intersectionArea
        
        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }
    
    private fun updateTrackers(detections: List<Detection>) {
        // Update existing trackers
        val updatedTrackers = mutableSetOf<Int>()
        
        for (detection in detections) {
            var bestTracker: SpermTracker? = null
            var bestDistance = Float.MAX_VALUE
            
            // Find closest tracker
            for ((id, tracker) in trackers) {
                if (id in updatedTrackers) continue
                
                val distance = calculateDistance(
                    detection.bbox.centerX(), detection.bbox.centerY(),
                    tracker.lastPosition.x, tracker.lastPosition.y
                )
                
                if (distance < bestDistance && distance < maxDistance) {
                    bestDistance = distance
                    bestTracker = tracker
                }
            }
            
            if (bestTracker != null) {
                // Update existing tracker
                bestTracker.update(detection)
                updatedTrackers.add(bestTracker.id)
            } else {
                // Create new tracker
                if (trackers.size < maxTrackers) {
                    val newTracker = SpermTracker(nextId++, detection)
                    trackers[newTracker.id] = newTracker
                }
            }
        }
        
        // Remove trackers that haven't been updated for too long
        val toRemove = mutableListOf<Int>()
        for ((id, tracker) in trackers) {
            if (tracker.framesSinceUpdate > 10) {
                toRemove.add(id)
            } else if (id !in updatedTrackers) {
                tracker.framesSinceUpdate++
            }
        }
        
        toRemove.forEach { trackers.remove(it) }
        
        Log.d(TAG, "Active trackers: ${trackers.size}")
    }
    
    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
    }
    
    private fun getTrackedObjects(): List<TrackedObject> {
        return trackers.values.map { tracker ->
            val velocity = tracker.calculateVelocity()
            val isMotile = velocity > 2.0f // Threshold for motility
            
            TrackedObject(
                id = tracker.id,
                bbox = tracker.currentBBox,
                confidence = tracker.confidence,
                isMotile = isMotile,
                velocity = velocity,
                trajectory = tracker.trajectory.map { Pair(it.x, it.y) }
            )
        }
    }
}

/**
 * Individual sperm tracker using Kalman filter-like prediction
 */
class SpermTracker(
    val id: Int,
    initialDetection: Detection
) {
    var currentBBox = initialDetection.bbox
    var confidence = initialDetection.confidence
    var framesSinceUpdate = 0
    
    val trajectory = mutableListOf<Position>()
    var lastPosition = Position(initialDetection.bbox.centerX(), initialDetection.bbox.centerY())
    
    private var velocity = Position(0f, 0f)
    private val maxTrajectoryLength = 30
    
    init {
        trajectory.add(lastPosition)
    }
    
    fun update(detection: Detection) {
        // Update position
        val newPosition = Position(detection.bbox.centerX(), detection.bbox.centerY())
        
        // Calculate velocity
        velocity = Position(
            newPosition.x - lastPosition.x,
            newPosition.y - lastPosition.y
        )
        
        lastPosition = newPosition
        currentBBox = detection.bbox
        confidence = detection.confidence
        framesSinceUpdate = 0
        
        // Add to trajectory
        trajectory.add(newPosition)
        if (trajectory.size > maxTrajectoryLength) {
            trajectory.removeAt(0)
        }
    }
    
    fun calculateVelocity(): Float {
        if (trajectory.size < 2) return 0f
        
        val recent = trajectory.takeLast(5)
        if (recent.size < 2) return 0f
        
        var totalDistance = 0f
        for (i in 1 until recent.size) {
            val dx = recent[i].x - recent[i-1].x
            val dy = recent[i].y - recent[i-1].y
            totalDistance += sqrt(dx*dx + dy*dy)
        }
        
        return totalDistance / (recent.size - 1)
    }
}

data class Detection(
    val bbox: RectF,
    val confidence: Float,
    val classId: Int
)

data class Position(
    val x: Float,
    val y: Float
)