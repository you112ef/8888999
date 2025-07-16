package com.example.spermanalyzer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.pytorch.*
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.FloatBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var module: Module
    private lateinit var analysisEngine: AnalysisEngine
    private lateinit var casaCalculator: CASACalculator
    
    private lateinit var previewView: PreviewView
    private lateinit var imageView: ImageView
    private lateinit var resultTextView: TextView
    private lateinit var analyzeButton: Button
    private lateinit var captureButton: Button
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    
    private val CAMERA_PERMISSION_CODE = 1001
    
    companion object {
        private const val TAG = "SpermAnalyzer"
        private const val MODEL_NAME = "sperm_analyzer.torchscript.pt"
        private const val INPUT_SIZE = 640
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        initializeAI()
        requestCameraPermission()
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setupClickListeners()
    }
    
    private fun initializeViews() {
        previewView = findViewById(R.id.previewView)
        imageView = findViewById(R.id.imageView)
        resultTextView = findViewById(R.id.resultTextView)
        analyzeButton = findViewById(R.id.analyzeButton)
        captureButton = findViewById(R.id.captureButton)
    }
    
    private fun initializeAI() {
        try {
            // Load PyTorch model
            module = LiteModuleLoader.load(assetFilePath(MODEL_NAME))
            
            // Initialize analysis components
            analysisEngine = AnalysisEngine()
            casaCalculator = CASACalculator()
            
            Log.d(TAG, "AI Model loaded successfully")
            Toast.makeText(this, "AI Model loaded successfully", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading AI model: ${e.message}", e)
            Toast.makeText(this, "Error loading AI model: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupClickListeners() {
        analyzeButton.setOnClickListener {
            // Analyze sample image for demonstration
            analyzeSampleImage()
        }
        
        captureButton.setOnClickListener {
            captureImage()
        }
    }
    
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                arrayOf(Manifest.permission.CAMERA), 
                CAMERA_PERMISSION_CODE)
        } else {
            startCamera()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int, 
        permissions: Array<String>, 
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            imageCapture = ImageCapture.Builder().build()
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun captureImage() {
        // Implementation for capturing image from camera
        // For now, using sample analysis
        analyzeSampleImage()
    }
    
    private fun analyzeSampleImage() {
        try {
            // Load sample image (you would replace this with actual captured image)
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.sample_sperm_image)
                ?: createSampleBitmap()
            
            // Run AI analysis
            val results = runInference(bitmap)
            
            // Process results with tracking
            val trackedObjects = analysisEngine.processDetections(results)
            
            // Calculate CASA metrics
            val casaMetrics = casaCalculator.calculateMetrics(trackedObjects)
            
            // Display results
            displayResults(bitmap, trackedObjects, casaMetrics)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during analysis: ${e.message}", e)
            Toast.makeText(this, "Analysis error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun runInference(bitmap: Bitmap): FloatArray {
        // Resize bitmap to model input size
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        
        // Convert to tensor
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resizedBitmap,
            floatArrayOf(0.0f, 0.0f, 0.0f),  // mean normalization
            floatArrayOf(1.0f, 1.0f, 1.0f)   // std normalization
        )
        
        // Run inference
        val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
        
        // Get output as float array
        return outputTensor.dataAsFloatArray
    }
    
    private fun displayResults(
        bitmap: Bitmap, 
        trackedObjects: List<TrackedObject>, 
        casaMetrics: CASAMetrics
    ) {
        // Draw bounding boxes on image
        val resultBitmap = drawBoundingBoxes(bitmap, trackedObjects)
        imageView.setImageBitmap(resultBitmap)
        
        // Display CASA metrics
        val resultText = buildString {
            appendLine("🔬 Sperm Analysis Results")
            appendLine("════════════════════════")
            appendLine("📊 CASA Metrics:")
            appendLine("• VCL: ${String.format("%.2f", casaMetrics.vcl)} μm/s")
            appendLine("• VSL: ${String.format("%.2f", casaMetrics.vsl)} μm/s")
            appendLine("• LIN: ${String.format("%.2f", casaMetrics.lin)}%")
            appendLine("• MOT: ${String.format("%.2f", casaMetrics.motility)}%")
            appendLine()
            appendLine("🎯 Detection Results:")
            appendLine("• Total detected: ${trackedObjects.size}")
            appendLine("• Motile sperm: ${trackedObjects.count { it.isMotile }}")
            appendLine("• Static sperm: ${trackedObjects.count { !it.isMotile }}")
        }
        
        resultTextView.text = resultText
    }
    
    private fun drawBoundingBoxes(bitmap: Bitmap, trackedObjects: List<TrackedObject>): Bitmap {
        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            textSize = 40f
        }
        
        trackedObjects.forEach { obj ->
            // Set color based on motility
            paint.color = if (obj.isMotile) Color.GREEN else Color.RED
            
            // Draw bounding box
            canvas.drawRect(obj.bbox, paint)
            
            // Draw ID and confidence
            paint.style = Paint.Style.FILL
            canvas.drawText(
                "ID:${obj.id} (${String.format("%.1f", obj.confidence * 100)}%)",
                obj.bbox.left,
                obj.bbox.top - 10,
                paint
            )
            paint.style = Paint.Style.STROKE
        }
        
        return resultBitmap
    }
    
    private fun createSampleBitmap(): Bitmap {
        // Create a sample bitmap for demonstration
        val bitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.LTGRAY)
        
        val paint = Paint().apply {
            color = Color.BLUE
            style = Paint.Style.FILL
        }
        
        // Draw some sample "sperm" objects
        for (i in 0..9) {
            val x = (Math.random() * (INPUT_SIZE - 40)).toFloat() + 20
            val y = (Math.random() * (INPUT_SIZE - 40)).toFloat() + 20
            canvas.drawCircle(x, y, 15f, paint)
        }
        
        return bitmap
    }
    
    private fun assetFilePath(assetName: String): String {
        val file = File(filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        
        try {
            assets.open(assetName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
            }
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error copying asset file: $assetName", e)
            throw e
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

// Data classes for analysis
data class TrackedObject(
    val id: Int,
    val bbox: RectF,
    val confidence: Float,
    val isMotile: Boolean,
    val velocity: Float,
    val trajectory: List<Pair<Float, Float>>
)

data class CASAMetrics(
    val vcl: Double,  // Velocity Curvilinear
    val vsl: Double,  // Velocity Straight Line
    val lin: Double,  // Linearity
    val motility: Double  // Motility percentage
)