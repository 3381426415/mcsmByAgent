// src/utils/wsClient.js
import { Client } from '@stomp/stompjs';
import { ref } from 'vue';

const isDev = import.meta.env.DEV;
const brokerURL = isDev ? 'ws://127.0.0.1:8000/ws' : '/ws';

// 连接状态：'connected' | 'disconnected' | 'reconnecting'
export const wsState = ref('disconnected');

// 重连回调列表
const reconnectCallbacks = [];
let isFirstConnect = true;

export function onReconnect(callback) {
  reconnectCallbacks.push(callback);
  return () => {
    const idx = reconnectCallbacks.indexOf(callback);
    if (idx >= 0) reconnectCallbacks.splice(idx, 1);
  };
}

const client = new Client({
    brokerURL: brokerURL,
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
        wsState.value = 'connected';
        isFirstConnect = false;
        // 每次连接（含首次）都触发回调，让订阅方及时订阅
        for (const cb of reconnectCallbacks) {
            try { cb(); } catch (e) { console.error('[WS] connect callback error:', e); }
        }
    },
    onDisconnect: () => {
        wsState.value = 'disconnected';
    },
    onStompError: () => {
        wsState.value = 'disconnected';
    },
});

// 断线时标记为重连中
let originalOnWebSocketClose;
const origActivate = client.activate.bind(client);
client.activate = () => {
    wsState.value = 'reconnecting';
    return origActivate();
};

// 每次连接前动态注入 token
client.beforeConnect = () => {
    const token = localStorage.getItem('token') || '';
    client.connectHeaders = { token };
};

export default client;
