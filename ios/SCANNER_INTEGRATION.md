# Barcode Scanner Integration

## Overview
VisionKit barcode scanning has been integrated into the BoardGameTimer iOS app using a clean abstraction that supports both real camera scanning and testing.

## Architecture

### Scanner Abstraction (`BarcodeScanner` protocol)
```swift
protocol BarcodeScanner {
    func scan() async throws -> String
}
```

Simple async interface that returns a barcode string or throws an error.

### Implementations

#### 1. VisionKitBarcodeScanner (Production)
- **Location:** `BoardGameTimer/Scanner/BarcodeScanner.swift`
- **Requirements:** iOS 16+, device with camera support
- **Supported Symbologies:**
  - EAN-8, EAN-13 (European/International barcodes)
  - UPC-E (Universal Product Code)
  - Code 128, Code 39 (alphanumeric barcodes)
- **Features:**
  - Auto-detects and scans first barcode in frame
  - Manual tap to confirm barcode
  - Prevents multiple simultaneous scans
  - Proper continuation-based async/await integration

#### 2. FakeBarcodeScanner (Testing/Preview)
- **Location:** `BoardGameTimer/Scanner/BarcodeScanner.swift`
- **Purpose:** Unit tests, SwiftUI previews, simulator testing
- **Configuration:**
  - `barcodeToReturn`: Set the barcode to return
  - `delay`: Simulate scan duration
  - `shouldFail`: Trigger error states
  - `errorToThrow`: Specific error to throw

### SwiftUI Integration

#### BarcodeScannerView
- **Type:** `UIViewControllerRepresentable` wrapper for `DataScannerViewController`
- **Usage:** Only visible when actively scanning on supported devices
- **Features:**
  - Live camera preview with barcode highlighting
  - Auto-scan on first detection
  - Tap-to-confirm option
  - Material design overlay with instructions

#### ScanView
- **Location:** `BoardGameTimer/Views/ScanView.swift`
- **Integration Points:**
  1. **Idle state:** Show scan prompt
  2. **Scanning state:**
     - Real device: Show `BarcodeScannerView` with camera
     - Simulator/fallback: Show progress indicator
  3. **Resolving state:** Show lookup progress
  4. **Success/Error states:** Show results or retry options

### ViewModel Integration

#### ScanViewModel
- **Exposes scanner:** `visionKitScanner: VisionKitBarcodeScanner?`
  - Returns scanner instance if using VisionKit
  - Returns `nil` if using fake scanner
  - Allows SwiftUI view to conditionally show camera UI
- **State machine:**
  - `idle` → `scanning` → `resolving` → `resolved`/`unsupported`/`error`
- **Methods:**
  - `startScan()`: Triggers barcode scan
  - `resolveBarcode(_:)`: Looks up game from barcode
  - `reset()`: Returns to idle state

### Device Selection

**AppCoordinator** automatically selects appropriate scanner:
```swift
#if targetEnvironment(simulator)
  // Always use fake scanner in simulator
  FakeBarcodeScanner()
#else
  if #available(iOS 16.0, *), DataScannerViewController.isSupported {
    // Use real scanner on supported devices
    VisionKitBarcodeScanner()
  } else {
    // Fallback to fake scanner
    FakeBarcodeScanner()
  }
#endif
```

## Error Handling

### ScannerError enum
- `cameraUnavailable`: Device doesn't support camera/DataScanner
- `scanCancelled`: User cancelled the scan
- `scanInProgress`: Attempted to scan while already scanning
- `invalidBarcode`: Barcode format not supported

All errors conform to `LocalizedError` for user-friendly messages.

## Testing

### Test Files
1. **BarcodeScannerTests.swift** (NEW)
   - Tests FakeBarcodeScanner configuration and behavior
   - Tests VisionKitBarcodeScanner state management
   - Tests scanner integration with ViewModel
   - NO camera required - all mocked

2. **ScanViewModelTests.swift** (Existing)
   - Integration tests using FakeBarcodeScanner
   - Tests state transitions
   - Tests error handling

### Test Coverage
- ✅ Barcode return values
- ✅ Delay simulation
- ✅ Error states (cancellation, unavailable, in-progress)
- ✅ Scanner reusability
- ✅ ViewModel scanner exposure
- ✅ Barcode resolution flow
- ✅ Error propagation

## Usage Examples

### In Production
```swift
// AppCoordinator automatically selects scanner
let scanner = /* VisionKitBarcodeScanner or FakeBarcodeScanner */
let viewModel = ScanViewModel(scanner: scanner, service: service)
ScanView(viewModel: viewModel, path: $path)
```

### In Tests
```swift
let scanner = FakeBarcodeScanner()
scanner.barcodeToReturn = "0029877030712"
let viewModel = ScanViewModel(scanner: scanner, service: mockService)
await viewModel.startScan()
// Assert state transitions
```

### In Previews
```swift
#Preview {
    let scanner = FakeBarcodeScanner()
    let service = MockBoardGameService()
    ScanView(
        viewModel: ScanViewModel(scanner: scanner, service: service),
        path: .constant(NavigationPath())
    )
}
```

## Adding the Test File to Xcode

**IMPORTANT:** The new test file `BarcodeScannerTests.swift` needs to be added to the Xcode project:

1. Open `BoardGameTimer.xcodeproj` in Xcode
2. Right-click on `BoardGameTimerTests` folder
3. Select "Add Files to 'BoardGameTimer'..."
4. Navigate to `BoardGameTimerTests/BarcodeScannerTests.swift`
5. Ensure "Add to targets: BoardGameTimerTests" is checked
6. Click "Add"

## Future Enhancements

Potential improvements:
- QR code support for game URLs/IDs
- Batch scanning for multiple games
- Manual barcode entry fallback
- Scan history/cache
- Custom barcode symbology filtering
- Haptic feedback on successful scan
- Audio cues for accessibility

## Dependencies

- **VisionKit framework** (iOS 16+)
- **SwiftUI** for UI integration
- **Async/await** for scanner interface
- No third-party dependencies required
