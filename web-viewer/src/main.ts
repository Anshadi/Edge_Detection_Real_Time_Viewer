// Main application entry point
document.addEventListener('DOMContentLoaded', () => {
    const app = new EdgeDetectionApp();
    app.initialize();
});

class EdgeDetectionApp {
    private canvas: HTMLCanvasElement;
    private ctx: CanvasRenderingContext2D;
    private originalImage: HTMLImageElement | null = null;
    private processedImage: HTMLImageElement | null = null;
    private mode: 'original' | 'grayscale' | 'blur' | 'edge' = 'original';
    private fps = 0;
    private lastFrameTime = 0;
    private frameCount = 0;
    private lastFpsUpdate = 0;

    constructor() {
        this.canvas = document.createElement('canvas');
        this.ctx = this.canvas.getContext('2d')!;
    }

    public initialize(): void {
        this.setupUI();
        this.setupEventListeners();
        this.loadSampleImage();
        this.animate(0);
    }

    private setupUI(): void {
        const app = document.getElementById('app');
        if (!app) return;

        app.innerHTML = `
            <div class="container">
                <header>
                    <h1>Edge Detection Viewer</h1>
                    <div class="fps-counter">FPS: <span id="fps">--</span></div>
                </header>
                
                <div class="controls">
                    <div class="mode-selector">
                        <button data-mode="original" class="active">Original</button>
                        <button data-mode="grayscale">Grayscale</button>
                        <button data-mode="blur">Blur</button>
                        <button data-mode="edge">Edge Detection</button>
                    </div>
                    
                    <div class="file-actions">
                        <label class="btn">
                            Upload Image
                            <input type="file" id="fileInput" accept="image/*" style="display: none;">
                        </label>
                        <button id="saveBtn" class="btn">Save Processed Image</button>
                    </div>
                </div>
                
                <div class="image-viewer">
                    <div class="image-container">
                        <h3>Original</h3>
                        <div class="image-wrapper">
                            <img id="originalImage" src="" alt="Original">
                        </div>
                    </div>
                    <div class="image-container">
                        <h3>Processed (<span id="currentMode">Original</span>)</h3>
                        <div class="image-wrapper">
                            <canvas id="processedCanvas"></canvas>
                        </div>
                    </div>
                </div>
            </div>
        `;

        this.canvas = document.getElementById('processedCanvas') as HTMLCanvasElement;
        this.ctx = this.canvas.getContext('2d')!;
    }

    private setupEventListeners(): void {
        // Mode selection
        document.querySelectorAll('.mode-selector button').forEach(button => {
            button.addEventListener('click', (e) => {
                const target = e.target as HTMLElement;
                const mode = target.dataset.mode as 'original' | 'grayscale' | 'blur' | 'edge';
                if (mode) {
                    this.setMode(mode);
                }
            });
        });

        // File upload
        const fileInput = document.getElementById('fileInput') as HTMLInputElement;
        fileInput.addEventListener('change', (e) => {
            const file = (e.target as HTMLInputElement).files?.[0];
            if (file) {
                this.loadImage(file);
            }
        });

        // Save button
        const saveBtn = document.getElementById('saveBtn');
        saveBtn?.addEventListener('click', () => this.saveImage());
    }

    private setMode(mode: 'original' | 'grayscale' | 'blur' | 'edge'): void {
        this.mode = mode;
        document.querySelectorAll('.mode-selector button').forEach(btn => {
            btn.classList.toggle('active', btn.getAttribute('data-mode') === mode);
        });
        
        const modeDisplay = document.getElementById('currentMode');
        if (modeDisplay) {
            modeDisplay.textContent = mode.charAt(0).toUpperCase() + mode.slice(1);
        }
        
        this.processImage();
    }

    private loadSampleImage(): void {
        // Create a sample image (you can replace this with your own sample image)
        const img = new Image();
        img.onload = () => {
            this.originalImage = img;
            this.processImage();
        };
        img.src = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZWVlIi8+PHRleHQgeD0iNTAlIiB5PSI1JSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjI0IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBhbGlnbm1lbnQtYmFzZWxpbmU9Im1pZGRsZSIgZmlsbD0iIzMzMyI+RWRnZSBEZXRlY3Rpb24gVmlld2VyPC90ZXh0Pjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTYiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGFsaWdubWVudC1iYXNlbGluZT0ibWlkZGxlIiBmaWxsPSIjNjY2Ij5VcGxvYWQgYW4gaW1hZ2UgdG8gYmVnaW48L3RleHQ+PC9zdmc+';
    }

    private loadImage(file: File): void {
        const reader = new FileReader();
        reader.onload = (e) => {
            const img = new Image();
            img.onload = () => {
                this.originalImage = img;
                this.processImage();
            };
            img.src = e.target?.result as string;
            
            // Update the original image display
            const originalImg = document.getElementById('originalImage') as HTMLImageElement;
            if (originalImg) {
                originalImg.src = e.target?.result as string;
            }
        };
        reader.readAsDataURL(file);
    }

    private processImage(): void {
        if (!this.originalImage) return;

        // Set canvas size to match image
        this.canvas.width = this.originalImage.width;
        this.canvas.height = this.originalImage.height;

        // Draw the original image first
        this.ctx.drawImage(this.originalImage, 0, 0);

        // Get image data
        const imageData = this.ctx.getImageData(0, 0, this.canvas.width, this.canvas.height);
        const data = imageData.data;

        // Apply selected filter
        switch (this.mode) {
            case 'grayscale':
                this.applyGrayscale(data);
                break;
            case 'blur':
                this.applyBlur(data, this.canvas.width, this.canvas.height);
                break;
            case 'edge':
                this.applyEdgeDetection(data, this.canvas.width, this.canvas.height);
                break;
            // 'original' mode doesn't need processing
        }

        // Put the processed data back
        this.ctx.putImageData(imageData, 0, 0);
    }

    private applyGrayscale(data: Uint8ClampedArray): void {
        for (let i = 0; i < data.length; i += 4) {
            const avg = (data[i] + data[i + 1] + data[i + 2]) / 3;
            data[i] = avg;     // R
            data[i + 1] = avg; // G
            data[i + 2] = avg; // B
        }
    }

    private applyBlur(data: Uint8ClampedArray, width: number, height: number): void {
        const tempData = new Uint8ClampedArray(data);
        
        for (let y = 1; y < height - 1; y++) {
            for (let x = 1; x < width - 1; x++) {
                for (let c = 0; c < 3; c++) {
                    let sum = 0;
                    for (let ky = -1; ky <= 1; ky++) {
                        for (let kx = -1; kx <= 1; kx++) {
                            const idx = ((y + ky) * width + (x + kx)) * 4 + c;
                            sum += tempData[idx];
                        }
                    }
                    const idx = (y * width + x) * 4 + c;
                    data[idx] = sum / 9;
                }
            }
        }
    }

    private applyEdgeDetection(data: Uint8ClampedArray, width: number, height: number): void {
        // Convert to grayscale first
        const gray = new Uint8Array(width * height);
        for (let i = 0; i < width * height; i++) {
            const idx = i * 4;
            gray[i] = 0.299 * data[idx] + 0.587 * data[idx + 1] + 0.114 * data[idx + 2];
        }
        
        // Sobel operators
        const sobelX = [-1, 0, 1, -2, 0, 2, -1, 0, 1];
        const sobelY = [-1, -2, -1, 0, 0, 0, 1, 2, 1];
        
        // Apply Sobel operator
        for (let y = 1; y < height - 1; y++) {
            for (let x = 1; x < width - 1; x++) {
                let gx = 0, gy = 0;
                let idx = 0;
                
                for (let ky = -1; ky <= 1; ky++) {
                    for (let kx = -1; kx <= 1; kx++) {
                        const pixel = gray[(y + ky) * width + (x + kx)];
                        gx += sobelX[idx] * pixel;
                        gy += sobelY[idx] * pixel;
                        idx++;
                    }
                }
                
                const magnitude = Math.sqrt(gx * gx + gy * gy);
                const edge = magnitude > 50 ? 255 : 0;
                
                const outIdx = (y * width + x) * 4;
                data[outIdx] = edge;     // R
                data[outIdx + 1] = edge; // G
                data[outIdx + 2] = edge; // B
                // Alpha channel remains unchanged
            }
        }
    }

    private saveImage(): void {
        const link = document.createElement('a');
        link.download = `edge-detection-${this.mode}-${new Date().getTime()}.png`;
        link.href = this.canvas.toDataURL('image/png');
        link.click();
    }

    private updateFPS(timestamp: number): void {
        if (!this.lastFrameTime) {
            this.lastFrameTime = timestamp;
            this.lastFpsUpdate = timestamp;
            return;
        }

        // Count frames
        this.frameCount++;
        
        // Calculate FPS every second
        const delta = timestamp - this.lastFpsUpdate;
        if (delta > 1000) {
            this.fps = Math.round((this.frameCount * 1000) / delta);
            this.frameCount = 0;
            this.lastFpsUpdate = timestamp;
            
            // Update FPS display
            const fpsElement = document.getElementById('fps');
            if (fpsElement) {
                fpsElement.textContent = this.fps.toString();
            }
        }
        
        this.lastFrameTime = timestamp;
    }

    private animate(timestamp: number): void {
        this.updateFPS(timestamp);
        requestAnimationFrame((ts) => this.animate(ts));
    }
}
