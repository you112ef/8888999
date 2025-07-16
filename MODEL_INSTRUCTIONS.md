# 🤖 AI Model Instructions

## ⚠️ CRITICAL: Real PyTorch Model Required

This Android application requires a **REAL, TRAINED** PyTorch model for sperm analysis. The app will **NOT WORK** without a properly trained model.

## 📁 Model Placement

The trained model must be placed at:
```
android-app/app/src/main/assets/sperm_analyzer.torchscript.pt
```

## 🔬 Model Requirements

### Architecture: YOLOv8n (optimized for mobile)
- **Input Size**: 640x640 RGB images
- **Output Format**: YOLO detections [x_center, y_center, width, height, confidence, class]
- **Classes**: 1 (sperm)
- **Format**: TorchScript (.pt file)
- **Size**: Optimized for mobile (~6MB recommended)

### Training Data
- **Dataset**: Microscopic sperm images
- **Annotations**: Bounding boxes around individual sperm
- **Resolution**: High-resolution microscopy images
- **Variety**: Different motility states, concentrations, and conditions

## 🛠️ Model Conversion Process

### 1. Train YOLOv8 Model
```python
from ultralytics import YOLO

# Train the model
model = YOLO('yolov8n.yaml')
results = model.train(data='sperm_dataset.yaml', epochs=100)

# Export to PyTorch
model.export(format='torchscript', optimize=True)
```

### 2. Optimize for Mobile
```python
import torch

# Load the model
model = torch.jit.load('best.torchscript')

# Optimize for mobile
optimized_model = torch.jit.optimize_for_inference(model)

# Save optimized model
torch.jit.save(optimized_model, 'sperm_analyzer.torchscript.pt')
```

### 3. Validation
```python
# Test model inference
dummy_input = torch.randn(1, 3, 640, 640)
output = model(dummy_input)
print(f"Output shape: {output.shape}")
```

## 📊 Expected Model Performance

- **Precision**: >85% for sperm detection
- **Recall**: >90% for sperm detection
- **Inference Time**: <100ms on modern Android devices
- **Model Size**: 3-8MB
- **Memory Usage**: <200MB during inference

## 🚫 What NOT to Use

❌ **Placeholder/dummy models**
❌ **Pre-trained models not trained on sperm data**
❌ **Models with wrong input/output dimensions**
❌ **Non-TorchScript formats**
❌ **Cloud API calls**

## ✅ Verification Checklist

Before using the model, verify:
- [ ] Model file exists at correct path
- [ ] File size is reasonable (3-15MB)
- [ ] Model loads without errors in PyTorch
- [ ] Input shape is (1, 3, 640, 640)
- [ ] Output contains detection data
- [ ] Model was trained on actual sperm data

## 🔬 Training Data Sources

### Recommended Datasets:
1. **CASA dataset** - Computer Assisted Sperm Analysis datasets
2. **University research data** - Academic microscopy datasets
3. **Clinical data** - Anonymized medical samples
4. **Synthetic data** - Augmented microscopy images

### Data Augmentation:
- Rotation (0-360°)
- Brightness/contrast adjustment
- Noise addition
- Blur simulation
- Scale variations

## 📞 Support

If you need assistance with:
- Model training
- Data preparation
- Performance optimization
- Mobile deployment

Please ensure you have proper datasets and computing resources for training a real, functional sperm analysis model.

## ⚖️ Legal Notice

Ensure compliance with:
- Medical device regulations
- Data privacy laws (HIPAA, GDPR)
- Research ethics guidelines
- Institutional review boards (IRB)

**This is a scientific research tool and should not be used for clinical diagnosis without proper validation and regulatory approval.**