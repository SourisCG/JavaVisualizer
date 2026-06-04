import WebSocket from 'ws';
import { logger } from '../utils/logger';

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
    frameRate?: number;
    jpegQuality?: number;
    modifiers?: {
        shift: boolean;
        ctrl: boolean;
        alt: boolean;
        meta: boolean;
    };
}

interface ClientInfo {
    ws: WebSocket;
    role: 'agent' | 'webview' | 'unknown';
}

export class WebSocketBridge {
    private server: WebSocket.Server | null = null;
    private agentClient: WebSocket | null = null;
    private webviewClient: WebSocket | null = null;
    private port: number = 0;
    private clients: Map<WebSocket, ClientInfo> = new Map();

    onFrame?: (data: Buffer) => void;
    onEvent?: (event: EventMessage) => void;
    onAgentConnect?: () => void;
    onAgentDisconnect?: () => void;
    onWebviewConnect?: () => void;
    onWebviewDisconnect?: () => void;

    start(): Promise<number> {
        return new Promise((resolve, reject) => {
            logger.info('Starting WebSocket bridge server');
            
            this.server = new WebSocket.Server({ port: 0 }, () => {
                const addr = this.server!.address();
                if (typeof addr === 'object' && addr) {
                    this.port = addr.port;
                    logger.info(`WebSocket bridge server listening on port ${this.port}`);
                    resolve(this.port);
                } else {
                    reject(new Error('Failed to get server port'));
                }
            });

            this.server.on('connection', (ws) => {
                logger.info('New WebSocket connection received');
                
                const clientInfo: ClientInfo = { ws, role: 'unknown' };
                this.clients.set(ws, clientInfo);

                ws.on('message', (data, isBinary) => {
                    if (isBinary) {
                        this.handleBinaryMessage(ws, data as Buffer);
                    } else {
                        this.handleTextMessage(ws, data.toString());
                    }
                });

                ws.on('close', () => {
                    const info = this.clients.get(ws);
                    this.clients.delete(ws);
                    
                    if (info?.role === 'agent') {
                        logger.info('Agent disconnected');
                        this.agentClient = null;
                        this.onAgentDisconnect?.();
                    } else if (info?.role === 'webview') {
                        logger.info('Webview disconnected');
                        this.webviewClient = null;
                        this.onWebviewDisconnect?.();
                    }
                });

                ws.on('error', (error) => {
                    logger.error('WebSocket client error', error);
                });
            });

            this.server.on('error', (error) => {
                logger.error('WebSocket server error', error);
                reject(error);
            });
        });
    }

    private handleBinaryMessage(ws: WebSocket, data: Buffer): void {
        const info = this.clients.get(ws);
        
        if (info?.role === 'agent') {
            logger.debug(`Received frame from agent: ${data.length} bytes`);
            this.onFrame?.(data);
            
            if (this.webviewClient && this.webviewClient.readyState === WebSocket.OPEN) {
                this.webviewClient.send(data);
                logger.debug('Frame forwarded to webview');
            } else {
                logger.warn('Cannot forward frame: webview not connected');
            }
        } else {
            logger.warn('Received binary message from unknown client');
        }
    }

    private handleTextMessage(ws: WebSocket, message: string): void {
        try {
            const msg = JSON.parse(message);
            const info = this.clients.get(ws);
            
            if (msg.type === 'identify') {
                this.handleIdentification(ws, msg.role);
                return;
            }
            
            if (info?.role === 'webview') {
                logger.debug(`Received event from webview: ${msg.type}`);
                this.onEvent?.(msg);
                
                if (this.agentClient && this.agentClient.readyState === WebSocket.OPEN) {
                    this.agentClient.send(message);
                    logger.debug('Event forwarded to agent');
                } else {
                    logger.warn('Cannot forward event: agent not connected');
                }
            } else if (info?.role === 'agent') {
                logger.debug(`Received message from agent: ${msg.type}`);
            } else {
                logger.warn('Received text message from unknown client');
            }
        } catch (error) {
            logger.error('Error parsing WebSocket message', error as Error);
        }
    }

    private handleIdentification(ws: WebSocket, role: string): void {
        const info = this.clients.get(ws);
        if (!info) {
            return;
        }

        if (role === 'agent') {
            logger.info('Agent identified');
            info.role = 'agent';
            this.agentClient = ws;
            this.onAgentConnect?.();
        } else if (role === 'webview') {
            logger.info('Webview identified');
            info.role = 'webview';
            this.webviewClient = ws;
            this.onWebviewConnect?.();
        } else {
            logger.warn(`Unknown role in identification: ${role}`);
        }
    }

    sendEvent(event: EventMessage): void {
        if (this.agentClient && this.agentClient.readyState === WebSocket.OPEN) {
            this.agentClient.send(JSON.stringify(event));
            logger.debug(`Sent event to agent: ${event.type}`);
        } else {
            logger.warn('Cannot send event: agent not connected');
        }
    }

    stop(): void {
        logger.info('Stopping WebSocket bridge server');
        
        this.clients.forEach((info, ws) => {
            try {
                ws.close();
            } catch (error) {
                logger.error('Error closing WebSocket client', error as Error);
            }
        });
        this.clients.clear();
        
        this.agentClient = null;
        this.webviewClient = null;
        
        if (this.server) {
            this.server.close();
            this.server = null;
        }
        
        logger.info('WebSocket bridge server stopped');
    }

    getPort(): number {
        return this.port;
    }
    
    isAgentConnected(): boolean {
        return this.agentClient !== null && this.agentClient.readyState === WebSocket.OPEN;
    }
    
    isWebviewConnected(): boolean {
        return this.webviewClient !== null && this.webviewClient.readyState === WebSocket.OPEN;
    }
}
