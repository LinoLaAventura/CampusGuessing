import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client/dist/sockjs';

function resolveApiBase() {
  return import.meta?.env?.VITE_API_BASE_URL || '/api';
}

function buildSockJsUrl(username) {
  const base = resolveApiBase().replace(/\/$/, '');
  const qs = new URLSearchParams({ username: username || '' });
  return `${base}/ws-battle?${qs.toString()}`;
}

export class BattleSocket {
  /** @type {Client | null} */
  #client = null;

  /** @type {string} */
  #username = '';

  /** @type {Set<(msg:any)=>void>} */
  #stateSubscribers = new Set();

  /** @type {Set<(msg:any)=>void>} */
  #inviteSubscribers = new Set();

  /** @type {Set<(connected:boolean)=>void>} */
  #connectionSubscribers = new Set();

  /** @type {boolean} */
  #connected = false;

  /** @type {Promise<void> | null} */
  #connectPromise = null;

  /** @type {(() => void) | null} */
  #connectResolve = null;

  /** @type {((err: any) => void) | null} */
  #connectReject = null;

  get username() {
    return this.#username;
  }

  get connected() {
    return this.#connected;
  }

  subscribeState(cb) {
    this.#stateSubscribers.add(cb);
    return () => this.#stateSubscribers.delete(cb);
  }

  subscribeInvite(cb) {
    this.#inviteSubscribers.add(cb);
    return () => this.#inviteSubscribers.delete(cb);
  }

  subscribeConnection(cb) {
    this.#connectionSubscribers.add(cb);
    cb(this.#connected);
    return () => this.#connectionSubscribers.delete(cb);
  }

  #emitConnection(next) {
    this.#connected = next;
    for (const cb of this.#connectionSubscribers) cb(next);
  }

  #emitState(msg) {
    for (const cb of this.#stateSubscribers) cb(msg);
  }

  #emitInvite(msg) {
    for (const cb of this.#inviteSubscribers) cb(msg);
  }

  async ensureConnected(timeoutMs = 5000) {
    if (this.#connected && this.#client) return;
    if (!this.#username) {
      throw new Error('未登录，无法建立对战连接');
    }

    if (!this.#client) {
      await this.connect(this.#username);
    }

    if (this.#connected) return;
    if (!this.#connectPromise) {
      throw new Error('WebSocket连接未初始化');
    }

    const timeout = new Promise((_, reject) => {
      globalThis.setTimeout(() => reject(new Error('WebSocket连接超时')), timeoutMs);
    });

    await Promise.race([this.#connectPromise, timeout]);
  }

  async connect(username) {
    const nextUsername = String(username || '').trim();
    if (!nextUsername) {
      this.disconnect();
      return;
    }

    // 同一用户且已连接，直接复用
    if (this.#client && this.#username === nextUsername) {
      if (this.#connected) return;
      if (this.#connectPromise) return this.#connectPromise;
    }

    // 切换用户：先断开旧连接
    this.disconnect();
    this.#username = nextUsername;

    this.#connectPromise = new Promise((resolve, reject) => {
      this.#connectResolve = resolve;
      this.#connectReject = reject;
    });

    const sockJsUrl = buildSockJsUrl(this.#username);

    const client = new Client({
      webSocketFactory: () => new SockJS(sockJsUrl),
      reconnectDelay: 2500,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
    });

    client.onConnect = () => {
      this.#emitConnection(true);

      this.#connectResolve?.();
      this.#connectResolve = null;
      this.#connectReject = null;
      this.#connectPromise = null;

      client.subscribe('/user/queue/battle/invite', (message) => {
        try {
          const data = JSON.parse(message.body);
          this.#emitInvite(data);
        } catch {
          // ignore
        }
      });

      client.subscribe('/user/queue/battle/state', (message) => {
        try {
          const data = JSON.parse(message.body);
          this.#emitState(data);
        } catch {
          // ignore
        }
      });
    };

    client.onWebSocketClose = () => {
      this.#emitConnection(false);

      // 仅在“尚未成功连接”阶段，才 reject（避免已连接后的正常断线导致 Promise unhandled）
      if (this.#connectReject) {
        this.#connectReject(new Error('WebSocket连接已关闭'));
        this.#connectResolve = null;
        this.#connectReject = null;
        this.#connectPromise = null;
      }
    };

    client.onWebSocketError = () => {
      this.#emitConnection(false);

      if (this.#connectReject) {
        this.#connectReject(new Error('WebSocket连接失败'));
        this.#connectResolve = null;
        this.#connectReject = null;
        this.#connectPromise = null;
      }
    };

    client.onStompError = () => {
      // STOMP 层错误也视为断开
      this.#emitConnection(false);

      if (this.#connectReject) {
        this.#connectReject(new Error('WebSocket协议错误'));
        this.#connectResolve = null;
        this.#connectReject = null;
        this.#connectPromise = null;
      }
    };

    this.#client = client;
    client.activate();

    return this.#connectPromise;
  }

  disconnect() {
    if (this.#client) {
      this.#client.deactivate().catch(() => {});
    }

    if (this.#connectReject) {
      this.#connectReject(new Error('连接已取消'));
    }
    this.#connectPromise = null;
    this.#connectResolve = null;
    this.#connectReject = null;

    this.#client = null;
    this.#username = '';
    this.#emitConnection(false);
  }

  async #publish(destination, body) {
    await this.ensureConnected();
    if (!this.#client || !this.#connected) throw new Error('WebSocket未连接');
    this.#client.publish({ destination, body: JSON.stringify(body ?? {}) });
  }

  async invite(toUsername) {
    await this.#publish('/app/battle/invite', {
      fromUsername: this.#username,
      toUsername: String(toUsername || '').trim(),
    });
  }

  async respond(roomCode, accepted) {
    await this.#publish('/app/battle/respond', {
      roomCode: String(roomCode || '').trim(),
      accepted: Boolean(accepted),
      username: this.#username,
    });
  }

  async answer(roomCode, longitude, latitude) {
    await this.#publish('/app/battle/answer', {
      roomCode: String(roomCode || '').trim(),
      username: this.#username,
      longitude,
      latitude,
    });
  }

  async quit() {
    await this.#publish('/app/battle/quit', {
      fromUsername: this.#username,
      toUsername: '',
    });
  }
}

export const battleSocket = new BattleSocket();
