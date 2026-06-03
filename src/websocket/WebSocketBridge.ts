import WebSocket from 'ws';

export interface FrameMessage {
    type: 'frame';
    data: Buffer;
}

export interface EventMessage {
    type: string;
    x?: number;
    y?: number;
    button?: number;
    key?: string;
    code?: string;
    deltaX?: number;
    deltaY?: number;
    modifiers?: {
        shift: boolean;
        ctrl: boolean;
        alt: boolean;
        meta: boolean;
    };
}

export class WebSocketBridge {
    private server: WebSocket.Server | null = null;
    private client: WebSocket | null = null;
    private webviewClient: WebSocket | null = null;
    private port: number = 0;

    onFrame?: (data: Buffer) => void;
    onEvent?: (event: EventMessage) => void;
    onJavaConnect?: () => void;
    onJavaDisconnect?: () => void;

    start(): Promise<number> {
        return new Promise((resolve, reject) => {
            this.server = new WebSocket.Server({ port: 0 }, () => {
                const addr = this.server!.address();
                if (typeof addr === 'object' && addr) {
                    this.port = addr.port;
                    resolve(this.port);
                } else {
                    reject(new Error('Failed to get server port'));
                }
            });

            this.server.on('connection', (ws) => {
                this.client = ws;
                this.onJavaConnect?.();

                ws.on('message', (data) => {
                    if (Buffer.isBuffer(data)) {
                        this.onFrame?.(data);
                        if (this.webviewClient && this.webviewClient.readyState === WebSocket.OPEN) {
                            this.webviewClient.send(data);
                        }
                    } else {
                        try {
                            const msg = JSON.parse(data.toString());
                            this.onEvent?.(msg);
                        } catch {}
                    }
                });

                ws.on('close', () => {
                    this.client = null;
                    this.onJavaDisconnect?.();
                });
            });

            this.server.on('error', reject);
        });
    }

    connectWebview(port: number): void {
        this.webviewClient = new WebSocket(`ws://localhost:${port}`);
    }

    sendEvent(event: EventMessage): void {
        if (this.client && this.client.readyState === WebSocket.OPEN) {
            this.client.send(JSON.stringify(event));
        }
    }

    stop(): void {
        if (this.client) {
            this.client.close();
            this.client = null;
        }
        if (this.webviewClient) {
            this.webviewClient.close();
            this.webviewClient = null;
        }
        if (this.server) {
            this.server.close();
            this.server = null;
        }
    }

    getPort(): number {
        return this.port;
    }
}
