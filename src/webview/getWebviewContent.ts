import * as vscode from 'vscode';
import * as crypto from 'crypto';

function getNonce(): string {
    return crypto.randomBytes(16).toString('hex');
}

export function getWebviewContent(webview: vscode.Webview): string {
    const nonce = getNonce();
    const cspSource = webview.cspSource;

    return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src ${cspSource} 'unsafe-inline'; script-src 'nonce-${nonce}'; img-src ${cspSource} blob: data:; connect-src ws://localhost:*; font-src ${cspSource};">
    <title>JavaVisualizer</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: var(--vscode-font-family);
            background: var(--vscode-editor-background);
            color: var(--vscode-editor-foreground);
            height: 100vh;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }

        .toolbar {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 8px 16px;
            background: var(--vscode-titleBar-activeBackground);
            border-bottom: 1px solid var(--vscode-panel-border);
            position: sticky;
            top: 0;
            z-index: 100;
            flex-shrink: 0;
        }

        .toolbar button {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 6px 12px;
            border: none;
            border-radius: 4px;
            background: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
            cursor: pointer;
            font-size: 13px;
            font-family: var(--vscode-font-family);
            transition: background 0.15s;
        }

        .toolbar button:hover {
            background: var(--vscode-button-hoverBackground);
        }

        .toolbar button:active {
            transform: scale(0.97);
        }

        .toolbar button.secondary {
            background: var(--vscode-button-secondaryBackground);
            color: var(--vscode-button-secondaryForeground);
        }

        .toolbar button.secondary:hover {
            background: var(--vscode-button-secondaryHoverBackground);
        }

        .toggle-container {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 13px;
        }

        .toggle {
            position: relative;
            width: 36px;
            height: 20px;
            cursor: pointer;
        }

        .toggle input {
            opacity: 0;
            width: 0;
            height: 0;
        }

        .toggle-slider {
            position: absolute;
            inset: 0;
            background: var(--vscode-input-background);
            border-radius: 10px;
            transition: background 0.2s;
        }

        .toggle-slider::before {
            content: '';
            position: absolute;
            width: 16px;
            height: 16px;
            left: 2px;
            top: 2px;
            background: var(--vscode-foreground);
            border-radius: 50%;
            transition: transform 0.2s;
        }

        .toggle input:checked + .toggle-slider {
            background: var(--vscode-button-background);
        }

        .toggle input:checked + .toggle-slider::before {
            transform: translateX(16px);
        }

        .spacer {
            flex: 1;
        }

        .status {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 12px;
            color: var(--vscode-descriptionForeground);
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: var(--vscode-descriptionForeground);
        }

        .status-dot.connected {
            background: var(--vscode-testing-iconPassed);
        }

        .status-dot.compiling {
            background: var(--vscode-editorWarning-foreground);
            animation: pulse 1s infinite;
        }

        .status-dot.error {
            background: var(--vscode-errorForeground);
        }

        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.4; }
        }

        .canvas-container {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
            position: relative;
        }

        #preview-canvas {
            max-width: 100%;
            max-height: 100%;
            image-rendering: auto;
        }

        .error-overlay {
            position: absolute;
            inset: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            background: var(--vscode-editor-background);
            z-index: 50;
        }

        .error-overlay.hidden {
            display: none;
        }

        .error-panel {
            max-width: 600px;
            padding: 24px;
            border-radius: 8px;
            background: var(--vscode-input-background);
            border: 1px solid var(--vscode-panel-border);
        }

        .error-panel h3 {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 16px;
            color: var(--vscode-errorForeground);
            font-size: 16px;
        }

        .error-panel pre {
            background: var(--vscode-editor-background);
            padding: 12px;
            border-radius: 4px;
            font-family: var(--vscode-editor-font-family);
            font-size: 12px;
            overflow-x: auto;
            white-space: pre-wrap;
            word-break: break-word;
            max-height: 300px;
            overflow-y: auto;
        }

        .error-panel .actions {
            margin-top: 16px;
            display: flex;
            gap: 8px;
        }

        .error-panel .actions button {
            padding: 6px 12px;
            border: none;
            border-radius: 4px;
            background: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
            cursor: pointer;
            font-size: 13px;
        }

        .empty-state {
            text-align: center;
            color: var(--vscode-descriptionForeground);
        }

        .empty-state h2 {
            font-size: 18px;
            margin-bottom: 8px;
            color: var(--vscode-foreground);
        }

        .empty-state p {
            font-size: 14px;
        }
    </style>
</head>
<body>
    <div class="toolbar">
        <button id="btn-refresh" title="Refresh Preview">
            <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
                <path d="M13.5 2.5a.5.5 0 0 1 .5.5v3a.5.5 0 0 1-.5.5h-3a.5.5 0 0 1 0-1h1.793L10.146 3.854a.5.5 0 0 1 .708-.708l2.5 2.5a.5.5 0 0 1 .146.354V3a.5.5 0 0 1 .5-.5zM2.5 9a.5.5 0 0 1 .5.5v3a.5.5 0 0 0 1 0v-1.793l1.646 1.647a.5.5 0 0 0 .708-.708l-2.5-2.5A.5.5 0 0 0 3.5 9h-1z"/>
            </svg>
            Refresh
        </button>

        <div class="toggle-container">
            <label class="toggle">
                <input type="checkbox" id="chk-autoreload">
                <span class="toggle-slider"></span>
            </label>
            <span>Auto-Reload</span>
        </div>

        <button id="btn-desktop" class="secondary" title="Run on Desktop">
            <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
                <path d="M0 4s.5-1 2-1h12c1.5 0 2 1 2 1v6s-.5 1-2 1H2c-1.5 0-2-1-2-1V4zm1 7h14v1H1v-1zm4.5 2h5v1h-5v-1z"/>
            </svg>
            Run Desktop
        </button>

        <div class="spacer"></div>

        <div class="status">
            <span class="status-dot" id="status-dot"></span>
            <span id="status-text">Idle</span>
        </div>
    </div>

    <div class="canvas-container">
        <canvas id="preview-canvas"></canvas>

        <div class="error-overlay hidden" id="error-overlay">
            <div class="error-panel">
                <h3>
                    <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                        <path d="M8 1a7 7 0 1 0 0 14A7 7 0 0 0 8 1zm0 12.5a5.5 5.5 0 1 1 0-11 5.5 5.5 0 0 1 0 11zM8 4a.75.75 0 0 1 .75.75v3.5a.75.75 0 0 1-1.5 0v-3.5A.75.75 0 0 1 8 4zm0 8a1 1 0 1 1 0-2 1 1 0 0 1 0 2z"/>
                    </svg>
                    <span id="error-title">Error</span>
                </h3>
                <pre id="error-details"></pre>
                <div class="actions">
                    <button id="btn-retry">Retry</button>
                </div>
            </div>
        </div>

        <div class="empty-state" id="empty-state">
            <h2>JavaVisualizer</h2>
            <p>Press <strong>Refresh</strong> to compile and preview your JavaFX app.</p>
        </div>
    </div>

    <script nonce="${nonce}">
        console.log('JavaVisualizer webview script loaded');
        const vscode = acquireVsCodeApi();
        console.log('VS Code API acquired');
        const canvas = document.getElementById('preview-canvas');
        const ctx = canvas.getContext('2d');
        const btnRefresh = document.getElementById('btn-refresh');
        const btnDesktop = document.getElementById('btn-desktop');
        const chkAutoReload = document.getElementById('chk-autoreload');
        const statusDot = document.getElementById('status-dot');
        const statusText = document.getElementById('status-text');
        const errorOverlay = document.getElementById('error-overlay');
        const errorTitle = document.getElementById('error-title');
        const errorDetails = document.getElementById('error-details');
        const btnRetry = document.getElementById('btn-retry');
        const emptyState = document.getElementById('empty-state');

        let ws = null;
        let autoReload = false;

        btnRefresh.addEventListener('click', () => {
            vscode.postMessage({ type: 'refresh' });
        });

        btnDesktop.addEventListener('click', () => {
            vscode.postMessage({ type: 'runDesktop' });
        });

        chkAutoReload.addEventListener('change', (e) => {
            autoReload = e.target.checked;
            vscode.postMessage({ type: 'autoReload', value: autoReload });
        });

        btnRetry.addEventListener('click', () => {
            vscode.postMessage({ type: 'refresh' });
        });

        function setStatus(status, text) {
            statusDot.className = 'status-dot ' + status;
            statusText.textContent = text;
        }

        function showError(title, details) {
            errorTitle.textContent = title;
            errorDetails.textContent = details;
            errorOverlay.classList.remove('hidden');
            emptyState.style.display = 'none';
            setStatus('error', title);
        }

        function hideError() {
            errorOverlay.classList.add('hidden');
        }

        function connectWebSocket(port) {
            if (ws) {
                ws.close();
            }

            ws = new WebSocket('ws://localhost:' + port);
            ws.binaryType = 'arraybuffer';

            ws.onopen = () => {
                ws.send(JSON.stringify({ type: 'identify', role: 'webview' }));
                setStatus('connected', 'Connected');
                hideError();
                emptyState.style.display = 'none';
            };

            ws.onmessage = (event) => {
                if (event.data instanceof ArrayBuffer) {
                    const blob = new Blob([event.data], { type: 'image/jpeg' });
                    const url = URL.createObjectURL(blob);
                    const img = new Image();
                    img.onload = () => {
                        canvas.width = img.width;
                        canvas.height = img.height;
                        ctx.drawImage(img, 0, 0);
                        URL.revokeObjectURL(url);
                    };
                    img.src = url;
                } else {
                    try {
                        const msg = JSON.parse(event.data);
                        if (msg.type === 'resize') {
                            canvas.width = msg.width;
                            canvas.height = msg.height;
                        }
                    } catch (e) {
                        console.error('Error parsing WebSocket message:', e);
                    }
                }
            };

            ws.onclose = () => {
                setStatus('', 'Disconnected');
            };

            ws.onerror = () => {
                setStatus('error', 'Connection failed');
            };
        }

        canvas.addEventListener('mousedown', (e) => {
            if (!ws || ws.readyState !== WebSocket.OPEN) return;
            const rect = canvas.getBoundingClientRect();
            const x = (e.clientX - rect.left) * (canvas.width / rect.width);
            const y = (e.clientY - rect.top) * (canvas.height / rect.height);
            ws.send(JSON.stringify({
                type: 'mouse_press',
                x: Math.round(x),
                y: Math.round(y),
                button: e.button
            }));
        });

        canvas.addEventListener('mouseup', (e) => {
            if (!ws || ws.readyState !== WebSocket.OPEN) return;
            const rect = canvas.getBoundingClientRect();
            const x = (e.clientX - rect.left) * (canvas.width / rect.width);
            const y = (e.clientY - rect.top) * (canvas.height / rect.height);
            ws.send(JSON.stringify({
                type: 'mouse_release',
                x: Math.round(x),
                y: Math.round(y),
                button: e.button
            }));
        });

        canvas.addEventListener('mousemove', (e) => {
            if (!ws || ws.readyState !== WebSocket.OPEN) return;
            const rect = canvas.getBoundingClientRect();
            const x = (e.clientX - rect.left) * (canvas.width / rect.width);
            const y = (e.clientY - rect.top) * (canvas.height / rect.height);
            ws.send(JSON.stringify({
                type: 'mouse_move',
                x: Math.round(x),
                y: Math.round(y)
            }));
        });

        canvas.addEventListener('wheel', (e) => {
            if (!ws || ws.readyState !== WebSocket.OPEN) return;
            e.preventDefault();
            const rect = canvas.getBoundingClientRect();
            const x = (e.clientX - rect.left) * (canvas.width / rect.width);
            const y = (e.clientY - rect.top) * (canvas.height / rect.height);
            ws.send(JSON.stringify({
                type: 'mouse_scroll',
                x: Math.round(x),
                y: Math.round(y),
                deltaX: e.deltaX,
                deltaY: e.deltaY
            }));
        });

        document.addEventListener('keydown', (e) => {
            if (!ws || ws.readyState !== WebSocket.OPEN) return;
            ws.send(JSON.stringify({
                type: 'key_press',
                key: e.key,
                code: e.code,
                modifiers: {
                    shift: e.shiftKey,
                    ctrl: e.ctrlKey,
                    alt: e.altKey,
                    meta: e.metaKey
                }
            }));
        });

        document.addEventListener('keyup', (e) => {
            if (!ws || ws.readyState !== WebSocket.OPEN) return;
            ws.send(JSON.stringify({
                type: 'key_release',
                key: e.key,
                code: e.code,
                modifiers: {
                    shift: e.shiftKey,
                    ctrl: e.ctrlKey,
                    alt: e.altKey,
                    meta: e.metaKey
                }
            }));
        });

        window.addEventListener('message', (event) => {
            const message = event.data;
            switch (message.type) {
                case 'setStatus':
                    setStatus(message.status, message.text);
                    break;
                case 'showError':
                    showError(message.title, message.details);
                    break;
                case 'hideError':
                    hideError();
                    break;
                case 'connectWebSocket':
                    connectWebSocket(message.port);
                    break;
                case 'setAutoReload':
                    chkAutoReload.checked = message.value;
                    autoReload = message.value;
                    break;
            }
        });

        vscode.postMessage({ type: 'ready' });
    </script>
</body>
</html>`;
}
