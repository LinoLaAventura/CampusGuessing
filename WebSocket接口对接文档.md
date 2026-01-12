# 校园猜猜猜 - WebSocket对战接口对接文档

## 📌 快速开始

### 1. 连接WebSocket

**连接地址：**
```
ws://localhost:8080/api/ws-battle?username={用户名}
```

**重要说明：**
- ⚠️ **必须**通过URL参数传递 `username`，否则无法识别用户身份
- 支持SockJS降级（浏览器不支持原生WebSocket时自动切换）
- 连接成功后，服务器会自动维护用户在线状态

**前端示例（使用SockJS + STOMP）：**
```javascript
// 1. 引入依赖
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>

// 2. 建立连接
let stompClient = null;
const username = "alice";  // 当前用户名

function connect() {
    const socket = new SockJS(`http://localhost:8080/api/ws-battle?username=${username}`);
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('已连接: ' + frame);
        
        // 订阅个人消息队列（接收对战相关通知）
        stompClient.subscribe(`/user/queue/battle/state`, function(message) {
            const data = JSON.parse(message.body);
            handleBattleMessage(data);
        });
        
        // 订阅邀请通知
        stompClient.subscribe(`/user/queue/battle/invite`, function(message) {
            const data = JSON.parse(message.body);
            handleInvite(data);
        });
    });
}

// 3. 断开连接
function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    console.log("已断开连接");
}
```

---

## 🎮 核心功能流程

### 流程1: 发起对战邀请

**客户端发送：**
```javascript
// 发送到: /app/battle/invite
stompClient.send("/app/battle/invite", {}, JSON.stringify({
    fromUsername: "alice",    // 邀请者
    toUsername: "bob"         // 被邀请者
}));
```

**服务器推送（给被邀请者）：**
```javascript
// 推送到: /user/queue/battle/invite
{
    "type": "INVITE",
    "roomCode": "A3F9B2E1",
    "playerA": "alice",
    "playerB": "bob",
    "message": "alice 邀请你进行对战"
}
```

**可能的错误响应（推送给邀请者）：**
```javascript
// 推送到: /user/queue/battle/state
{
    "type": "INVITE_REJECTED",
    "message": "❌ 用户 bob 未在线"  // 或其他错误原因
}
```

---

### 流程2: 响应邀请（接受/拒绝）

**接受邀请：**
```javascript
// 发送到: /app/battle/respond
stompClient.send("/app/battle/respond", {}, JSON.stringify({
    roomCode: "A3F9B2E1",   // 房间代码
    accepted: true,          // true=接受, false=拒绝
    username: "bob"          // 当前用户名
}));
```

**拒绝邀请：**
```javascript
stompClient.send("/app/battle/respond", {}, JSON.stringify({
    roomCode: "A3F9B2E1",
    accepted: false,
    username: "bob"
}));
```

**服务器推送（接受后，双方都会收到）：**
```javascript
// 推送到: /user/queue/battle/state
{
    "type": "GAME_START",
    "roomCode": "A3F9B2E1",
    "playerA": "alice",
    "playerB": "bob",
    "playerAHealth": 100,
    "playerBHealth": 100,
    "currentRound": 1,
    "question": {
        "id": 123,
        "title": "这是哪里？",
        "imageKey": "abc123.jpg",
        "difficulty": "简单",
        "authorUsername": "admin"
    },
    "message": "对战开始！"
}
```

**服务器推送（拒绝后，邀请者会收到）：**
```javascript
// 推送到: /user/queue/battle/state
{
    "type": "INVITE_REJECTED",
    "message": "❌ bob 拒绝了你的邀请"
}
```

---

### 流程3: 提交答案

**客户端发送：**
```javascript
// 发送到: /app/battle/answer
stompClient.send("/app/battle/answer", {}, JSON.stringify({
    roomCode: "A3F9B2E1",
    username: "alice",
    longitude: 113.123456,   // 答案经度
    latitude: 23.456789      // 答案纬度
}));
```

**服务器推送（单方提交后，双方收到）：**
```javascript
// 推送到: /user/queue/battle/state
{
    "type": "PLAYER_ANSWERED",
    "roomCode": "A3F9B2E1",
    "playerA": "alice",
    "playerB": "bob",
    "playerAAnswered": true,   // alice已答
    "playerBAnswered": false,  // bob未答
    "message": "alice 已提交答案，等待对方..."
}
```

**服务器推送（双方都提交后，显示回合结果）：**
```javascript
// 推送到: /user/queue/battle/state
{
    "type": "ROUND_RESULT",
    "roomCode": "A3F9B2E1",
    "playerA": "alice",
    "playerB": "bob",
    "playerAHealth": 80,       // alice剩余血量
    "playerBHealth": 100,      // bob剩余血量
    "currentRound": 1,
    "roundResult": {
        "playerADistance": 1234.5,   // alice距离正确答案1234.5米
        "playerBDistance": 567.8,    // bob距离正确答案567.8米
        "damagedPlayer": "alice",    // alice被扣血
        "damage": 20                 // 扣除20血量
    },
    "message": "回合1结束！bob 更接近，alice 扣除 20 血量"
}
```

**下一回合开始（3秒后自动推送）：**
```javascript
// 推送到: /user/queue/battle/state
{
    "type": "NEW_QUESTION",
    "roomCode": "A3F9B2E1",
    "currentRound": 2,
    "question": {
        "id": 124,
        "title": "这是什么建筑？",
        "imageKey": "def456.jpg",
        ...
    },
    "playerAAnswered": false,
    "playerBAnswered": false,
    "message": "第2回合开始！"
}
```

---

### 流程4: 游戏结束

**服务器推送（某方血量归零时）：**
```javascript
// 推送到: /user/queue/battle/state
{
    "type": "GAME_OVER",
    "roomCode": "A3F9B2E1",
    "playerA": "alice",
    "playerB": "bob",
    "playerAHealth": 0,
    "playerBHealth": 60,
    "winner": "bob",
    "message": "🎉 bob 获胜！"
}
```

---

### 流程5: 主动退出对战

**客户端发送：**
```javascript
// 发送到: /app/battle/quit
stompClient.send("/app/battle/quit", {}, JSON.stringify({
    roomCode: "A3F9B2E1",
    username: "alice"
}));
```

**服务器推送（对方会收到）：**
```javascript
// 推送到: /user/queue/battle/state
{
    "type": "GAME_OVER",
    "winner": "bob",
    "message": "alice 退出对战，bob 获胜！"
}
```

---

## 📦 完整消息类型定义

### MessageType 枚举

| 类型 | 说明 | 触发时机 |
|-----|------|---------|
| `INVITE` | 邀请通知 | 收到对战邀请时 |
| `INVITE_ACCEPTED` | 邀请被接受 | 对方接受邀请时 |
| `INVITE_REJECTED` | 邀请被拒绝 | 对方拒绝邀请或邀请失败时 |
| `GAME_START` | 游戏开始 | 双方进入对战，第1回合开始 |
| `NEW_QUESTION` | 新题目 | 新回合开始，推送新题目 |
| `PLAYER_ANSWERED` | 玩家已作答 | 单方提交答案后 |
| `ROUND_RESULT` | 回合结果 | 双方都提交答案后 |
| `GAME_OVER` | 游戏结束 | 某方血量归零或退出 |

---

## 🎯 前端集成示例（完整代码）

```html
<!DOCTYPE html>
<html>
<head>
    <title>对战测试</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
</head>
<body>
    <h1>对战测试页面</h1>
    
    <div>
        <input id="username" placeholder="输入用户名" value="alice">
        <button onclick="connect()">连接</button>
        <button onclick="disconnect()">断开</button>
    </div>
    
    <div>
        <input id="inviteTarget" placeholder="邀请对方" value="bob">
        <button onclick="sendInvite()">发起邀请</button>
    </div>
    
    <div id="invitePanel" style="display:none">
        <p id="inviteMsg"></p>
        <button onclick="acceptInvite()">接受</button>
        <button onclick="rejectInvite()">拒绝</button>
    </div>
    
    <div id="battlePanel" style="display:none">
        <h3>对战中 - 房间: <span id="roomCode"></span></h3>
        <p>回合: <span id="round"></span></p>
        <p><span id="playerA"></span> 血量: <span id="healthA"></span></p>
        <p><span id="playerB"></span> 血量: <span id="healthB"></span></p>
        <div id="question"></div>
        <input id="lng" placeholder="经度" type="number" step="0.000001">
        <input id="lat" placeholder="纬度" type="number" step="0.000001">
        <button onclick="submitAnswer()">提交答案</button>
        <button onclick="quitBattle()">退出对战</button>
    </div>
    
    <div id="messages" style="height:300px; overflow:auto; border:1px solid #ccc"></div>

    <script>
        let stompClient = null;
        let currentRoomCode = null;
        let currentUsername = null;

        // 连接WebSocket
        function connect() {
            currentUsername = document.getElementById('username').value;
            const socket = new SockJS(`http://localhost:8080/api/ws-battle?username=${currentUsername}`);
            stompClient = Stomp.over(socket);
            
            stompClient.connect({}, function(frame) {
                addMessage('✅ 已连接');
                
                // 订阅对战状态消息
                stompClient.subscribe('/user/queue/battle/state', function(msg) {
                    handleBattleState(JSON.parse(msg.body));
                });
                
                // 订阅邀请消息
                stompClient.subscribe('/user/queue/battle/invite', function(msg) {
                    handleInvite(JSON.parse(msg.body));
                });
            });
        }

        // 断开连接
        function disconnect() {
            if (stompClient) stompClient.disconnect();
            addMessage('❌ 已断开');
        }

        // 发起邀请
        function sendInvite() {
            const target = document.getElementById('inviteTarget').value;
            stompClient.send('/app/battle/invite', {}, JSON.stringify({
                fromUsername: currentUsername,
                toUsername: target
            }));
            addMessage(`📤 已发送邀请给 ${target}`);
        }

        // 处理收到的邀请
        function handleInvite(data) {
            currentRoomCode = data.roomCode;
            document.getElementById('inviteMsg').innerText = data.message;
            document.getElementById('invitePanel').style.display = 'block';
            addMessage(`📨 ${data.message}`);
        }

        // 接受邀请
        function acceptInvite() {
            stompClient.send('/app/battle/respond', {}, JSON.stringify({
                roomCode: currentRoomCode,
                accepted: true,
                username: currentUsername
            }));
            document.getElementById('invitePanel').style.display = 'none';
        }

        // 拒绝邀请
        function rejectInvite() {
            stompClient.send('/app/battle/respond', {}, JSON.stringify({
                roomCode: currentRoomCode,
                accepted: false,
                username: currentUsername
            }));
            document.getElementById('invitePanel').style.display = 'none';
            currentRoomCode = null;
        }

        // 处理对战状态消息
        function handleBattleState(data) {
            addMessage(`📥 ${data.type}: ${data.message || ''}`);
            
            switch(data.type) {
                case 'GAME_START':
                case 'NEW_QUESTION':
                    currentRoomCode = data.roomCode;
                    document.getElementById('battlePanel').style.display = 'block';
                    document.getElementById('roomCode').innerText = data.roomCode;
                    document.getElementById('round').innerText = data.currentRound;
                    document.getElementById('playerA').innerText = data.playerA;
                    document.getElementById('playerB').innerText = data.playerB;
                    document.getElementById('healthA').innerText = data.playerAHealth;
                    document.getElementById('healthB').innerText = data.playerBHealth;
                    if (data.question) {
                        document.getElementById('question').innerHTML = 
                            `<p>题目: ${data.question.title}</p>
                             <img src="https://picui.cn/${data.question.imageKey}" width="300">`;
                    }
                    break;
                    
                case 'ROUND_RESULT':
                    document.getElementById('healthA').innerText = data.playerAHealth;
                    document.getElementById('healthB').innerText = data.playerBHealth;
                    if (data.roundResult) {
                        addMessage(`📊 ${data.playerA} 距离: ${data.roundResult.playerADistance.toFixed(2)}m`);
                        addMessage(`📊 ${data.playerB} 距离: ${data.roundResult.playerBDistance.toFixed(2)}m`);
                        addMessage(`💥 ${data.roundResult.damagedPlayer} 扣除 ${data.roundResult.damage} 血量`);
                    }
                    break;
                    
                case 'GAME_OVER':
                    addMessage(`🏆 ${data.winner} 获胜！`);
                    document.getElementById('battlePanel').style.display = 'none';
                    currentRoomCode = null;
                    break;
                    
                case 'INVITE_REJECTED':
                    addMessage(`❌ ${data.message}`);
                    break;
            }
        }

        // 提交答案
        function submitAnswer() {
            const lng = parseFloat(document.getElementById('lng').value);
            const lat = parseFloat(document.getElementById('lat').value);
            
            stompClient.send('/app/battle/answer', {}, JSON.stringify({
                roomCode: currentRoomCode,
                username: currentUsername,
                longitude: lng,
                latitude: lat
            }));
            
            addMessage(`📍 已提交答案: (${lng}, ${lat})`);
        }

        // 退出对战
        function quitBattle() {
            stompClient.send('/app/battle/quit', {}, JSON.stringify({
                roomCode: currentRoomCode,
                username: currentUsername
            }));
            document.getElementById('battlePanel').style.display = 'none';
            currentRoomCode = null;
        }

        // 添加消息到日志
        function addMessage(msg) {
            const div = document.getElementById('messages');
            const p = document.createElement('p');
            p.innerText = `[${new Date().toLocaleTimeString()}] ${msg}`;
            div.appendChild(p);
            div.scrollTop = div.scrollHeight;
        }
    </script>
</body>
</html>
```

---

## ⚠️ 常见问题 (FAQ)

### Q1: 为什么连接后收不到消息？
**A:** 检查以下几点：
1. 连接URL是否包含 `?username=xxx` 参数
2. 订阅路径是否正确（`/user/queue/battle/state` 和 `/user/queue/battle/invite`）
3. 确保用户名唯一，不能在多个页面用相同用户名连接

### Q2: 如何测试双人对战？
**A:** 在两个浏览器标签页分别打开测试页面，使用不同的用户名连接：
```
标签页1: username = alice
标签页2: username = bob
```
然后在标签页1发起邀请 → 标签页2接受 → 开始对战

### Q3: 局域网如何连接？
**A:** 将连接地址改为服务器的局域网IP：
```javascript
const socket = new SockJS('http://192.168.1.100:8080/api/ws-battle?username=alice');
```
确保防火墙开放8080端口。

### Q4: 如何获取题目的完整图片URL？
**A:** 拼接图床域名：
```javascript
const imageUrl = `https://picui.cn/${question.imageKey}`;
```

### Q5: 断线重连如何处理？
**A:** STOMP支持心跳机制，连接断开后需要重新调用 `connect()` 方法：
```javascript
stompClient.connect({}, onConnected, function(error) {
    console.log('连接失败:', error);
    // 5秒后重试
    setTimeout(connect, 5000);
});
```

---

## 🔗 相关文件

- **后端控制器**: `demo/src/main/java/com/campusguess/demo/controller/BattleWebSocketController.java`
- **WebSocket配置**: `demo/src/main/java/com/campusguess/demo/config/WebSocketConfig.java`
- **测试页面**: `demo/src/main/resources/static/battle-demo.html`
- **消息DTO**: `demo/src/main/java/com/campusguess/demo/model/dto/battle/`

---

## 📞 技术支持

如有疑问，请查阅项目根目录下的以下文档：
- `双人对战功能说明.md` - 业务逻辑说明
- `局域网对战测试指南.md` - 局域网部署指南
- `对战系统更新说明.md` - 版本更新日志
